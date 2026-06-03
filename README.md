# Resume Generator

这是一个简历生成系统，当前后端为 Spring Boot，现阶段保留少量静态页面用于过渡验证；后续前端会迁移为 React，后端主要提供 REST API。

## 当前能力

- 用户注册、登录、退出
- 当前用户简历资料读取和更新
- PDF 导出
- 公开分享链接生成、撤销和公开访问
- 三套 Thymeleaf 简历预览模板
- React 开发环境跨域支持

## 技术栈

- Java 11
- Spring Boot 2.3.1
- Spring Security
- Spring Data JPA
- MySQL 8
- Maven

## 本地启动

启动 MySQL：

```bash
docker run --name mysql-standalone -p 6603:3306 -e MYSQL_ROOT_PASSWORD=password -e MYSQL_DATABASE=resume-portal -d mysql
```

启动后端：

```bash
mvn spring-boot:run
```

默认后端地址：

```text
http://localhost:5000
```

## 配置项

以下配置都有本地默认值，部署时建议通过环境变量覆盖：

```text
DB_URL
DB_USERNAME
DB_PASSWORD
SQL_INIT_MODE
CORS_ALLOWED_ORIGINS
DB_POOL_MAX_SIZE
DB_POOL_MIN_IDLE
```

React 本地开发时，默认允许：

```text
http://localhost:3000
http://localhost:5173
```

## 主要 API

```text
POST /api/auth/register
POST /api/auth/login
GET  /api/auth/me
POST /api/auth/logout

GET  /api/profile
PUT  /api/profile
GET  /api/profile/export/pdf

GET  /api/profile/share
POST /api/profile/share/generate
POST /api/profile/share/revoke
GET  /api/public/{shareToken}
```

## 后续方向

详细规划见 `docs/FEATURE_ROADMAP.md`。当前优先级是先完成前后端分离基础、安全收口、测试补齐，再推进高并发基础设施。
