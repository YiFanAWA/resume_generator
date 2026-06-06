# 工程化基线说明

本文记录当前项目新增的三项工程化基础：Flyway 数据库迁移、Docker Compose 一键运行、CI 自动验证。

## 1. Flyway 数据库迁移

目的：让数据库结构变更可追踪、可重复、可验证，避免继续依赖 `ddl-auto=update` 自动改库。

当前配置：

- 迁移脚本目录：`src/main/resources/db/migration`
- 初始化脚本：`V1__init_resume_portal_schema.sql`
- 分享安全字段脚本：`V2__add_share_security_and_view_stats.sql`
- dev/prod 默认使用 `spring.jpa.hibernate.ddl-auto=validate`
- test 环境关闭 Flyway，继续使用 H2 `create-drop`

启动顺序：

```text
Spring Boot 启动
  -> Flyway 检查 flyway_schema_history
  -> 执行未运行过的 V*.sql
  -> Hibernate validate 校验实体和表结构是否一致
  -> 应用正常启动
```

如果是全新数据库，Flyway 会依次执行 V1、V2。

如果是已有本地旧数据库，可以临时设置：

```text
FLYWAY_BASELINE_ON_MIGRATE=true
```

这会让 Flyway 接管已有库。接管完成后，建议再改回 `false`，避免生产环境误跳过迁移。

## 2. Docker Compose 一键运行

目的：把 MySQL、Redis、后端应用的启动方式固定下来，减少“端口不对、数据库没起、环境变量不一致”的问题。

本地直接运行 Spring Boot 时，dev 默认连接 `localhost:6603`，并默认关闭 Redis health check。使用 Docker Compose 时会启动 Redis，并通过 `REDIS_HEALTH_ENABLED=true` 开启 Redis 健康检查。

Compose MySQL 会设置 `MYSQL_ROOT_HOST=%`，允许宿主机上的 Spring Boot 通过映射端口 `localhost:6603` 连接容器内 MySQL。这个配置只适合本地开发；生产环境应该使用独立数据库账号而不是 root。

相关文件：

- `Dockerfile`
- `docker-compose.yml`
- `.dockerignore`
- `.env.example`

首次运行：

```bash
copy .env.example .env
docker compose up -d --build
```

访问地址：

```text
http://localhost:5000/app/
```

查看日志：

```bash
docker compose logs -f app
```

停止服务：

```bash
docker compose down
```

如果需要删除本地 MySQL/Redis 数据卷，使用：

```bash
docker compose down -v
```

## 3. CI 自动验证

目的：每次提交代码时自动检查项目是否还能测试、构建、打包。

相关文件：

```text
.github/workflows/ci.yml
```

当前流水线会执行：

```text
npm ci
npm run build
./mvnw -B test
./mvnw -B -DskipTests package
```

这一步不是自动部署，只是质量门禁。等项目部署方案稳定后，再把 Docker 镜像构建和自动发布接进来。

## 后续建议

1. 增加 Testcontainers，用真实 MySQL/Redis 跑集成测试。
2. 给 Docker 镜像增加版本号和镜像仓库推送。
3. 增加生产部署脚本，例如 Nginx + HTTPS + app 多实例。
4. 把依赖漏洞扫描接入 CI。
