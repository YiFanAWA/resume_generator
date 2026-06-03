"use strict";

import request from "./helper.js";

const form = document.querySelector(".form-register");
const registerBtn = document.querySelector(".button-register");
const loginLink = document.querySelector(".link-lognin a");

const register = async function (event) {
  event.preventDefault();

  const username = document.querySelector(".input-username").value.trim();
  const password = document.querySelector(".input-password").value.trim();
  const confirmPassword = document.querySelector(".input-confirm-password").value.trim();

  if (!username || !password || !confirmPassword) {
    alert("Please fill in all fields.");
    return;
  }
  if (password !== confirmPassword) {
    alert("Passwords do not match.");
    return;
  }
  if (password.length < 8) {
    alert("Password must be at least 8 characters long.");
    return;
  }

  registerBtn.disabled = true;
  registerBtn.textContent = "Registering...";

  try {
    const result = await request("/api/auth/register", {
      method: "POST",
      body: JSON.stringify({ username, password, confirmPassword }),
    });

    if (result.message === "Registration successful") {
      alert("Registration successful. Please login.");
      window.location.href = "/login.html";
      return;
    }

    alert(result.error || "Registration failed.");
  } catch (error) {
    console.error("Registration failed:", error);
    alert(error.message || "Registration failed. Please try again.");
  } finally {
    registerBtn.disabled = false;
    registerBtn.textContent = "Register";
  }
};

form?.addEventListener("submit", register);

loginLink?.addEventListener("click", function (event) {
  event.preventDefault();
  window.location.href = "/login.html";
});
