# Architecture Boundary

## Decision

- The single recommended architecture is: React/Vite 作为前端 UI，Spring Boot 3 作为后端 API 与业务 owner，MySQL/Flyway 作为持久化 owner，Caffeine/Redis 作为缓存 owner，OpenHTMLToPDF 作为 PDF 渲染 owner。
- Why this matches current repo truth: 当前代码已经具备 `/app/` React 入口、`/api/**` REST API、Spring Security Session/CSRF、Flyway migrations、HTML to PDF、公开分享缓存、Docker Compose 和 k6 脚本。
- Why inherited or stale paths are not current authority: 旧静态页面和旧根包类路径只保留迁移/兼容意义；当前权威代码路径是分层后的 `web/service/repository/model/config/cache/pdf/ratelimit`。

## Owner Layers

- Core/domain owner: `service` 包，包含 `AuthService`、`ProfileService`、`ShareService`、`ProfileMapper`。
- Contract/schema owner: 现阶段由 API tests、Controller response、Flyway migration 和配置 metadata 共同约束；后续 DTO/Bean Validation 应进入合同层。
- Persistence/config owner: `repository`、`model`、`src/main/resources/db/migration`、`application*.properties`。
- Runtime/apply owner: `ResumePortalApplication`、`config`、Docker Compose、Maven/Node build scripts。
- Adapter/API owner: `web` 包，包含 `ApiController`、`HomeController`、`ApiExceptionHandler`、`ApiErrorResponse`。
- UI/CLI/operator owner: `frontend/src`、`README.md` 运行命令、`perf/k6` 压测入口。
- Evidence/observability owner: JUnit/Maven 输出、Actuator、Micrometer 指标、k6 输出、Docker logs。

## Forbidden Paths

- Do not put shared semantics in UI/controller/prompt/temp scripts.
- Do not preserve rejected legacy product semantics under softer names.
- Do not treat archive docs as current truth unless the active index points back to them.
- Do not add compatibility branches without a current migration decision.
- Do not let React 本地状态决定分享安全、访问次数、PDF 限制、认证状态或数据库真相。
- Do not let Controller 直接拥有注册、简历保存、分享访问、PDF 渲染等业务规则。

## Dependency Direction

```text
model / repository / migration contract
  -> service owner rules
  -> cache / pdf / ratelimit infrastructure
  -> web adapter
  -> frontend UI
  -> docs / acceptance evidence
```

## Current Contracts

- Request/command: `/api/auth/**`、`/api/profile/**`、`/api/public/**`，写请求必须携带 CSRF token。
- Response/report: 私有简历返回用户字段和分享状态；公开简历过滤 `email`、`phone`、`userName`、`shareToken`、`isPublic`。
- Error model: API 错误应收敛到 `ApiErrorResponse`，PDF 导出仍有少量 byte 文本错误响应需要后续收敛。
- Validation owner: 当前主要由 Service、Security、PDF 限制、数据库约束和测试覆盖；后续应补 DTO + Bean Validation。
- Generated source, if any: React build 产物位于 `src/main/resources/static/app`，不得把构建产物当成手写业务真源。

## Stop Condition

- This architecture boundary is accepted when: 新增后端代码能明确落到一个 owner 包，`mvnw test` 通过，相关文档路径不再指向旧根包类。
