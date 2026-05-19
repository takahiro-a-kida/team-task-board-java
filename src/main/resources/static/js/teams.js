import { api, escapeHtml, toast } from "./api.js";

const ROLE_LABEL = { OWNER: "OWNER", MEMBER: "MEMBER" };

const state = {
  me: null,
  teams: [],
  selected: null,
  members: [],
};

const userChip = document.getElementById("user-chip");
const teamsPills = document.getElementById("teams-pills");
const detail = document.getElementById("team-detail");

async function bootstrap() {
  try {
    state.me = await api.me();
  } catch {
    location.href = "/login.html";
    return;
  }
  userChip.textContent = `${state.me.displayName} (${state.me.username})`;
  document.getElementById("logout-btn").addEventListener("click", async () => {
    await api.logout().catch(() => {});
    location.href = "/login.html";
  });
  document.getElementById("create-team-btn").addEventListener("click", createTeam);
  document.getElementById("new-team-name").addEventListener("keydown", (e) => {
    if (e.key === "Enter") createTeam();
  });
  await loadTeams();
}

async function loadTeams(preferId) {
  state.teams = await api.teams();
  renderTeamPills();
  if (state.teams.length === 0) {
    detail.innerHTML = '<div class="empty">まだチームがありません。左のフォームから作成してください。</div>';
    state.selected = null;
    return;
  }
  const next = preferId && state.teams.find((t) => t.id === preferId)
    ? preferId
    : state.selected && state.teams.find((t) => t.id === state.selected)
      ? state.selected
      : state.teams[0].id;
  selectTeam(next);
}

function renderTeamPills() {
  teamsPills.innerHTML = state.teams
    .map(
      (t) =>
        `<div class="team-pill ${t.id === state.selected ? "selected" : ""}" data-id="${t.id}">${escapeHtml(t.name)}</div>`,
    )
    .join("");
  teamsPills.querySelectorAll(".team-pill").forEach((el) =>
    el.addEventListener("click", () => selectTeam(Number(el.dataset.id))),
  );
}

async function selectTeam(teamId) {
  state.selected = teamId;
  renderTeamPills();
  try {
    state.members = await api.members(teamId);
  } catch (e) {
    detail.innerHTML = `<div class="empty">${escapeHtml(e.message)}</div>`;
    return;
  }
  renderDetail();
}

function myRole() {
  return state.members.find((m) => m.userId === state.me.id)?.role;
}

function renderDetail() {
  const team = state.teams.find((t) => t.id === state.selected);
  const role = myRole();
  const isOwner = role === "OWNER";
  detail.innerHTML = `
    <h2 style="margin-top:0;">${escapeHtml(team.name)}</h2>
    <p style="color: var(--muted);">あなたの権限: <strong>${escapeHtml(role || "-")}</strong></p>
    <h3>メンバー</h3>
    <table class="member-table">
      <thead>
        <tr><th>ユーザー</th><th>ロール</th><th></th></tr>
      </thead>
      <tbody>
        ${state.members
          .map(
            (m) => `
          <tr>
            <td>${escapeHtml(m.displayName)} <span style="color:var(--muted);">(${escapeHtml(m.username)})</span></td>
            <td>
              ${
                isOwner && m.userId !== state.me.id
                  ? `<select data-role-for="${m.userId}">
                       ${["OWNER", "MEMBER"].map((r) => `<option value="${r}" ${m.role === r ? "selected" : ""}>${r}</option>`).join("")}
                     </select>`
                  : `<span>${escapeHtml(ROLE_LABEL[m.role])}</span>`
              }
            </td>
            <td>
              ${
                isOwner && m.userId !== state.me.id
                  ? `<button class="danger" data-remove="${m.userId}">削除</button>`
                  : ""
              }
            </td>
          </tr>`,
          )
          .join("")}
      </tbody>
    </table>

    ${
      isOwner
        ? `
      <h3 style="margin-top:24px;">メンバーを追加</h3>
      <div style="display:flex; gap:8px; align-items:end; max-width: 460px;">
        <div style="flex:1;">
          <label>ユーザー名</label>
          <input id="add-username" type="text" placeholder="username" />
        </div>
        <div>
          <label>ロール</label>
          <select id="add-role">
            <option value="MEMBER">MEMBER</option>
            <option value="OWNER">OWNER</option>
          </select>
        </div>
        <button class="primary" id="add-member-btn">追加</button>
      </div>
      <p style="color: var(--muted); font-size: 12px;">ユーザーが存在しない場合は追加できません。</p>
    `
        : ""
    }
  `;

  detail.querySelectorAll("[data-remove]").forEach((btn) =>
    btn.addEventListener("click", () => removeMember(Number(btn.dataset.remove))),
  );
  detail.querySelectorAll("[data-role-for]").forEach((sel) =>
    sel.addEventListener("change", () => changeRole(Number(sel.dataset.roleFor), sel.value)),
  );
  detail.querySelector("#add-member-btn")?.addEventListener("click", addMember);
}

async function addMember() {
  const username = detail.querySelector("#add-username").value.trim();
  const role = detail.querySelector("#add-role").value;
  if (!username) return;
  try {
    await api.addMember(state.selected, username, role);
    toast("メンバーを追加しました");
    await selectTeam(state.selected);
  } catch (e) {
    toast(e.message, { error: true });
  }
}

async function changeRole(userId, role) {
  try {
    await api.changeRole(state.selected, userId, role);
    toast("ロールを変更しました");
    await selectTeam(state.selected);
  } catch (e) {
    toast(e.message, { error: true });
    await selectTeam(state.selected);
  }
}

async function removeMember(userId) {
  if (!confirm("このメンバーを削除しますか?")) return;
  try {
    await api.removeMember(state.selected, userId);
    toast("メンバーを削除しました");
    await selectTeam(state.selected);
  } catch (e) {
    toast(e.message, { error: true });
  }
}

async function createTeam() {
  const input = document.getElementById("new-team-name");
  const name = input.value.trim();
  if (!name) return;
  try {
    const id = await api.createTeam(name);
    input.value = "";
    toast("チームを作成しました");
    await loadTeams(Number(id));
  } catch (e) {
    toast(e.message, { error: true });
  }
}

bootstrap();
