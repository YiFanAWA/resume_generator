# Current State Audit

This file records current truth for the Resume Generator repo before new implementation work begins.

## Existing Truth Inventory

- Current repo/root: `D:/java_project/resume-generator`.
- Git branch and dirty state: `main` 跟踪 `origin/main`；当前存在后端包结构分层、测试 import、README/docs、Sliver adoption 文档补齐等未提交改动。
- Existing agent constitution files: `AGENTS.md` 已建立为根目录 agent 宪法；当前未发现 `CLAUDE.md`、`GEMINI.md`。
- Existing internal docs: 当前采用 `docs/` 作为过渡真源目录，核心索引为 `docs/README.md`。
- Existing external docs: 根目录 `README.md` 提供项目入口、本地运行、验证和文档入口。
- Entry points: React/Vite 前端入口 `/app/`；后端 REST API 入口 `/api/**`；公开分享入口 `/app/public-share?token={shareToken}`。
- Generated files and generators: React 构建产物输出到 `src/main/resources/static/app`；Flyway migration 位于 `src/main/resources/db/migration`。
- Test/build scripts: 后端 `.\mvnw.cmd test`；前端 `npm.cmd ci`、`npm.cmd run build`；压测 `perf/k6/resume-baseline.js`。
- Deployment/runtime scripts: `docker-compose.yml` 编排 MySQL、Redis、Spring Boot 应用；`Dockerfile` 构建前后端合并镜像。
- Known production/live evidence: 当前没有正式生产环境、监控告警、备份恢复或多实例高可用证据。
- Current open user requests: 使用今天下载的 Sliver skill 补齐项目框架治理文件，并保持当前后端分层改动。

## Product Boundary

- What the product currently is: 前后端分离的在线简历生成系统，包含简历编辑、PDF 导出、公开分享、安全配置、缓存和基础压测。
- What the product is not: 当前不是生产级 SaaS，不包含正式多租户、支付、AI 生成内容、生产监控告警和完整高可用部署。
- Inherited or stale product semantics: 早期静态页面和旧 `public-share.html` 仍作为兼容入口存在，但当前主入口是 React `/app/`。
- User-confirmed current direction: Spring Boot 3 + React/Vite + MySQL/Flyway + Redis/Caffeine，可逐步演进高并发、异步 PDF、分布式限流和访问统计。
- Concepts explicitly rejected by the user: 不要只给建议而不落地；不要写不可验证的“通过”；不要继续让后端核心类混在根包。

## Current Call Chains

- User-facing entry: 浏览器访问 `/app/`，React 页面负责登录、注册、简历编辑、公开分享和 PDF 触发。
- API/controller/adapter: `src/main/java/com/daemonsets/resumeportal/web` 负责 HTTP 路由、请求参数和响应封装。
- Core owner: `src/main/java/com/daemonsets/resumeportal/service` 负责认证、简历、分享和映射业务。
- Storage/config/schema: `repository` 负责 JPA 查询；`model` 负责 JPA Entity；Flyway 负责 schema 迁移；`config` 负责 Spring Security/Async。
- Runtime/apply path: Spring Boot 应用由 `ResumePortalApplication` 启动，默认扫描根包及子包。
- UI/CLI/operator surface: `frontend/src` 是用户 UI；`docker compose`、Maven Wrapper、npm scripts、k6 是操作入口。
- Observability/evidence source: Actuator health/info/metrics、Micrometer PDF/cache 指标、Maven/JUnit 输出、k6 输出和 Docker logs。

## Owner Map

- Single owner layer: 后端以 `web/service/repository/model/config/cache/pdf/ratelimit` 为单一路径 owner map。
- Shared contracts: API 结构当前由 Controller/Service Map 响应和测试约束，后续应逐步收敛为 DTO/validation 合同。
- Adapter boundaries: `web` 只做 HTTP 适配，不承载业务规则。
- UI boundaries: React 只消费 API 结果并展示，不复制后端分享安全、访问次数或 PDF 规则。
- Forbidden owner layers: 禁止让 Controller、React 本地状态、静态页面、脚本或 README 成为核心业务真源。

## Dangerous Adoption Actions

These actions require user confirmation before execution:

- Create or rewrite `AGENTS.md`: 本次已由用户明确要求“补齐”后创建；后续改写仍需确认。
- Declare `dev-docs/` as internal source truth: 未执行；当前只声明 `docs/` 为过渡真源。
- Migrate, archive, or delete old docs: 未执行；只补齐索引和修正旧路径。
- Reframe product name, public positioning, or UI copy: 未执行。
- Delete legacy API/fields/routes/config: 未执行。
- Create nested `dev-docs` git repo: 未执行。
- Bulk rewrite README/docs: 未执行；仅做必要索引和路径同步。

## Handoff State

- Current git status captured: `git status --short` 显示后端类移动、测试 import 修改、README/docs 修改和新 adoption 文档未提交。
- Main repo latest commit: `d59c46e feat: harden resume generator architecture`。
- Nested repo latest commit, if any: 当前未发现需要单独处理的嵌套仓库。
- Running services/ports: 本次未启动或验证本地服务；默认后端端口 `5000`，MySQL Compose 映射 `6603`，Redis `6379`。
- Current validation evidence: `check_project_guardrails.py D:\java_project\resume-generator --mode adoption --truth-dir docs` 于 2026-06-14 运行通过；`.\mvnw.cmd test` 于 2026-06-14 01:27 运行通过，`Tests run: 15, Failures: 0, Errors: 0, Skipped: 0`。
- Known drift risks: k6、真实 MySQL/Redis、Docker build、远程 GitHub Actions 未在本轮验收；公开统计仍直接落库，PDF 仍同步导出。
- Next safe command: `git status --short`，确认当前包移动和治理文档补齐改动范围，再决定是否提交。

## Stop Condition

- Adoption is complete when: `AGENTS.md`、`docs/README.md`、`docs/current-state-audit.md`、`docs/architecture.md`、`docs/acceptance.md` 存在且 guardrail adoption 检查通过，后端测试通过。
- Do not start feature implementation until: 当前包结构、文档索引、验收命令和未验收风险已写回并被下一位 agent 读取。
