# 公开分享简历页面 - 前端开发文档

## 📋 项目概述

创建公开分享简历的静态 HTML 页面（`public-share.html`），实现前后端分离架构下的简历展示功能。

### 架构说明
- **前端**：纯 HTML + CSS + JavaScript（无需框架）
- **后端**：Spring Boot REST API（已完成）
- **通信方式**：前端通过 Fetch API 调用后端接口获取 JSON 数据

### 访问流程
```
用户访问：http://服务器地址/public-share.html?token=xxx
    ↓
前端 JavaScript 提取 token
    ↓
调用 API：GET /api/public/{token}
    ↓
后端返回 JSON 数据
    ↓
前端渲染为可视化简历页面
```

---

## 🎯 功能需求

### 1. URL 参数解析
- 从 URL 查询参数中提取 `token`（格式：`?token=243dd82a-0834-4914-83c5-17fc95d4a0b5`）
- 使用 `URLSearchParams` API 提取参数
- 如果 URL 中没有 `token` 参数，显示错误提示："Invalid Link - Missing share token"

### 2. API 数据获取
- 调用后端接口：`GET /api/public/{token}`
- 使用 `fetch()` API 发起请求
- 处理以下情况：
  - ✅ 200 OK：成功获取数据，渲染页面
  -  404 Not Found：显示 "Resume not found or private"
  - ❌ 网络错误：显示 "Error loading resume, please try again later"

### 3. 数据渲染

#### 基本信息区域
| 字段 | 显示规则 |
|------|---------|
| `firstName` + `lastName` | 合并显示为标题 |
| `designation` | 显示为副标题（可选） |
| `summary` | 显示为个人简介段落（可选） |

#### 工作经历区域（`jobs` 数组）
遍历显示每项工作经历：
- `company`：公司名称
- `designation`：职位
- `formattedStartDate` + `formattedEndDate`：时间段
- `currentJob`：如果为 `true`，结束时间显示 "Present"
- `responsibilities`：职责描述（可选）

#### 教育经历区域（`educations` 数组）
遍历显示每项教育经历：
- `college`：学校名称
- `qualification`：学历/专业
- `formattedStartDate` + `formattedEndDate`：时间段
- `summary`：描述（可选）

#### 技能区域（`skills` 数组）
- 以标签/徽章（tag/badge）形式展示
- 水平排列，自动换行

### 4. 打印功能
- 在页面右上角添加 "Print Resume" 按钮
- 点击按钮调用 `window.print()`
- 打印样式优化：
  - 隐藏打印按钮
  - 移除阴影和背景色
  - 优化页面边距

### 5. 加载状态
- 页面初始化时显示 "Loading resume..." 文字
- 可选：添加加载动画（spinner）

### 6. 错误处理
所有异步操作必须包含 try-catch，并显示友好的错误信息。

---

## 📡 API 接口文档

### 获取公开简历数据

**请求方法**：`GET`

**URL**：`/api/public/{shareToken}`

**完整示例**：
```javascript
fetch('/api/public/243dd82a-0834-4914-83c5-17fc95d4a0b5')
    .then(response => response.json())
    .then(data => {
        // 处理数据
    })
    .catch(error => {
        // 处理错误
    });
```

**成功响应（200 OK）**：
```json
{
    "firstName": "Isaac",
    "lastName": "Newton",
    "designation": "Mathematician, physicist, astronomer, theologian, and author",
    "summary": "一位杰出的科学家和数学家...",
    "jobs": [
        {
            "id": 8,
            "company": "剑桥大学",
            "designation": "卢卡斯数学教授",
            "startDate": "2020-07-25",
            "endDate": "2026-07-08",
            "responsibilities": "负责数学教学与研究",
            "formattedStartDate": "7月 2020",
            "formattedEndDate": "7月 2026",
            "currentJob": false
        }
    ],
    "educations": [
        {
            "id": 6,
            "college": "剑桥大学三一学院",
            "qualification": "学士/硕士",
            "startDate": null,
            "endDate": null,
            "summary": "",
            "formattedStartDate": null,
            "formattedEndDate": null
        }
    ],
    "skills": ["Java", "Spring Boot", "MySQL"],
    "theme": 1
}
```

**失败响应（404 Not Found）**：
```json
{
    "error": "Resume not found or private"
}
```

---

## 🎨 UI/UX 规范

### 响应式布局

| 断点 | 布局策略 |
|------|---------|
| 桌面端（> 768px） | 居中容器，最大宽度 900px，padding 40px |
| 平板端（480px - 768px） | padding 30px，调整字体大小 |
| 手机端（< 480px） | 全宽显示，padding 20px，缩小字体 |

### 视觉设计原则

#### 整体风格
- 简洁专业的简历风格
- 白色背景，深色文字
- 浅灰色背景用于区分区块

#### 排版层次
```
一级标题（姓名）：32px，粗体，居中
二级标题（职位）：18px，常规，居中，灰色
区块标题（工作经历/教育经历/技能）：22px，粗体，带下划线
内容标题（公司/学校名称）：18px，粗体
内容副标题（职位/学历）：14px，灰色
时间信息：13px，浅灰色
描述文本：14px，常规行高 1.5-1.6
```

#### 间距规范
- 区块之间：30px margin-bottom
- 卡片之间：20px margin-bottom
- 卡片内边距：15px padding
- 标题下边距：15px margin-bottom

### 打印样式（@media print）
```css
@media print {
    .print-btn {
        display: none;
    }
    body {
        padding: 0;
    }
    .container {
        box-shadow: none;
        max-width: 100%;
    }
}
```

---

## 📁 文件位置

```
src/main/resources/static/
├── public-share.html          ← 新文件（需要创建）
── login.html
├── register.html
── resume.html
├── js/
│   ├── login.js
│   ├── register.js
│   └── resume.js
└── css/
    └── style.css
```

**文件路径**：`src/main/resources/static/public-share.html`

---

## 🔒 安全规范

### 数据过滤
- ✅ 后端已自动过滤敏感信息（`email`、`phone` 不会返回）
- ✅ 前端不应尝试获取这些字段

### 权限控制
- ✅ 此页面为公开访问，**不需要**用户登录
- ✅ **不需要**调用需要认证的 API（如 `/api/profile`）
- ✅ 只读展示，不提供编辑/保存功能

### Token 验证
- Token 验证由后端负责
- 前端只需处理 404 响应即可

---

## ❌ 不需要实现的功能

- ❌ 登录/注册功能
- ❌ 简历编辑功能
- ❌ 数据保存/提交
- ❌ 主题切换（使用默认样式）
- ❌ PDF 导出（使用浏览器打印替代）
- ❌ 图片上传（头像等）
- ❌ 动画效果（保持简洁）

---

## ✅ 测试清单

### 功能测试
- [ ] 有效 token 能正确显示简历
- [ ] 无效 token（不存在的）显示 "Resume not found or private"
- [ ] 缺失 token 参数显示 "Invalid Link"
- [ ] 网络断开时显示友好错误提示
- [ ] 打印按钮点击后调用浏览器打印
- [ ] 打印预览样式正确（无按钮、无边框阴影）

### 数据展示测试
- [ ] 姓名正确显示
- [ ] 职位正确显示（如果有）
- [ ] 个人简介正确显示（如果有）
- [ ] 工作经历列表完整显示
- [ ] 在职工作显示 "Present" 而非结束时间
- [ ] 教育经历列表完整显示
- [ ] 技能以标签形式正确显示
- [ ] 空字段不显示对应区块（如 `summary` 为空则不显示简介区块）

### 响应式测试
- [ ] 桌面端（1920x1080）显示正常
- [ ] 平板端（768x1024）显示正常
- [ ] 手机端（375x667）显示正常
- [ ] 打印预览样式正确

### 浏览器兼容性
- [ ] Chrome/Edge（最新）
- [ ] Firefox（最新）
- [ ] Safari（最新）

---

## 📝 开发时间估算

| 任务 | 预计时间 |
|------|---------|
| HTML 结构搭建 | 30 分钟 |
| CSS 样式编写 | 1 小时 |
| JavaScript 逻辑实现 | 1 小时 |
| 响应式适配 | 30 分钟 |
| 打印样式优化 | 30 分钟 |
| 测试与调试 | 1 小时 |
| **总计** | **约 4.5 小时** |

---

##  技术栈要求

### 必须使用
- HTML5
- CSS3（内联样式，无需外部 CSS 文件）
- JavaScript ES6+（使用 `fetch()`、箭头函数等现代语法）

### 禁止使用
- jQuery
- Vue.js / React / Angular
- Bootstrap / Tailwind 等 CSS 框架
- 任何第三方 UI 库

### 代码规范
- 使用语义化 HTML 标签（`<header>`、`<section>`、`<footer>`）
- CSS 类名使用 BEM 或 kebab-case 命名
- JavaScript 使用 `async/await` 处理异步
- 保持代码简洁，添加必要注释

---

## 🚀 快速开始

### 1. 创建文件
在 `src/main/resources/static/` 目录下创建 `public-share.html`

### 2. 基础结构
```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Public Resume</title>
    <style>
        /* 内联 CSS */
    </style>
</head>
<body>
    <div class="container" id="resumeContainer">
        <div class="loading">Loading resume...</div>
    </div>
    
    <script>
        // JavaScript 逻辑
    </script>
</body>
</html>
```

### 3. 核心逻辑
```javascript
async function loadResume() {
    const container = document.getElementById('resumeContainer');
    
    try {
        // 1. 提取 token
        const urlParams = new URLSearchParams(window.location.search);
        const token = urlParams.get('token');
        
        if (!token) {
            container.innerHTML = '<div class="error">Invalid Link</div>';
            return;
        }
        
        // 2. 调用 API
        const response = await fetch(`/api/public/${token}`);
        
        if (!response.ok) {
            container.innerHTML = '<div class="error">Resume not found</div>';
            return;
        }
        
        // 3. 渲染数据
        const profile = await response.json();
        renderResume(profile);
        
    } catch (error) {
        container.innerHTML = '<div class="error">Error loading resume</div>';
    }
}

function renderResume(profile) {
    // 将 JSON 数据渲染为 HTML
}

// 页面加载时执行
loadResume();
```

---

##  联系方式

如有疑问，请联系后端开发人员。

**API 地址**：`http://localhost:5000/api/public/{token}`

**测试 token 示例**：`243dd82a-0834-4914-83c5-17fc95d4a0b5`

---

## 📌 注意事项

1. **前后端分离**：前端只负责展示，不调用需要认证的 API
2. **错误处理**：所有异步操作必须包含 try-catch
3. **用户体验**：加载状态、错误提示要友好
4. **响应式设计**：必须适配手机、平板、桌面端
5. **代码简洁**：保持代码清晰，添加必要注释
6. **无依赖**：不使用任何第三方库或框架

---

## 📄 参考文档

- [前端修改指南](docs/FRONTEND_MODIFICATION_GUIDE.md)
- [功能路线图](docs/FEATURE_ROADMAP.md)

---

**文档版本**：v1.0  
**创建日期**：2026-06-03  
**最后更新**：2026-06-03
