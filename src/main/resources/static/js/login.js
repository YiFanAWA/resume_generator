"use strict";

import request from "./helper.js";

const form = document.querySelector(".form-login");
const userName = document.querySelector(".input-username");
const password = document.querySelector(".input-password");
const loginBtn = document.querySelector(".button-loginin");
const registerLink = document.querySelector(".link-create-account a");

const handleLogin = async function (event) {
  event.preventDefault();

  const username = userName.value.trim();
  const pass = password.value.trim();

  if (!username || !pass) {
    alert("Username and password are required.");
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
      return;
    }

    alert(result.error || "Invalid username or password.");
  } catch (error) {
    console.error("Login failed:", error);
    alert(error.message || "Login failed. Please try again.");
  } finally {
    loginBtn.disabled = false;
    loginBtn.textContent = "Sign in";
  }
};

form?.addEventListener("submit", handleLogin);

registerLink?.addEventListener("click", function (event) {
  event.preventDefault();
  window.location.href = "/register.html";
});
