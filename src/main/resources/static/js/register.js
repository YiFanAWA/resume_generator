"use strict";
import request from "./helper.js";

const form = document.querySelector(".form-register");
const registerBtn = document.querySelector(".button-register");
const loginLink = document.querySelector(".link-lognin a");

if (loginLink) {
  loginLink.addEventListener("click", function (e) {
    e.preventDefault();
    window.location.href = "/login.html";
  });
}

const register = async function (e) {
  e.preventDefault();

  const username = document.querySelector(".input-username").value.trim();
  const password = document.querySelector(".input-password").value.trim();
  const confirmPassword = document.querySelector(".input-confirm-password").value.trim();

  if (!username || !password || !confirmPassword) return alert("请填写所有字段");
  if (password !== confirmPassword) return alert("两次密码不一致");
  if (password.length < 4) return alert("密码至少4位");

  registerBtn.disabled = true;
  registerBtn.textContent = "Registering...";

  try {
    const result = await request("/api/auth/register", {
      method: "POST",
      body: JSON.stringify({ username, password, confirmPassword }),
    });

    if (result.message === "Registration successful") {
      alert("注册成功，请登录");
      window.location.href = "/login.html";
    } else {
      alert(result.error || "注册失败");
    }
  } catch (error) {
    console.error("注册失败:", error);
    alert("注册失败，请重试");
  } finally {
    registerBtn.disabled = false;
    registerBtn.textContent = "Register";
  }
};

form.addEventListener("submit", register);
