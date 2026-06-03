import request from "./helper.js";

const escapeHtml = (value = "") =>
  String(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");

const firstName = document.getElementById("firstName");
const lastName = document.getElementById("lastName");
const email = document.getElementById("email");
const phoneNumber = document.getElementById("phone");
const designation = document.getElementById("designation");
const summary = document.getElementById("summary");
const logout = document.querySelector(".nav-link");
const saveBtn = document.querySelector(".btn-save");

const addJobRow = function (job = {}) {
  const jobList = document.querySelector(".job-list");
  const row = document.createElement("div");
  row.className = "job-item";
  row.innerHTML = `
    <input type="text" placeholder="Company" value="${escapeHtml(job.company || "")}" class="job-company">
    <input type="text" placeholder="Designation" value="${escapeHtml(job.designation || "")}" class="job-designation">
    <input type="date" value="${escapeHtml(job.startDate || "")}" class="job-start">
    <input type="date" value="${escapeHtml(job.endDate || "")}" class="job-end">
    <label><input type="checkbox" ${job.currentJob ? "checked" : ""} class="job-current"> Current Job</label>
    <button type="button" class="btn-delete">Delete</button>
  `;
  jobList.appendChild(row);

  row.querySelector(".btn-delete").addEventListener("click", function () {
    row.remove();
  });
};

const addEducationRow = function (edu = {}) {
  const eduList = document.querySelector(".edu-list");
  const row = document.createElement("div");
  row.className = "edu-item";
  row.innerHTML = `
    <input type="text" placeholder="College" value="${escapeHtml(edu.college || "")}" class="edu-college">
    <input type="text" placeholder="Qualification" value="${escapeHtml(edu.qualification || "")}" class="edu-qualification">
    <input type="date" value="${escapeHtml(edu.startDate || "")}" class="edu-start">
    <input type="date" value="${escapeHtml(edu.endDate || "")}" class="edu-end">
    <input type="text" placeholder="Summary" value="${escapeHtml(edu.summary || "")}" class="edu-summary">
    <button type="button" class="btn-delete">Delete</button>
  `;
  eduList.appendChild(row);

  row.querySelector(".btn-delete").addEventListener("click", function () {
    row.remove();
  });
};

const addSkillRow = function (skill = "") {
  const skillsList = document.querySelector(".skills-list");
  const row = document.createElement("div");
  row.className = "skill-item";
  row.innerHTML = `
    <input type="text" placeholder="Skill Name" value="${escapeHtml(skill)}" class="skill-name">
    <button type="button" class="btn-delete">Delete</button>
  `;
  skillsList.appendChild(row);

  row.querySelector(".btn-delete").addEventListener("click", function () {
    row.remove();
  });
};

const updateShareUi = function (shareState = {}) {
  const shareLinkContainer = document.getElementById("shareLinkContainer");
  const shareLinkInput = document.getElementById("shareLink");
  const generateBtn = document.getElementById("generateShareBtn");
  const revokeBtn = document.getElementById("revokeShareBtn");

  if (!shareLinkContainer || !shareLinkInput || !generateBtn || !revokeBtn) {
    return;
  }

  if (shareState.isPublic && shareState.shareUrl) {
    shareLinkInput.value = window.location.origin + shareState.shareUrl;
    shareLinkContainer.style.display = "block";
    generateBtn.style.display = "none";
    revokeBtn.style.display = "inline-block";
  } else {
    shareLinkInput.value = "";
    shareLinkContainer.style.display = "none";
    generateBtn.style.display = "inline-block";
    revokeBtn.style.display = "none";
  }
};

const loadProfile = async function () {
  try {
    const profile = await request("/api/profile", { method: "GET" });

    document.querySelector(".user-name").textContent = `${profile.firstName || ""} ${profile.lastName || ""}'s Profile`;
    firstName.value = profile.firstName || "";
    lastName.value = profile.lastName || "";
    email.value = profile.email || "";
    phoneNumber.value = profile.phone || "";
    designation.value = profile.designation || "";
    summary.value = profile.summary || "";
    document.getElementById("theme").value = `theme${profile.theme || 1}`;
    updateShareUi(profile);

    const jobList = document.querySelector(".job-list");
    jobList.innerHTML = "";
    if (Array.isArray(profile.jobs)) {
      profile.jobs.forEach((job) => addJobRow(job));
    }

    const eduList = document.querySelector(".edu-list");
    eduList.innerHTML = "";
    if (Array.isArray(profile.educations)) {
      profile.educations.forEach((edu) => addEducationRow(edu));
    }

    const skillsList = document.querySelector(".skills-list");
    skillsList.innerHTML = "";
    if (Array.isArray(profile.skills)) {
      profile.skills.forEach((skill) => addSkillRow(skill));
    }
  } catch (error) {
    console.error("Failed to load profile:", error);
    alert("Failed to load profile. Please login again.");
    window.location.href = "/login.html";
  }
};

const saveProfile = async function () {
  const profile = {
    firstName: firstName.value,
    lastName: lastName.value,
    email: email.value,
    phone: phoneNumber.value,
    designation: designation.value,
    summary: summary.value,
    theme: parseInt(document.getElementById("theme").value.replace("theme", ""), 10) || 1,
    jobs: [],
    educations: [],
    skills: [],
  };

  document.querySelectorAll(".job-item").forEach(function (row) {
    profile.jobs.push({
      company: row.querySelector(".job-company").value,
      designation: row.querySelector(".job-designation").value,
      startDate: row.querySelector(".job-start").value,
      endDate: row.querySelector(".job-end").value,
      currentJob: row.querySelector(".job-current").checked,
    });
  });

  document.querySelectorAll(".edu-item").forEach(function (row) {
    profile.educations.push({
      college: row.querySelector(".edu-college").value,
      qualification: row.querySelector(".edu-qualification").value,
      startDate: row.querySelector(".edu-start").value,
      endDate: row.querySelector(".edu-end").value,
      summary: row.querySelector(".edu-summary").value,
    });
  });

  document.querySelectorAll(".skill-item").forEach(function (row) {
    const skill = row.querySelector(".skill-name").value.trim();
    if (skill) {
      profile.skills.push(skill);
    }
  });

  try {
    saveBtn.disabled = true;
    saveBtn.textContent = "Saving...";
    await request("/api/profile", {
      method: "PUT",
      body: JSON.stringify(profile),
    });
    alert("Profile saved successfully.");
  } catch (error) {
    console.error("Failed to save profile:", error);
    alert("Failed to save profile. Please try again.");
  } finally {
    saveBtn.disabled = false;
    saveBtn.textContent = "Save";
  }
};

const exportPdf = async function () {
  try {
    const response = await fetch("/api/profile/export/pdf", {
      method: "GET",
      credentials: "same-origin",
    });

    if (!response.ok) {
      alert("Failed to export PDF.");
      return;
    }

    const blob = await response.blob();
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `${firstName.value || "resume"}_${lastName.value || ""}_Resume.pdf`;
    document.body.appendChild(link);
    link.click();
    window.URL.revokeObjectURL(url);
    link.remove();
  } catch (error) {
    console.error("Failed to export PDF:", error);
    alert("Failed to export PDF. Please try again.");
  }
};

const generateShareLink = async function () {
  try {
    const response = await fetch("/api/profile/share/generate", {
      method: "POST",
      credentials: "same-origin",
    });

    if (!response.ok) {
      alert("Failed to generate share link.");
      return;
    }

    const result = await response.json();
    updateShareUi(result);
    alert("Share link generated.");
  } catch (error) {
    console.error("Failed to generate share link:", error);
    alert("Failed to generate share link.");
  }
};

const revokeShareLink = async function () {
  try {
    const response = await fetch("/api/profile/share/revoke", {
      method: "POST",
      credentials: "same-origin",
    });

    if (!response.ok) {
      alert("Failed to revoke share link.");
      return;
    }

    const result = await response.json();
    updateShareUi(result);
    alert("Share link revoked.");
  } catch (error) {
    console.error("Failed to revoke share link:", error);
    alert("Failed to revoke share link.");
  }
};

const copyShareLink = async function () {
  const shareLinkInput = document.getElementById("shareLink");

  try {
    await navigator.clipboard.writeText(shareLinkInput.value);
  } catch (error) {
    shareLinkInput.select();
    document.execCommand("copy");
  }

  alert("Link copied to clipboard.");
};

document.querySelector(".experience-section .btn-add")?.addEventListener("click", () => addJobRow());
document.querySelector(".education-section .btn-add")?.addEventListener("click", () => addEducationRow());
document.querySelector(".skills-section .btn-add")?.addEventListener("click", () => addSkillRow());
saveBtn?.addEventListener("click", saveProfile);
document.getElementById("exportPdfBtn")?.addEventListener("click", exportPdf);
document.getElementById("generateShareBtn")?.addEventListener("click", generateShareLink);
document.getElementById("revokeShareBtn")?.addEventListener("click", revokeShareLink);
document.getElementById("copyShareLink")?.addEventListener("click", copyShareLink);

logout?.addEventListener("click", async function (event) {
  event.preventDefault();
  try {
    await fetch("/api/auth/logout", {
      method: "POST",
      credentials: "same-origin",
    });
  } finally {
    window.location.href = "/login.html";
  }
});

loadProfile();
