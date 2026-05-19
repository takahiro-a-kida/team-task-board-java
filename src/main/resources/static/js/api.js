// 共通 API クライアント。CSRF Cookie を読んで X-XSRF-TOKEN ヘッダに付与する。

export function getCookie(name) {
  const m = document.cookie.match(new RegExp("(?:^|;\\s*)" + name + "=([^;]+)"));
  return m ? decodeURIComponent(m[1]) : null;
}

async function ensureCsrfCookie() {
  if (!getCookie("XSRF-TOKEN")) {
    await fetch("/api/me", { credentials: "same-origin" });
  }
}

async function request(method, path, body) {
  if (method !== "GET") {
    await ensureCsrfCookie();
  }
  const headers = { Accept: "application/json" };
  let payload;
  if (body instanceof FormData || body instanceof URLSearchParams) {
    payload = body;
  } else if (body !== undefined) {
    headers["Content-Type"] = "application/json";
    payload = JSON.stringify(body);
  }
  const csrf = getCookie("XSRF-TOKEN");
  if (csrf) {
    headers["X-XSRF-TOKEN"] = csrf;
  }
  const res = await fetch(path, {
    method,
    headers,
    body: payload,
    credentials: "same-origin",
  });
  if (res.status === 401) {
    if (!location.pathname.endsWith("/login.html")) {
      location.href = "/login.html";
    }
    throw new Error("unauthorized");
  }
  if (!res.ok) {
    let detail;
    try { detail = await res.json(); } catch { detail = { message: res.statusText }; }
    const err = new Error(detail.message || "リクエストに失敗しました");
    err.status = res.status;
    err.detail = detail;
    throw err;
  }
  if (res.status === 204) return null;
  const ct = res.headers.get("Content-Type") || "";
  if (ct.includes("application/json")) return res.json();
  return res.text();
}

export const api = {
  me: () => request("GET", "/api/me"),
  logout: () => request("POST", "/api/auth/logout"),
  login: async (username, password) => {
    await ensureCsrfCookie();
    const body = new URLSearchParams({ username, password });
    const csrf = getCookie("XSRF-TOKEN");
    const res = await fetch("/api/auth/login", {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
        "X-XSRF-TOKEN": csrf || "",
      },
      body,
      credentials: "same-origin",
    });
    if (!res.ok) {
      let detail;
      try { detail = await res.json(); } catch { detail = { message: res.statusText }; }
      const err = new Error(detail.message || "ログインに失敗しました");
      err.status = res.status;
      throw err;
    }
    return true;
  },

  teams: () => request("GET", "/api/teams"),
  createTeam: (name) => request("POST", "/api/teams", { name }),
  members: (teamId) => request("GET", `/api/teams/${teamId}/members`),
  addMember: (teamId, username, role) =>
    request("POST", `/api/teams/${teamId}/members`, { username, role }),
  changeRole: (teamId, userId, role) =>
    request("PATCH", `/api/teams/${teamId}/members/${userId}`, { role }),
  removeMember: (teamId, userId) =>
    request("DELETE", `/api/teams/${teamId}/members/${userId}`),

  searchTasks: (teamId, params) => {
    const qs = new URLSearchParams();
    Object.entries(params || {}).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== "") qs.append(k, v);
    });
    const q = qs.toString();
    return request("GET", `/api/teams/${teamId}/tasks${q ? "?" + q : ""}`);
  },
  getTask: (teamId, taskId) => request("GET", `/api/teams/${teamId}/tasks/${taskId}`),
  createTask: (teamId, body) => request("POST", `/api/teams/${teamId}/tasks`, body),
  updateTask: (teamId, taskId, body) =>
    request("PUT", `/api/teams/${teamId}/tasks/${taskId}`, body),
  changeStatus: (teamId, taskId, status) =>
    request("PATCH", `/api/teams/${teamId}/tasks/${taskId}/status`, { status }),
  deleteTask: (teamId, taskId) =>
    request("DELETE", `/api/teams/${teamId}/tasks/${taskId}`),

  searchUsers: (q) => request("GET", `/api/users/search?q=${encodeURIComponent(q)}`),
};

export function toast(message, opts = {}) {
  const el = document.createElement("div");
  el.className = "toast" + (opts.error ? " error" : "");
  el.textContent = message;
  document.body.appendChild(el);
  requestAnimationFrame(() => el.classList.add("show"));
  setTimeout(() => {
    el.classList.remove("show");
    setTimeout(() => el.remove(), 300);
  }, opts.duration || 2500);
}

export function escapeHtml(s) {
  if (s == null) return "";
  return String(s)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

export function formatDate(s) {
  return s || "";
}
