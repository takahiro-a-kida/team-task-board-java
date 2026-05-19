import { api, escapeHtml, toast } from "./api.js";

const STATUSES = ["TODO", "IN_PROGRESS", "DONE"];
const PRIORITY_LABEL = { HIGH: "高", MEDIUM: "中", LOW: "低" };
const STATUS_LABEL = { TODO: "未着手", IN_PROGRESS: "作業中", DONE: "完了" };

const state = {
  me: null,
  teams: [],
  currentTeamId: null,
  members: [],
  tasks: [],
  filter: {},
};

const els = {
  teamSelect: document.getElementById("team-select"),
  userChip: document.getElementById("user-chip"),
  logout: document.getElementById("logout-btn"),
  newTask: document.getElementById("new-task-btn"),
  modalRoot: document.getElementById("modal-root"),
  reset: document.getElementById("reset-filter"),
  filters: {
    keyword: document.getElementById("f-keyword"),
    status: document.getElementById("f-status"),
    priority: document.getElementById("f-priority"),
    assignee: document.getElementById("f-assignee"),
    dueFrom: document.getElementById("f-due-from"),
    dueTo: document.getElementById("f-due-to"),
  },
};

async function bootstrap() {
  try {
    state.me = await api.me();
  } catch {
    location.href = "/login.html";
    return;
  }
  els.userChip.textContent = `${state.me.displayName} (${state.me.username})`;

  state.teams = await api.teams();
  if (state.teams.length === 0) {
    document.querySelector("main").innerHTML =
      '<div class="empty">所属しているチームがありません。<a href="/teams.html">チーム管理</a>でチームを作成してください。</div>';
    return;
  }
  const saved = Number(localStorage.getItem("teamId"));
  state.currentTeamId = state.teams.find((t) => t.id === saved)?.id ?? state.teams[0].id;
  renderTeamSelect();
  await switchTeam(state.currentTeamId);
  attachListeners();
}

function renderTeamSelect() {
  els.teamSelect.innerHTML = state.teams
    .map((t) => `<option value="${t.id}" ${t.id === state.currentTeamId ? "selected" : ""}>${escapeHtml(t.name)}</option>`)
    .join("");
}

async function switchTeam(teamId) {
  state.currentTeamId = teamId;
  localStorage.setItem("teamId", String(teamId));
  try {
    state.members = await api.members(teamId);
  } catch {
    state.members = [];
  }
  await refreshTasks();
}

async function refreshTasks() {
  const f = state.filter;
  try {
    state.tasks = await api.searchTasks(state.currentTeamId, {
      keyword: f.keyword,
      status: f.status,
      priority: f.priority,
      assignee: f.assignee,
      dueFrom: f.dueFrom,
      dueTo: f.dueTo,
    });
  } catch (e) {
    toast(e.message, { error: true });
    state.tasks = [];
  }
  renderBoard();
}

function renderBoard() {
  for (const status of STATUSES) {
    const list = document.querySelector(`.column[data-status="${status}"] [data-list]`);
    const items = state.tasks.filter((t) => t.status === status);
    document.querySelector(`.column[data-status="${status}"] [data-count]`).textContent = items.length;
    if (items.length === 0) {
      list.innerHTML = '<div class="empty" style="padding:18px; font-size:12px;">タスクなし</div>';
    } else {
      list.innerHTML = items.map(renderCard).join("");
    }
  }
  attachCardHandlers();
}

function renderCard(task) {
  const due = task.dueDate
    ? `<span class="${isOverdue(task) ? "due-overdue" : ""}">期限 ${escapeHtml(task.dueDate)}</span>`
    : "";
  const assignee = task.assignee
    ? `<span>👤 ${escapeHtml(task.assignee.displayName)}</span>`
    : '<span style="color:#94a3b8;">未割当</span>';
  return `
    <div class="task-card" draggable="true" data-id="${task.id}">
      <div class="title">${escapeHtml(task.title)}</div>
      <div class="meta">
        <span class="priority-badge priority-${task.priority}">${PRIORITY_LABEL[task.priority]}</span>
        ${assignee}
        ${due}
      </div>
    </div>
  `;
}

function isOverdue(task) {
  if (!task.dueDate || task.status === "DONE") return false;
  return task.dueDate < new Date().toISOString().slice(0, 10);
}

function attachCardHandlers() {
  document.querySelectorAll(".task-card").forEach((card) => {
    card.addEventListener("click", () => openTaskModal(Number(card.dataset.id)));
    card.addEventListener("dragstart", (e) => {
      card.classList.add("dragging");
      e.dataTransfer.setData("text/plain", card.dataset.id);
      e.dataTransfer.effectAllowed = "move";
    });
    card.addEventListener("dragend", () => card.classList.remove("dragging"));
  });
  document.querySelectorAll(".column").forEach((col) => {
    col.addEventListener("dragover", (e) => {
      e.preventDefault();
      col.classList.add("drop-target");
    });
    col.addEventListener("dragleave", () => col.classList.remove("drop-target"));
    col.addEventListener("drop", async (e) => {
      e.preventDefault();
      col.classList.remove("drop-target");
      const id = Number(e.dataTransfer.getData("text/plain"));
      const newStatus = col.dataset.status;
      const task = state.tasks.find((t) => t.id === id);
      if (!task || task.status === newStatus) return;
      try {
        await api.changeStatus(state.currentTeamId, id, newStatus);
        task.status = newStatus;
        renderBoard();
        toast(`「${task.title}」を ${STATUS_LABEL[newStatus]} に移動しました`);
      } catch (ex) {
        toast(ex.message, { error: true });
      }
    });
  });
}

function attachListeners() {
  els.teamSelect.addEventListener("change", (e) => switchTeam(Number(e.target.value)));
  els.logout.addEventListener("click", async () => {
    await api.logout().catch(() => {});
    location.href = "/login.html";
  });
  els.newTask.addEventListener("click", () => openTaskModal(null));
  els.reset.addEventListener("click", () => {
    Object.values(els.filters).forEach((el) => { el.value = ""; });
    state.filter = {};
    refreshTasks();
  });

  let debounce;
  const applyFilter = () => {
    clearTimeout(debounce);
    debounce = setTimeout(() => {
      state.filter = {
        keyword: els.filters.keyword.value.trim(),
        status: els.filters.status.value,
        priority: els.filters.priority.value,
        assignee: els.filters.assignee.value,
        dueFrom: els.filters.dueFrom.value,
        dueTo: els.filters.dueTo.value,
      };
      refreshTasks();
    }, 200);
  };
  els.filters.keyword.addEventListener("input", applyFilter);
  ["status", "priority", "assignee", "dueFrom", "dueTo"].forEach((k) =>
    els.filters[k].addEventListener("change", applyFilter),
  );
}

async function openTaskModal(taskId) {
  let task = null;
  if (taskId != null) {
    try {
      task = await api.getTask(state.currentTeamId, taskId);
    } catch (e) {
      toast(e.message, { error: true });
      return;
    }
  }
  els.modalRoot.innerHTML = renderModal(task);
  bindModalHandlers(task);
}

function renderModal(task) {
  const isEdit = task != null;
  const assigneeOptions = ['<option value="">未割当</option>']
    .concat(
      state.members.map(
        (m) =>
          `<option value="${m.userId}" ${
            isEdit && task.assignee && task.assignee.id === m.userId ? "selected" : ""
          }>${escapeHtml(m.displayName)} (${escapeHtml(m.username)})</option>`,
      ),
    )
    .join("");
  const status = isEdit ? task.status : "TODO";
  const priority = isEdit ? task.priority : "MEDIUM";
  return `
    <div class="modal-backdrop">
      <div class="modal">
        <h2>${isEdit ? "タスクの編集" : "新規タスク"}</h2>
        <form id="task-form">
          <div class="form-grid">
            <div class="full">
              <label>タイトル</label>
              <input name="title" required maxlength="200" value="${escapeHtml(isEdit ? task.title : "")}" />
            </div>
            <div class="full">
              <label>説明</label>
              <textarea name="description" maxlength="4000">${escapeHtml(isEdit ? task.description : "")}</textarea>
            </div>
            <div>
              <label>ステータス</label>
              <select name="status" ${isEdit ? "" : "disabled"}>
                ${STATUSES.map((s) => `<option value="${s}" ${s === status ? "selected" : ""}>${STATUS_LABEL[s]}</option>`).join("")}
              </select>
            </div>
            <div>
              <label>優先度</label>
              <select name="priority">
                ${["HIGH", "MEDIUM", "LOW"]
                  .map((p) => `<option value="${p}" ${p === priority ? "selected" : ""}>${PRIORITY_LABEL[p]}</option>`)
                  .join("")}
              </select>
            </div>
            <div>
              <label>担当者</label>
              <select name="assigneeUserId">${assigneeOptions}</select>
            </div>
            <div>
              <label>期限</label>
              <input name="dueDate" type="date" value="${isEdit && task.dueDate ? task.dueDate : ""}" />
            </div>
          </div>
          <div class="actions">
            ${isEdit ? '<button type="button" class="danger" data-action="delete">削除</button>' : ""}
            <div class="spacer" style="flex:1;"></div>
            <button type="button" class="ghost" data-action="cancel">キャンセル</button>
            <button class="primary" type="submit">${isEdit ? "保存" : "作成"}</button>
          </div>
        </form>
      </div>
    </div>
  `;
}

function bindModalHandlers(task) {
  const root = els.modalRoot;
  const close = () => { root.innerHTML = ""; };
  root.querySelector("[data-action=cancel]")?.addEventListener("click", close);
  root.querySelector(".modal-backdrop").addEventListener("click", (e) => {
    if (e.target.classList.contains("modal-backdrop")) close();
  });

  const form = root.querySelector("#task-form");
  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    const fd = new FormData(form);
    const payload = {
      title: fd.get("title").trim(),
      description: fd.get("description") || "",
      priority: fd.get("priority"),
      assigneeUserId: fd.get("assigneeUserId") ? Number(fd.get("assigneeUserId")) : null,
      dueDate: fd.get("dueDate") || null,
    };
    try {
      if (task) {
        payload.status = fd.get("status");
        await api.updateTask(state.currentTeamId, task.id, payload);
        toast("タスクを更新しました");
      } else {
        await api.createTask(state.currentTeamId, payload);
        toast("タスクを作成しました");
      }
      close();
      await refreshTasks();
    } catch (ex) {
      toast(ex.message, { error: true });
    }
  });

  root.querySelector("[data-action=delete]")?.addEventListener("click", async () => {
    if (!confirm(`「${task.title}」を削除しますか?`)) return;
    try {
      await api.deleteTask(state.currentTeamId, task.id);
      toast("タスクを削除しました");
      close();
      await refreshTasks();
    } catch (ex) {
      toast(ex.message, { error: true });
    }
  });
}

bootstrap();
