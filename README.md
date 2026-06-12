# Resume Generator

一个前后端分离的在线简历生成系统。当前项目形态是 React/Vite 前端 + Spring Boot 3 后端 + MySQL/Flyway + HTML to PDF + 公开分享 + 缓存 + Docker/CI/k6 压测基线。

## 当前状态

- 前端入口：`/app/`
- 后端接口：`/api/**`
- 认证方式：Spring Security Session/Cookie + CSRF
- 数据库：MySQL 8，使用 Flyway 管理 schema
- PDF 导出：Thymeleaf 模板 + OpenHTMLToPDF
- 公开分享：分享链接、密码保护、过期时间、访问次数限制、访问统计
- 缓存：本地 Caffeine，可切换 Redis
- 工程化：Docker Compose、GitHub Actions、k6 压测脚本

## 本地运行

启动 MySQL 和 Redis：

```powershell
docker compose up -d mysql redis
```

启动后端：

```powershell
.\mvnw.cmd spring-boot:run
```

访问：

```text
http://localhost:5000/app/
```

前端开发模式：

```powershell
cd frontend
npm.cmd install
npm.cmd run dev
```

## 验证

后端测试：

```powershell
.\mvnw.cmd test
```

前端构建：

```powershell
cd frontend
npm.cmd ci
npm.cmd run build
```

k6 压测：

```powershell
.\.tools\k6\k6.exe run -e BASE_URL=http://localhost:5000 -e USERNAME=alice -e PASSWORD=password123 -e SHARE_TOKEN=your-share-token perf/k6/resume-baseline.js
```

## 文档

更完整的项目进度、架构说明、验收方式和后续规划见 [docs/README.md](docs/README.md)。
