# 公开分享页说明

公开分享页已经从旧静态 `public-share.html` 迁移到 React 页面。

## 访问路径

```text
/app/public-share?token={shareToken}
```

旧地址仍保留兼容跳转：

```text
/public-share.html?token={shareToken}
```

后端会将旧地址重定向到 React 路由。

## 数据流

```text
用户访问 /app/public-share?token=xxx
  -> React 读取 URL token
  -> GET /api/public/{token}
  -> 后端优先读取公开简历缓存
  -> 后端校验 token 和公开状态
  -> 返回过滤后的公开简历 DTO
  -> React 渲染公开简历
```

## API

```text
GET /api/public/{shareToken}
```

成功响应只包含公开字段：

```json
{
  "firstName": "Alice",
  "lastName": "Wang",
  "designation": "Backend Engineer",
  "summary": "Experienced engineer...",
  "jobs": [],
  "educations": [],
  "skills": ["Java", "Spring Boot", "React"],
  "theme": 1
}
```

不会返回：

```text
email
phone
userName
shareToken
isPublic
```

## 错误处理

- 缺少 token：前端显示 `Missing share token.`
- token 不存在或简历未公开：后端返回 `404`，前端显示错误提示。
- 网络错误：前端显示请求失败信息。

## 当前实现位置

- React 页面：`frontend/src/App.jsx`
- API：`src/main/java/com/daemonsets/resumeportal/ApiController.java`
- 缓存服务：`src/main/java/com/daemonsets/resumeportal/PublicResumeCacheService.java`
- 旧地址兼容跳转：`src/main/java/com/daemonsets/resumeportal/HomeController.java`

## 缓存策略

- 默认使用本地 Caffeine 缓存，缓存 key 为公开分享 `shareToken`。
- 部署环境可以通过 `PUBLIC_RESUME_CACHE_BACKEND=redis` 切换到 Redis。
- 缓存内容是过滤后的公开 DTO，不缓存 JPA 实体，也不包含 `email`、`phone`、`userName`、`shareToken`、`isPublic`。
- 用户更新简历、生成分享链接、撤销分享链接后，会在事务提交后清理对应 `shareToken` 缓存。
- Redis 读取、写入、删除失败时会降级到本地缓存和数据库，不影响公开页面可用性。

## 后续可做

- 给公开页增加打印按钮。
- 给公开页增加更接近正式简历模板的只读展示样式。
- 给 `/api/public/{shareToken}` 增加缓存命中率、缓存 miss 次数等 Micrometer 指标。
- 增加公开访问统计，用于后续简历浏览数据分析。
