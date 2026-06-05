# 当前稳定化与前后端分离计划

本文记录当前阶段的落地目标，避免在基础治理未完成时过早引入 Redis、消息队列、AI 等扩展能力。

## 当前阶段目标

- React/Vite 作为主要前端工程，Spring Boot 只负责 API、认证、数据持久化和 PDF 渲染。
- 旧静态 HTML 页面不再作为主入口，统一转向 `/app/`。
- `/api/**` 保持稳定，供 React 前端直接调用。
- PDF 导出从手写 OpenPDF 排版迁移为 Thymeleaf 模板 HTML to PDF。
- 建立可重复的测试和压测基线，为后续高并发改造提供数据依据。

## 已完成

- 新建 `frontend/` React/Vite 工程。
- React 页面已接入登录、注册、简历编辑、分享链接、公开分享页、PDF 导出等 `/api/**` 接口。
- Vite 构建产物输出到 `src/main/resources/static/app`。
- Spring Boot 入口 `/`、旧静态页面路径转向 React `/app/`。
- 修复安全配置和 `application.properties` 中的编码/配置问题。
- PDF 导出改为 Thymeleaf 模板渲染 HTML，再使用 OpenHTMLToPDF 输出 PDF。
- 移除旧 OpenPDF 依赖和 `com.lowagie` 代码引用。
- 增加 PDF 导出测试，确认接口返回真实 PDF。
- 后端 MockMvc 测试覆盖注册、认证、资料更新、分享生成/撤销、公开访问、越权访问和 PDF 导出。
- 增加 `dev/test/prod` 配置拆分、基础文件日志、安全 Cookie 和生产环境配置说明。

## 本轮收口任务

- 修复 README 和 docs 文档，使其反映当前真实架构。
- 为 PDF 渲染增加专用 print/PDF CSS，减少浏览器页面样式对 PDF 的干扰。
- 添加压测基线说明和 k6 脚本。

## 下一阶段建议

1. 先跑压测，记录当前基线。
2. `/api/public/{shareToken}` 已加入本地缓存和可选 Redis 后端，下一步启用 Redis 后复跑压测。
3. 如果 PDF 导出 P95 明显偏高，再做异步导出任务。
4. 在引入 RabbitMQ 前，先用 Spring `@Async` + 自定义线程池验证异步模型。
5. 最后再补缓存命中率、限流、熔断、Prometheus/Grafana 监控。

## 暂不建议立即做

- 不建议立刻上 RabbitMQ。当前没有足够多异步任务场景，先用线程池更轻。
- 不建议立刻上 AI 功能。AI 会放大成本、限流和异步任务复杂度，应在核心链路稳定后做。
- 不建议在没有压测数据时盲目调整数据库连接池或 Redis 参数。
