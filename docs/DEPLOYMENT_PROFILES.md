# 环境配置说明

项目现在使用 Spring Profile 区分本地开发、测试和生产配置。

## 配置文件

| 文件 | 用途 |
| --- | --- |
| `application.properties` | 公共配置：端口、缓存、日志、Actuator、Session、通用连接池参数 |
| `application-dev.properties` | 本地开发默认配置：本地 MySQL、localhost CORS、`ddl-auto=update` |
| `application-prod.properties` | 生产配置：数据库和 CORS 必须来自环境变量、`ddl-auto=validate`、Secure Cookie |
| `application-test.properties` | 测试配置：H2、关闭真实缓存、测试日志输出到 `target/test-logs` |

默认启动 profile 是 `dev`。生产启动时必须设置：

```powershell
$env:SPRING_PROFILES_ACTIVE="prod"
```

## 本地开发

本地默认连接：

```text
jdbc:mysql://localhost:6603/resume-portal
```

本地启动时通常只需要：

```powershell
mvn spring-boot:run
```

如果要覆盖本地数据库：

```powershell
$env:DB_URL="jdbc:mysql://localhost:6603/resume-portal?serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="password"
mvn spring-boot:run
```

## 生产必填环境变量

生产 profile 不再给数据库和 CORS 提供本地默认值，避免误用开发配置。

```text
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:mysql://your-db-host:3306/resume-portal?serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
DB_USERNAME=resume_app
DB_PASSWORD=replace-with-strong-password
CORS_ALLOWED_ORIGINS=https://your-domain.com
```

建议生产环境同时设置：

```text
JPA_DDL_AUTO=validate
SESSION_COOKIE_SECURE=true
LOG_FILE=logs/resume-generator-prod.log
LOG_FILE_MAX_SIZE=10MB
LOG_FILE_MAX_HISTORY=14
LOG_FILE_TOTAL_SIZE_CAP=1GB
ACCESS_LOG_ENABLED=true
PUBLIC_RESUME_CACHE_BACKEND=redis
REDIS_HOST=your-redis-host
REDIS_PORT=6379
```

如果应用直接处理 HTTPS，或已经正确配置反向代理转发头，可以再启用：

```text
APP_SECURITY_REQUIRE_HTTPS=true
```

如果 HTTPS 在 Nginx 等代理层终止，但 Spring Boot 没有配置转发头，先不要打开这个开关，避免 HTTP/HTTPS 重定向循环。

## 日志

当前最小日志方案：

- 应用日志写入 `LOG_FILE`，本地默认是 `logs/resume-generator-dev.log`。
- `logback-spring.xml` 已配置按日期和大小滚动日志。
- 生产 profile 默认开启 Tomcat access log。
- `logs/` 已加入 `.gitignore`，避免把运行日志提交进仓库。

后续如果要正式上线，可以继续把错误日志、业务审计日志和慢请求日志拆成独立 appender。

## 安全默认值

- Session Cookie 默认 `HttpOnly=true`。
- 生产 profile 默认 `SESSION_COOKIE_SECURE=true`。
- API 错误响应默认不暴露堆栈。
- 生产 CORS 必须显式配置真实域名。
- Spring Security 已显式启用基础安全响应头和同源 iframe 策略。
