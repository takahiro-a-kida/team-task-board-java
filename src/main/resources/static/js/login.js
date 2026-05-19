import { api } from "./api.js";

const form = document.getElementById("login-form");
const err = document.getElementById("error");

form.addEventListener("submit", async (e) => {
  e.preventDefault();
  err.textContent = "";
  const username = form.username.value.trim();
  const password = form.password.value;
  try {
    await api.login(username, password);
    location.href = "/";
  } catch (ex) {
    err.textContent = ex.message || "ログインに失敗しました";
  }
});
