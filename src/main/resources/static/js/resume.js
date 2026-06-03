"use strict";
import request from "./helper.js";

const firstName = document.getElementById("firstName");
const lastName = document.getElementById("lastName");
const email = document.getElementById("email");
const phoneNumber = document.getElementById("phone");
const designation = document.getElementById("designation");
const summary = document.getElementById("summary");
const logout = document.querySelector(".nav-link");
const saveBtn = document.querySelector(".btn-save");

// 检查必要元素是否存在
if (!firstName || !lastName || !saveBtn) {
  console.error("页面元素缺失，请检查 HTML 结构");
}

// 加载用户资料
const loadProfile = async function () {
  try {
    const profile = await request("/api/profile", {
      method: "GET",
    });

    document.querySelector(".user-name").textContent = `${profile.firstName || ""} ${profile.lastName || ""}'s Profile`;
    firstName.value = profile.firstName || "";
    lastName.value = profile.lastName || "";
    email.value = profile.email || "";
    phoneNumber.value = profile.phone || "";
    designation.value = profile.designation || "";
    summary.value = profile.summary || "";
    document.getElementById("theme").value = `theme${profile.theme || 1}`;

    const jobList = document.querySelector(".job-list");
    jobList.innerHTML = "";
    if (profile.jobs && profile.jobs.length > 0) {
      profile.jobs.forEach((job) => addJobRow(job));
    }

    const eduList = document.querySelector(".edu-list");
    eduList.innerHTML = "";
    if (profile.educations && profile.educations.length > 0) {
      profile.educations.forEach((edu) => addEducationRow(edu));
    }

    const skillsList = document.querySelector(".skills-list");
    skillsList.innerHTML = "";
    if (profile.skills && profile.skills.length > 0) {
      profile.skills.forEach((skill) => addSkillRow(skill));
    }
  } catch (error) {
    console.error("加载资料失败:", error);
    alert("加载资料失败，请重新登录");
    window.location.href = "/login.html";
  }
};

// 添加工作经历行
const addJobRow = function (job = {}) {
  const jobList = document.querySelector(".job-list");
  const row = document.createElement("div");
  row.className = "job-item";
  row.innerHTML = `
        <input type="text" placeholder="Company" value="${job.company || ""}" class="job-company">
        <input type="text" placeholder="Designation" value="${job.designation || ""}" class="job-designation">
        <input type="date" value="${job.startDate || ""}" class="job-start">
        <input type="date" value="${job.endDate || ""}" class="job-end">
        <label><input type="checkbox" ${job.currentJob ? "checked" : ""} class="job-current"> Current Job</label>
        <button type="button" class="btn-delete">Delete</button>
    `;
  jobList.appendChild(row);

  row.querySelector(".btn-delete").addEventListener("click", function () {
    row.remove();
  });
};

// 添加教育经历行
const addEducationRow = function (edu = {}) {
  const eduList = document.querySelector(".edu-list");
  const row = document.createElement("div");
  row.className = "edu-item";
  row.innerHTML = `
        <input type="text" placeholder="College" value="${edu.college || ""}" class="edu-college">
        <input type="text" placeholder="Qualification" value="${edu.qualification || ""}" class="edu-qualification">
        <input type="date" value="${edu.startDate || ""}" class="edu-start">
        <input type="date" value="${edu.endDate || ""}" class="edu-end">
        <input type="text" placeholder="Summary" value="${edu.summary || ""}" class="edu-summary">
        <button type="button" class="btn-delete">Delete</button>
    `;
  eduList.appendChild(row);

  row.querySelector(".btn-delete").addEventListener("click", function () {
    row.remove();
  });
};

// 添加技能行
const addSkillRow = function (skill = "") {
  const skillsList = document.querySelector(".skills-list");
  const row = document.createElement("div");
  row.className = "skill-item";
  row.innerHTML = `
        <input type="text" placeholder="Skill Name" value="${skill}" class="skill-name">
        <button type="button" class="btn-delete">Delete</button>
    `;
  skillsList.appendChild(row);

  row.querySelector(".btn-delete").addEventListener("click", function () {
    row.remove();
  });
};

// 添加按钮事件
const expBtnAdd = document.querySelector(".experience-section .btn-add");
if (expBtnAdd) {
  expBtnAdd.addEventListener("click", function () {
    addJobRow();
  });
}

const eduBtnAdd = document.querySelector(".education-section .btn-add");
if (eduBtnAdd) {
  eduBtnAdd.addEventListener("click", function () {
    addEducationRow();
  });
}

const skillsBtnAdd = document.querySelector(".skills-section .btn-add");
if (skillsBtnAdd) {
  skillsBtnAdd.addEventListener("click", function () {
    addSkillRow();
  });
}

// 保存资料
const saveProfile = async function () {
  const profile = {
    firstName: firstName.value,
    lastName: lastName.value,
    email: email.value,
    phone: phoneNumber.value,
    designation: designation.value,
    summary: summary.value,
    theme: parseInt(document.getElementById("theme").value.replace("theme", "")) || 1,
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
    profile.skills.push(row.querySelector(".skill-name").value);
  });

  try {
    saveBtn.disabled = true;
    saveBtn.textContent = "Saving...";

    await request("/api/profile", {
      method: "PUT",
      body: JSON.stringify(profile),
    });

    alert("保存成功！");
  } catch (error) {
    console.error("保存失败:", error);
    alert("保存失败，请重试");
  } finally {
    saveBtn.disabled = false;
    saveBtn.textContent = "Save";
  }
};

// 导出 PDF
const exportPdf = async function () {
  try {
    const response = await fetch("/api/profile/export/pdf", {
      method: "GET",
      credentials: "same-origin",
    });

    if (response.ok) {
      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `${firstName.value}_${lastName.value}_Resume.pdf`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      a.remove();
    } else {
      alert("导出 PDF 失败");
    }
  } catch (error) {
    console.error("导出 PDF 失败:", error);
    alert("导出 PDF 失败，请重试");
  }
};

// 绑定保存按钮
if (saveBtn) {
  saveBtn.addEventListener("click", saveProfile);
}

// 绑定导出 PDF 按钮
const exportPdfBtn = document.getElementById("exportPdfBtn");
if (exportPdfBtn) {
  exportPdfBtn.addEventListener("click", exportPdf);
}

// 退出登录（修复：调用后端 API）
if (logout) {
  logout.addEventListener("click", async function (e) {
    e.preventDefault();
    try {
      await fetch("/api/auth/logout", {
        method: "POST",
        credentials: "same-origin",
      });
    } catch (error) {
      console.error("登出失败:", error);
    } finally {
      window.location.href = "/login.html";
    }
  });
}

// ========== 分享功能 ==========

// 生成分享链接
const generateShareLink = async function () {
  try {
    const response = await fetch("/api/profile/share/generate", {
      method: "POST",
      credentials: "same-origin",
    });

    if (response.ok) {
      const result = await response.json();
      const shareLinkContainer = document.getElementById("shareLinkContainer");
      const shareLinkInput = document.getElementById("shareLink");
      const generateBtn = document.getElementById("generateShareBtn");
      const revokeBtn = document.getElementById("revokeShareBtn");

      shareLinkInput.value = window.location.origin + result.shareUrl;
      shareLinkContainer.style.display = "block";
      generateBtn.style.display = "none";
      revokeBtn.style.display = "inline-block";

      alert("Share link generated!");
    }
  } catch (error) {
    console.error("生成分享链接失败:", error);
    alert("生成分享链接失败");
  }
};

// 撤销分享链接
const revokeShareLink = async function () {
  try {
    const response = await fetch("/api/profile/share/revoke", {
      method: "POST",
      credentials: "same-origin",
    });

    if (response.ok) {
      const shareLinkContainer = document.getElementById("shareLinkContainer");
      const generateBtn = document.getElementById("generateShareBtn");
      const revokeBtn = document.getElementById("revokeShareBtn");

      shareLinkContainer.style.display = "none";
      generateBtn.style.display = "inline-block";
      revokeBtn.style.display = "none";

      alert("Share link revoked!");
    }
  } catch (error) {
    console.error("撤销分享链接失败:", error);
    alert("撤销分享链接失败");
  }
};

// 复制分享链接
const copyShareLink = function () {
  const shareLinkInput = document.getElementById("shareLink");
  shareLinkInput.select();
  document.execCommand("copy");
  alert("Link copied to clipboard!");
};

// 绑定分享功能按钮（添加存在性检查）
const generateShareBtn = document.getElementById("generateShareBtn");
const revokeShareBtn = document.getElementById("revokeShareBtn");
const copyShareLinkBtn = document.getElementById("copyShareLink");

if (generateShareBtn) {
  generateShareBtn.addEventListener("click", generateShareLink);
}

if (revokeShareBtn) {
  revokeShareBtn.addEventListener("click", revokeShareLink);
}

if (copyShareLinkBtn) {
  copyShareLinkBtn.addEventListener("click", copyShareLink);
}

// 初始化
loadProfile();
