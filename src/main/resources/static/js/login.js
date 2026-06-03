"use strict";
import request from "./helper.js";

const form = document.querySelector(".form-login");
const userName = document.querySelector(".input-username");
const password = document.querySelector(".input-password");
const loginBtn = document.querySelector(".button-loginin");
const registerLink = document.querySelector(".link-create-account a");

const handleLogin = async function (e) {
  e.preventDefault();

  const username = userName.value.trim();
  const pass = password.value.trim();

  if (!username || !pass) {
    alert("账号密码不能为空");
    return;
  }

  loginBtn.disabled = true;
  loginBtn.textContent = "Logging in...";

  try {
    const result = await request("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({ username, password: pass }),
    });

    if (result.message === "Login successful") {
      window.location.href = "/resume.html";
    } else {
      alert(result.error || "账号或密码错误");
    }
  } catch (error) {
    console.error("登录失败:", error);
    alert("登录失败，请重试");
  } finally {
    loginBtn.disabled = false;
    loginBtn.textContent = "Sign in";
  }
};

form.addEventListener("submit", handleLogin);

if (registerLink) {
  registerLink.addEventListener("click", function (e) {
    e.preventDefault();
    window.location.href = "/register.html";
  });
}
