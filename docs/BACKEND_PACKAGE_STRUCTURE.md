# 后端包结构说明

本文记录当前 Spring Boot 后端分层后的包结构，作为后续重构和新功能落位的 owner map。

## 包结构

```text
com.daemonsets.resumeportal
├── ResumePortalApplication
├── cache
│   ├── PublicResumeCacheProperties
│   └── PublicResumeCacheService
├── config
│   ├── AsyncConfiguration
│   └── SecurityConfiguration
├── model
│   ├── Education
│   ├── Job
│   ├── MyUserDetails
│   ├── User
│   └── UserProfile
├── pdf
│   ├── PdfExportProperties
│   ├── PdfExportResult
│   └── PdfExportService
├── ratelimit
│   ├── RateLimitProperties
│   └── SimpleRateLimitFilter
├── repository
│   ├── UserProfileRepository
│   └── UserRepository
├── service
│   ├── AuthService
│   ├── MyUserDetailsService
│   ├── ProfileMapper
│   ├── ProfileService
│   └── ShareService
└── web
    ├── ApiController
    ├── ApiErrorResponse
    ├── ApiExceptionHandler
    └── HomeController
```

## 分层职责

- `web`：HTTP 入口层，只负责请求参数、路由、响应封装和异常响应。
- `service`：业务层，负责认证、简历编辑、公开分享规则和对象映射。
- `repository`：数据访问层，放 Spring Data JPA Repository。
- `model`：数据模型层，放 JPA Entity 和 Spring Security 用户详情模型。
- `config`：应用配置层，例如 Spring Security 和异步线程池配置。
- `cache`：公开简历缓存基础设施，负责 Caffeine/Redis 切换和降级。
- `pdf`：HTML to PDF 导出基础设施和 PDF 导出配置。
- `ratelimit`：请求限流基础设施。

## 设计约束

`ResumePortalApplication` 保留在根包，确保 Spring Boot 能自动扫描所有子包。

新增功能时优先按 owner 落位：

- HTTP 协议适配放 `web`。
- 可复用业务规则放 `service`。
- 数据查询和锁语义放 `repository`。
- PDF、缓存、限流等横向能力放对应基础设施包。
