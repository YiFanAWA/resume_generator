# Resume Generator 功能路线图

本文记录项目从当前前后端分离版本继续演进的路线。优先级原则是：先稳定核心链路，再做高并发基础设施，最后做 AI 和生态扩展。

## 当前状态

- React/Vite 前端已建立，主入口为 `/app/`。
- Spring Boot 提供 `/api/**` REST API。
- Session + Cookie 认证已可被 React 前端复用。
- PDF 导出已迁移为 Thymeleaf HTML to PDF。
- 公开分享链路已可用。
- `/api/public/{shareToken}` 已支持本地 Caffeine 缓存，并可切换 Redis 后端。
- 后端核心接口已有 MockMvc 测试。

## 第一阶段：稳定化和可观测基线

目标：确认当前系统真实性能和瓶颈，避免凭感觉引入中间件。

任务：

- 修复文档和开发运行说明。
- PDF 增加专用 print/PDF CSS。
- 建立 k6 压测脚本。
- 记录以下接口的基线数据：
  - `GET /api/public/{shareToken}`
  - `GET /api/profile`
  - `PUT /api/profile`
  - `GET /api/profile/export/pdf`
- 明确 P95、P99、错误率、吞吐量、PDF 平均生成耗时。

交付物：

- `docs/PERFORMANCE_BASELINE.md`
- `perf/k6/resume-baseline.js`
- 一份压测结果记录表。

## 第二阶段：读多写少缓存

目标：优先优化最适合缓存的公开分享页。

已完成：

- 本地开发默认使用 Caffeine 缓存。
- 部署环境可通过 `PUBLIC_RESUME_CACHE_BACKEND=redis` 使用 Redis。
- 缓存 `/api/public/{shareToken}` 返回的过滤后 DTO，而不是 JPA 实体。
- 用户更新简历、生成分享、撤销分享时，在事务提交后清理相关缓存。
- MockMvc 已覆盖缓存命中、缓存写入、缓存清理策略。

下一步：

1. 启动 Redis 后复跑 k6，记录缓存前后公开分享接口 P95/P99。
2. 为缓存命中率、miss 次数、Redis 降级次数补 Micrometer 指标。
3. 公开访问量上来后，再评估是否需要 Redis 集群或 CDN 层缓存。

为什么先做公开分享缓存：

- 公开分享是读多写少。
- 不依赖当前登录 session。
- 数据过滤后 DTO 比实体更适合缓存。
- 对后续 SEO、公开访问统计、二维码分享都有帮助。

## 第三阶段：PDF 异步导出

目标：避免 PDF 生成在高并发下阻塞请求线程。

建议先做轻量版本：

- 新增导出任务表或内存任务状态。
- `POST /api/profile/export/pdf/jobs` 创建任务，返回 `jobId`。
- `GET /api/profile/export/pdf/jobs/{jobId}` 查询状态。
- `GET /api/profile/export/pdf/jobs/{jobId}/download` 下载结果。
- 使用自定义 `ThreadPoolTaskExecutor` 执行导出。

等多实例部署或任务可靠性要求上升时，再引入 RabbitMQ。

## 第四阶段：限流和监控

目标：让系统在异常流量下可控。

建议：

- 给公开接口和 PDF 导出接口加限流。
- 使用 Actuator + Micrometer 暴露指标。
- 接入 Prometheus/Grafana。
- 重点观察：
  - API P95/P99
  - PDF 生成耗时
  - 数据库连接池使用率
  - 缓存命中率
  - JVM GC 和内存

## 第五阶段：产品能力增强

在核心链路稳定后再做：

- 更多简历模板。
- 模板预览和模板元数据管理。
- 简历浏览统计。
- 多版本简历管理。
- 二维码分享。
- AI 简历总结、技能建议、ATS 检查。
- GitHub 集成。

## 不建议过早做的事项

- 没有压测数据前，不建议直接上 RabbitMQ。
- 没有公网分享访问量前，不建议复杂化 Redis 集群。
- 没有成本和限流设计前，不建议接入 AI。
- 没有多租户需求前，不建议过早拆微服务。
