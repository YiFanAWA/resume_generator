# Resume Generator 项目说明

## 项目定位

Resume Generator 是一个前后端分离的在线简历生成系统。前端使用 React/Vite 提供登录、简历编辑、实时预览、公开分享管理；后端使用 Spring Boot 3 提供 REST API、认证授权、简历数据管理、PDF 渲染、分享安全控制、缓存与基础观测能力。

这个项目不只是 CRUD 展示，而是围绕真实 Web 产品常见问题做了工程化迭代：前后端分离、认证安全、数据库迁移、PDF 跨平台导出、缓存、Docker、CI、压测基线和后续高并发演进规划。

## 当前完成度

可以理解为：工程化 MVP 已成型，适合作为简历项目展示；但还不能包装成完整生产级系统。

已经完成：

- React/Vite 前端已替代早期静态页面，统一从 `/app/` 访问。
- Spring Boot 已升级到 3.3.x，运行基础为 Java 17。
- 后端接口按 `/api/**` 提供 REST API。
- 登录、注册、当前用户、退出登录已接入 Spring Security Session/Cookie。
- CSRF 防护已启用，前端和 k6 脚本会先获取 `/api/csrf` 再提交写请求。
- `ApiController` 已拆分为 Controller + Service + Mapper，Controller 只保留 HTTP 入参出参逻辑。
- 简历支持基础信息、工作经历、教育经历、技能、主题编辑。
- PDF 导出已从手写 OpenPDF 排版迁移为 Thymeleaf HTML 模板 + OpenHTMLToPDF。
- PDF 专用 CSS、classpath 静态资源读取和中文字体加载已处理，降低 JAR/Docker/Linux 部署乱码风险。
- 项目内置 `NotoSansSC-VF.ttf`，避免 Linux/Docker 中缺少中文字体导致 PDF 方框。
- 公开分享支持生成、撤销、密码保护、过期时间、最大访问次数、访问统计。
- 分享访问计数使用数据库悲观锁降低并发写丢失风险。
- 公开简历支持 Caffeine 本地缓存，并可切换 Redis。
- Flyway migration 管理数据库结构，避免依赖 Hibernate 自动改表。
- Docker Compose 可编排 MySQL、Redis、Spring Boot 应用。
- GitHub Actions CI 已配置前端依赖安装、前端构建、后端测试、后端打包。
- k6 压测脚本已建立，并支持 CSRF 后的登录、保存、公开访问、PDF 导出流程。
- Actuator 已开放 health/info/metrics，非公开 Actuator 端点需要管理员角色。
- 已加入基础限流：登录/注册、公开访问、PDF 导出、普通 API 可分别配置容量。
- PDF 导出已加入输入内容大小和输出文件大小限制。
- data.sql 示例账号密码已改为 BCrypt hash，默认不会自动执行 seed 数据。

暂未完成：

- 没有正式生产部署方案，例如 Nginx、HTTPS、多实例、负载均衡、灰度发布、回滚策略。
- 没有正式监控告警平台，例如 Prometheus/Grafana、告警规则、容量仪表盘。
- 没有备份恢复策略，例如 MySQL 定时备份、恢复演练、Redis 持久化策略说明。
- 没有 Testcontainers 级别的真实 MySQL/Redis 集成测试。
- 前端还没有 Vitest、Testing Library 或 Playwright E2E 测试。
- PDF 导出仍是同步接口，高并发下可能占用请求线程。
- 公开访问统计仍直接落库，高访问量时应演进为 Redis 计数或异步批量落库。

## 后端架构

当前后端的主要分层如下：

```text
Controller
  -> AuthService
  -> ProfileService
  -> ShareService
  -> ProfileMapper
  -> Repository
  -> MySQL

PDF Export
  -> ProfileService
  -> PdfExportService
  -> Thymeleaf Template
  -> OpenHTMLToPDF

Public Share
  -> ShareService
  -> PublicResumeCacheService
  -> Caffeine or Redis
  -> MySQL
```

核心职责：

- `ApiController`：只负责 HTTP 路由、参数接收、响应封装。
- `AuthService`：负责登录、注册、密码加密、旧明文密码兼容升级。
- `ProfileService`：负责简历查询、保存、PDF 导出前限制检查、公开缓存失效。
- `ShareService`：负责分享链接生成、撤销、密码、过期时间、访问次数限制、公开访问。
- `ProfileMapper`：负责实体对象到 API Map 的转换，避免 Controller 直接暴露 JPA 实体细节。
- `PdfExportService`：负责模板渲染、PDF 专用样式注入、字体加载、HTML to PDF。
- `PublicResumeCacheService`：负责本地缓存、Redis 缓存、缓存指标和失败降级。

## 数据库设计

主要实体：

- `User`：用户账号、密码、启用状态、角色。
- `UserProfile`：简历基础信息、主题、公开分享状态、分享安全配置、访问统计。
- `Job`：工作经历。
- `Education`：教育经历。
- `skills`：通过 element collection 保存技能列表。
- `job_responsibilities`：通过 element collection 保存工作职责列表。

当前数据库结构由 Flyway 管理：

- `V1__init_resume_portal_schema.sql`：初始化用户、简历、工作、教育、技能等基础表。
- `V2__add_share_security_and_view_stats.sql`：增加分享密码、过期时间、访问次数限制、访问统计。
- `V3__expand_resume_text_fields.sql`：扩大简历长文本字段，降低长内容保存失败风险。

## 安全与配置

已完成的安全治理：

- CSRF 防护开启，写请求必须携带 token。
- CORS 不允许 `*` 搭配 credentials，允许来源通过 `CORS_ALLOWED_ORIGINS` 配置。
- Session Cookie 默认 `HttpOnly`，`Secure` 会跟随 HTTPS 要求配置。
- HSTS 只在 `APP_SECURITY_REQUIRE_HTTPS=true` 时启用。
- 登录失败、未认证、无权限等 API 错误返回统一 JSON 结构。
- `/actuator/health` 和 `/actuator/info` 公开，其他 `/actuator/**` 需要管理员角色。
- 内置简单 IP 固定窗口限流，适合当前单实例阶段。

需要注意：

- 当前限流是单实例内存限流，多实例后应迁移到 Redis 或网关层。
- 当前 Session 认证适合单体部署，多实例后需要 sticky session、Spring Session Redis 或改 JWT。
- 生产环境必须使用强密码、HTTPS、最小权限数据库账号，不应复用 `.env.example` 默认密码。

## PDF 导出

当前 PDF 链路：

```text
UserProfile
  -> Thymeleaf profile template
  -> remove browser-only stylesheet
  -> inject pdf.css
  -> register CJK font
  -> OpenHTMLToPDF
  -> PDF byte array
```

已处理的问题：

- 使用 `classpath:/static/` 作为 PDF 静态资源 base URI，避免 JAR 内资源路径无法读取。
- 将网页样式和 PDF 样式隔离，避免浏览器样式影响 PDF 排版。
- 内置中文字体 `src/main/resources/fonts/NotoSansSC-VF.ttf`。
- Dockerfile 同时安装 `fonts-noto-cjk` 作为系统字体兜底。
- PDF 导出前检查简历文本长度，导出后检查 PDF 文件大小。
- 通过 Micrometer Timer 记录 `resume.pdf.export.duration`。

## Redis 能力

当前 Redis 主要用于公开简历缓存：

- 默认 dev 使用本地 Caffeine，不强制启动 Redis。
- prod 和 docker compose 可通过 `PUBLIC_RESUME_CACHE_BACKEND=redis` 切换 Redis。
- Redis 缓存 key 使用 `resume:public:{shareToken}`。
- Redis 失败时会记录指标并回退到本地缓存，避免缓存异常直接打断核心业务。

后续可扩展方向：

- 使用 Redis 做分布式限流。
- 使用 Redis 记录公开访问计数，再异步批量落库。
- 使用 Spring Session Redis 支持多实例 Session 共享。
- 使用 Redis 队列或 Stream 承接异步 PDF 任务。

## 验证方式

后端单元/集成测试：

```powershell
.\mvnw.cmd test
```

前端依赖与构建：

```powershell
cd frontend
npm.cmd ci
npm.cmd run build
```

k6 压测：

```powershell
.\.tools\k6\k6.exe run -e BASE_URL=http://localhost:5000 -e USERNAME=alice -e PASSWORD=password123 -e SHARE_TOKEN=your-share-token perf/k6/resume-baseline.js
```

建议验收口径：

- 没有实际跑过的命令不要标记为通过。
- 没有日志、测试输出、接口返回截图或压测报告的结果，标记为未验收。
- 压测结果必须记录日期、机器环境、后端 profile、数据库类型、用户数、阈值和失败率。

## 当前最值得继续做的迭代

第一阶段：补测试和验收闭环。

- 增加 Testcontainers，使用真实 MySQL/Redis 跑核心流程。
- 给前端增加 Vitest 或 Playwright，覆盖登录、编辑、保存、分享、导出入口。
- 在 CI 中加入 Docker build，验证镜像可构建。

第二阶段：高并发保护。

- 将当前内存限流升级为 Redis 分布式限流。
- 将公开访问计数改为 Redis 原子计数 + 异步批量落库。
- 将 PDF 导出改为异步任务，避免大量导出占满 Tomcat 请求线程。

第三阶段：生产化。

- 增加 Nginx/HTTPS 部署文档。
- 增加 Prometheus/Grafana 指标采集与仪表盘。
- 增加 MySQL 备份恢复文档。
- 增加生产环境安全配置 checklist。

第四阶段：产品能力。

- 多模板体系。
- 区块开关与排序。
- 多份简历版本管理。
- ATS 友好导出和导出前质量检查。
- 公开主页和多份简历集合。

## 面试表达

可以这样介绍项目：

```text
这是一个前后端分离的在线简历生成系统。前端使用 React/Vite 实现登录、简历编辑、实时预览、PDF 导出和公开分享管理；后端使用 Spring Boot 3 提供 REST API，负责认证、简历数据管理、PDF 渲染和分享安全控制。数据库使用 MySQL，并通过 Flyway 管理结构版本。公开分享接口支持 Caffeine/Redis 缓存，同时通过 k6 建立压测基线。项目还接入了 Docker Compose 和 GitHub Actions CI，具备基础工程化能力。
```

如果想强调工程思考，可以补充：

```text
我没有只做页面和 CRUD，而是按真实项目思路处理了前后端分离、权限控制、CSRF、数据库迁移、缓存、PDF 跨平台导出、压测、CI、Docker，以及后续高并发场景下限流、异步化、监控和多实例扩展路线。
```
