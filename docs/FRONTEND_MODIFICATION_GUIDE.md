# 前端修改指南 - 适配 Spring Boot 后端

## 概述

本文档说明如何将现有的前端页面（login.html、register.html、resume.html）修改为与 Spring Boot 后端 API 对接。

---

## 当前问题

你的前端代码中存在以下问题：

1. ❌ **使用了 Mock 数据拦截器** - `login.js` 和 `register.js` 中有模拟数据的 `window.fetch` 拦截器
2. ❌ **API 路径错误** - 调用的是 `/api/login` 和 `/api/register`，实际应该是 `/api/auth/login` 和 `/api/auth/register`
3. ❌ **简历页面功能不完整** - `resume.js` 只有初始化功能，缺少与后端交互的完整逻辑
4. ❌ **缺少 Session 认证支持** - 没有在 fetch 请求中添加 `credentials: 'same-origin'`
5. ❌ **缺少动态添加/删除功能** - Job、Education、Skill 的增删逻辑未实现

---

## 修改步骤

### 步骤 1：修改 `js/helper.js`

添加 Session 认证支持：

```javascript
"use strict";
const TIMEOUT = 5000;

const request = async function (url, options = {}) {
    const fetchPromise = fetch(url, {
        method: options.method || "POST",
        headers: { "Content-Type": "application/json", ...options.headers },
        credentials: "same-origin",  // ← 新增：支持 Session 认证
        ...options,
    });

    const timeoutPromise = new Promise((_, reject) => {
        setTimeout(() => reject(new Error("请求超时")), TIMEOUT);
    });

    const response = await Promise.race([fetchPromise, timeoutPromise]);

    if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }
    return response.json();
};

export default request;
```

**关键改动：**
- 添加 `credentials: "same-origin"` 以支持 Spring Security 的 Session 认证

---

### 步骤 2：修改 `js/login.js`

删除 Mock 拦截器，修改 API 路径：

```javascript
"use strict";
import request from "./helper.js";

const form = document.querySelector(".form-login");
const userName = document.querySelector(".input-username");
const password = document.querySelector(".input-password");
const loginBtn = document.querySelector(".button-loginin");
const registerLink = document.querySelector(".link-create-account a");

// ❌ 删除：删除第 10-43 行的 window.fetch 拦截器

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
        // ✅ 修改：API 路径从 /api/login 改为 /api/auth/login
        const result = await request("/api/auth/login", {
            body: JSON.stringify({ username, password: pass }),
        });

        // ✅ 修改：检查返回的消息
        if (result.message === "Login successful") {
            localStorage.setItem("username", username);
            window.location.href = "resume.html";
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
        window.location.href = "register.html";
    });
}
```

**关键改动：**
- ❌ 删除 `window.fetch` Mock 拦截器（第 10-43 行）
- ✅ API 路径改为 `/api/auth/login`
- ✅ 返回数据结构适配后端（`result.message` 而非 `result.code`）

---

### 步骤 3：修改 `js/register.js`

删除 Mock 拦截器，修改 API 路径：

```javascript
"use strict";
import request from "./helper.js";

const form = document.querySelector(".form-register");
const registerBtn = document.querySelector(".button-register");
const loginLink = document.querySelector(".link-lognin a");

// ❌ 删除：删除第 9-31 行的 window.fetch 拦截器

if (loginLink) {
    loginLink.addEventListener("click", function (e) {
        e.preventDefault();
        window.location.href = "login.html";
    });
}

const register = async function (e) {
    e.preventDefault();

    const username = document.querySelector(".input-username").value.trim();
    const password = document.querySelector(".input-password").value.trim();
    const confirmPassword = document.querySelector(".input-confirm-password").value.trim();

    if (!username || !password || !confirmPassword) return alert("请填写所有字段");
    if (password !== confirmPassword) return alert("两次密码不一致");
    if (password.length < 4) return alert("密码至少4位");  // ✅ 修改：后端要求至少4位

    registerBtn.disabled = true;
    registerBtn.textContent = "Registering...";

    try {
        // ✅ 修改：API 路径从 /api/register 改为 /api/auth/register
        const result = await request("/api/auth/register", {
            body: JSON.stringify({ username, password, confirmPassword }),
        });

        // ✅ 修改：检查返回的消息
        if (result.message === "Registration successful") {
            alert("注册成功，请登录");
            window.location.href = "login.html";
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
```

**关键改动：**
- ❌ 删除 `window.fetch` Mock 拦截器（第 9-31 行）
- ✅ API 路径改为 `/api/auth/register`
- ✅ 添加 `confirmPassword` 到请求体
- ✅ 密码长度检查改为至少 4 位（与后端一致）

---

### 步骤 4：重写 `js/resume.js`

添加完整的前后端交互逻辑：

```javascript
"use strict";
import request from "./helper.js";

const userName = localStorage.getItem("username");
const firstName = document.getElementById("firstName");
const lastName = document.getElementById("lastName");
const email = document.getElementById("email");
const phoneNumber = document.getElementById("phone");
const designation = document.getElementById("designation");
const summary = document.getElementById("summary");
const logout = document.querySelector(".nav-link");
const saveBtn = document.querySelector(".btn-save");

// ==================== 加载用户资料 ====================
const loadProfile = async function () {
    try {
        const profile = await request("/api/profile", {
            method: "GET",  // ✅ 新增：使用 GET 请求
        });

        // 填充基本信息
        document.querySelector(".user-name").textContent = `${profile.firstName || ""} ${profile.lastName || ""}'s Profile`;
        firstName.value = profile.firstName || "";
        lastName.value = profile.lastName || "";
        email.value = profile.email || "";
        phoneNumber.value = profile.phone || "";
        designation.value = profile.designation || "";
        summary.value = profile.summary || "";
        document.getElementById("theme").value = `theme${profile.theme || 1}`;

        // 加载工作经历
        const jobList = document.querySelector(".job-list");
        jobList.innerHTML = "";
        if (profile.jobs && profile.jobs.length > 0) {
            profile.jobs.forEach((job) => addJobRow(job));
        }

        // 加载教育经历
        const eduList = document.querySelector(".edu-list");
        eduList.innerHTML = "";
        if (profile.educations && profile.educations.length > 0) {
            profile.educations.forEach((edu) => addEducationRow(edu));
        }

        // 加载技能
        const skillsList = document.querySelector(".skills-list");
        skillsList.innerHTML = "";
        if (profile.skills && profile.skills.length > 0) {
            profile.skills.forEach((skill) => addSkillRow(skill));
        }
    } catch (error) {
        console.error("加载资料失败:", error);
        alert("加载资料失败，请重新登录");
        window.location.href = "login.html";
    }
};

// ==================== 添加工作经历行 ====================
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

    // 删除按钮事件
    row.querySelector(".btn-delete").addEventListener("click", function () {
        row.remove();
    });
};

// ==================== 添加教育经历行 ====================
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

    // 删除按钮事件
    row.querySelector(".btn-delete").addEventListener("click", function () {
        row.remove();
    });
};

// ==================== 添加技能行 ====================
const addSkillRow = function (skill = "") {
    const skillsList = document.querySelector(".skills-list");
    const row = document.createElement("div");
    row.className = "skill-item";
    row.innerHTML = `
        <input type="text" placeholder="Skill Name" value="${skill}" class="skill-name">
        <button type="button" class="btn-delete">Delete</button>
    `;
    skillsList.appendChild(row);

    // 删除按钮事件
    row.querySelector(".btn-delete").addEventListener("click", function () {
        row.remove();
    });
};

// ==================== 添加按钮事件 ====================
document.querySelector(".experience-section .btn-add").addEventListener("click", function () {
    addJobRow();
});

document.querySelector(".education-section .btn-add").addEventListener("click", function () {
    addEducationRow();
});

document.querySelector(".skills-section .btn-add").addEventListener("click", function () {
    addSkillRow();
});

// ==================== 保存资料 ====================
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

    // 收集工作经历
    document.querySelectorAll(".job-item").forEach(function (row) {
        profile.jobs.push({
            company: row.querySelector(".job-company").value,
            designation: row.querySelector(".job-designation").value,
            startDate: row.querySelector(".job-start").value,
            endDate: row.querySelector(".job-end").value,
            currentJob: row.querySelector(".job-current").checked,
        });
    });

    // 收集教育经历
    document.querySelectorAll(".edu-item").forEach(function (row) {
        profile.educations.push({
            college: row.querySelector(".edu-college").value,
            qualification: row.querySelector(".edu-qualification").value,
            startDate: row.querySelector(".edu-start").value,
            endDate: row.querySelector(".edu-end").value,
            summary: row.querySelector(".edu-summary").value,
        });
    });

    // 收集技能
    document.querySelectorAll(".skill-item").forEach(function (row) {
        profile.skills.push(row.querySelector(".skill-name").value);
    });

    try {
        saveBtn.disabled = true;
        saveBtn.textContent = "Saving...";

        await request("/api/profile", {
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

// 绑定保存按钮
if (saveBtn) {
    saveBtn.addEventListener("click", saveProfile);
}

// ==================== 退出登录 ====================
if (logout) {
    logout.addEventListener("click", function (e) {
        e.preventDefault();
        localStorage.removeItem("username");
        window.location.href = "login.html";
    });
}

// ==================== 初始化 ====================
loadProfile();
```

**关键改动：**
- ✅ 添加 `loadProfile()` 函数从后端获取资料
- ✅ 添加 `addJobRow()`、`addEducationRow()`、`addSkillRow()` 动态添加行
- ✅ 添加删除按钮功能
- ✅ 添加 `saveProfile()` 保存资料到后端
- ✅ 调用 `/api/profile` API（GET 和 POST）

---

### 步骤 5：修改 `resume.html`

移除表单的 `action` 属性（因为使用 JavaScript 提交）：

```html
<!-- 修改前 -->
<form action="/save-profile" method="POST" class="profile-form">

<!-- 修改后 -->
<form class="profile-form" onsubmit="return false;">
```

---

## 修改清单

| 文件 | 需要修改的内容 | 状态 |
|------|---------------|------|
| `js/helper.js` | 添加 `credentials: "same-origin"` | ❌ 待修改 |
| `js/login.js` | 删除 Mock 拦截器，修改 API 路径 | ❌ 待修改 |
| `js/register.js` | 删除 Mock 拦截器，修改 API 路径 | ❌ 待修改 |
| `js/resume.js` | 添加完整的前后端交互逻辑 | ❌ 待修改 |
| `resume.html` | 移除表单 action 属性 | ❌ 待修改 |

---

## API 路径对照表

| 功能 | 原路径 | 新路径 | 方法 |
|------|--------|--------|------|
| 登录 | `/api/login` | `/api/auth/login` | POST |
| 注册 | `/api/register` | `/api/auth/register` | POST |
| 获取资料 | 无 | `/api/profile` | GET |
| 保存资料 | 无 | `/api/profile` | POST |
| 登出 | 无 | `/api/auth/logout` | POST |

---

## 测试流程

### 1. 启动后端

```bash
# 启动 MySQL
docker start mysql-standalone

# 启动 Spring Boot 应用
mvn spring-boot:run
```

### 2. 访问前端

打开浏览器访问：http://localhost:5000/login.html

### 3. 测试功能

1. **注册新用户**
   - 点击 "Create one"
   - 填写用户名、密码、确认密码
   - 点击 "Register"
   - 应该提示 "注册成功，请登录"

2. **登录**
   - 输入用户名和密码
   - 点击 "Sign in"
   - 应该跳转到简历编辑页面

3. **编辑资料**
   - 填写基本信息
   - 点击 "Add Job" 添加工作经历
   - 点击 "Add Education" 添加教育经历
   - 点击 "Add Skill" 添加技能
   - 点击 "Save" 保存
   - 应该提示 "保存成功！"

4. **退出登录**
   - 点击 "Logout"
   - 应该跳转到登录页面

---

## 常见问题

### Q1: 登录后跳转到简历页面，但显示 401 错误？

**原因：** Spring Security 拦截了 `/api/profile` 请求

**解决：** 确保 `SecurityConfiguration.java` 中配置了：
```java
.antMatchers("/api/**").permitAll()
```

### Q2: 保存资料时提示 "Not authenticated"？

**原因：** 请求中没有携带 Session

**解决：** 确保 `helper.js` 中有 `credentials: "same-origin"`

### Q3: 点击 "Add Job" 没有反应？

**原因：** JavaScript 文件加载失败或按钮事件未绑定

**解决：** 
- 打开浏览器控制台（F12）查看错误
- 确保 `resume.js` 正确加载（检查 `<script type="module" src="js/resume.js"></script>`）

### Q4: Mock 拦截器未删除导致的问题？

**现象：** 请求被拦截，无法到达后端

**解决：** 删除 `login.js` 和 `register.js` 中的 `window.fetch` 拦截器代码

---

## 开发建议

1. **使用浏览器开发者工具**
   - 按 `F12` 打开控制台
   - 查看 Network 标签的请求详情
   - 查看 Console 标签的错误信息

2. **后端日志**
   - 查看 Spring Boot 控制台输出
   - 检查是否有 API 请求到达后端

3. **调试技巧**
   - 在 JavaScript 中添加 `console.log()` 输出变量
   - 使用 `debugger;` 语句断点调试

---

## 后续优化

- [ ] 添加表单验证（实时验证）
- [ ] 添加加载动画
- [ ] 优化错误提示（使用 Toast 而非 alert）
- [ ] 添加 PDF 导出功能的前端按钮
- [ ] 响应式设计适配移动端

---

## 联系方式

如有问题，请提交 Issue 或联系项目维护者。
