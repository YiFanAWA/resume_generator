# Acceptance

## Required Gates

- Code/build gates: 后端改动至少运行 `.\mvnw.cmd test`；包移动或依赖升级后优先运行 `.\mvnw.cmd clean test`；前端改动运行 `npm.cmd ci` 和 `npm.cmd run build`。
- Contract/API gates: 涉及 `/api/**` 时检查 CSRF、认证、公开字段过滤、分享密码/过期/访问次数、PDF content type 和结构化错误。
- UI/CLI gates: 涉及 React 页面时至少完成 Vite build；涉及用户流程时补浏览器或 E2E 证据。
- Docs/index gates: 架构、行为、配置、验收变化必须更新 `docs/README.md` 和对应专题文档；新增 active doc 必须从索引可达。
- Live/user evidence gates: 生产、多实例、k6、Docker build、真实 MySQL/Redis、远程 CI 只有当前日志或命令证据时才能标记通过。
- Git boundary gates: 提交前必须查看 `git status --short`，显式 stage 文件，禁止 `git add .`。

## Evidence Log

- Current truth audit evidence: 已读取当前代码树、`docs/README.md`、Git 状态、最近提交 `d59c46e`、Sliver guardrail 脚本和 adoption 模板。
- Constitution decision evidence: 用户在 guardrail 缺少 `AGENTS.md` 等文件后明确要求“你进行补齐”，因此本轮允许创建 root `AGENTS.md`。
- Source-truth index evidence: 当前采用 `docs/` 作为过渡真源目录，`docs/README.md` 链接 current-state、architecture、acceptance 和后端包结构说明。
- Owner boundary evidence: 后端类已移动到 `web/service/repository/model/config/cache/pdf/ratelimit`；`ResumePortalApplication` 留在根包负责扫描。
- Validation evidence: Sliver guardrail `check_project_guardrails.py D:\java_project\resume-generator --mode adoption --truth-dir docs` 运行通过；`.\mvnw.cmd test` 于 2026-06-14 01:27 运行通过，结果为 `Tests run: 15, Failures: 0, Errors: 0, Skipped: 0`。
- User-confirmed dangerous actions: 创建 `AGENTS.md` 和补齐 adoption 真相文件已由用户确认；未创建 `dev-docs`，未迁移/删除旧 docs。

## Stop Conditions

- Adoption can be considered complete when: `check_project_guardrails.py D:\java_project\resume-generator --mode adoption --truth-dir docs` 通过，`.\mvnw.cmd test` 通过，旧包路径扫描无残留。
- The next feature/refactor can start when: 当前未提交改动被用户确认是否提交，下一步任务有明确 owner 层、验收命令和文档写回位置。
- Do not claim ready if: 未跑当前命令、k6/真实 MySQL/Redis/Docker/远程 CI 没有证据，或 Git 仍有不明来源的脏改动。

## Drift Checklist

- Did the agent inspect current code and git state? 是，已检查 `git status --short`、代码树和 docs。
- Did the agent avoid treating the repo as empty? 是，采用 mid-project adoption，并保留现有 docs 体系。
- Did the agent identify the current owner layer? 是，owner map 固定为 `web/service/repository/model/config/cache/pdf/ratelimit`。
- Did the agent keep old product semantics out of current docs? 是，旧静态入口仅作为兼容跳转说明，不作为主架构。
- Did the agent ask before dangerous adoption actions? 是，上一轮已停止；本轮用户明确要求补齐后才创建 `AGENTS.md`。
- Did internal docs stay internal and public docs stay public? 当前过渡期使用 `docs/` 承载项目真相；未声明 `dev-docs` 为唯一真源。
- Did the handoff state include dirty files and next commands? 是，见 `docs/current-state-audit.md` 的 Handoff State。

## Drift Lock

- Current source truth beats memory and old sessions.
- User corrections override agent taste.
- Missing `dev-docs` is a bootstrap/adoption trigger, not an excuse to skip work.
- Dangerous adoption actions stop for user confirmation.
- 后续 agent 必须先读 `AGENTS.md` 和 `docs/README.md`，再决定 owner 层和验收命令。
