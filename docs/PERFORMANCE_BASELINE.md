# 性能压测基线

本文件记录当前阶段的压测方法。目标不是追求极限 QPS，而是为后续 Redis 缓存、PDF 异步导出、限流和监控提供可对比的基线。

## 推荐工具

使用 k6：

```bash
k6 version
```

如果本机没有安装 k6，可以先只保留脚本，等部署环境或测试机准备好后执行。

## 前置准备

1. 启动 MySQL。
2. 启动 Spring Boot 后端。
3. 注册或准备一个测试用户。
4. 登录后生成公开分享链接，拿到 `shareToken`。
5. 准备测试账号和密码。

## 运行命令

```bash
k6 run perf/k6/resume-baseline.js
```

自定义地址和测试数据：

```bash
k6 run ^
  -e BASE_URL=http://localhost:5000 ^
  -e USERNAME=alice ^
  -e PASSWORD=password123 ^
  -e SHARE_TOKEN=your-share-token ^
  perf/k6/resume-baseline.js
```

PowerShell 单行示例：

```powershell
k6 run -e BASE_URL=http://localhost:5000 -e USERNAME=alice -e PASSWORD=password123 -e SHARE_TOKEN=your-share-token perf/k6/resume-baseline.js
```

## 当前关注接口

```text
POST /api/auth/login
GET  /api/auth/me
GET  /api/profile
PUT  /api/profile
GET  /api/profile/export/pdf
GET  /api/public/{shareToken}
```

## 场景拆分

`perf/k6/resume-baseline.js` 默认包含两个场景：

- `read_baseline`：默认最高 5 VUs，覆盖登录、读取当前用户资料、公开分享页、抽样 PDF 导出。
- `write_baseline`：默认 1 VU，覆盖 `PUT /api/profile` 保存资料。

这样做是为了避免多个 VU 同时写同一个测试账号，导致结果主要反映“单用户并发保存冲突”，而不是系统整体读性能。

可选参数：

```text
READ_MAX_VUS=5
WRITE_VUS=1
INCLUDE_WRITES=true
INCLUDE_PDF=true
PDF_SAMPLE_RATE=0.2
```

只跑读场景：

```powershell
k6 run -e INCLUDE_WRITES=false -e BASE_URL=http://localhost:5000 -e USERNAME=alice -e PASSWORD=password123 -e SHARE_TOKEN=your-share-token perf/k6/resume-baseline.js
```

单独观察写入冲突时，可以提高 `WRITE_VUS`，但这不代表真实用户流量，更多是用来验证同一用户并发保存的稳定性。

## 建议记录指标

| 指标 | 含义 | 目标 |
| --- | --- | --- |
| http_req_failed | 请求失败率 | < 1% |
| http_req_duration p95 | 95% 请求耗时 | < 500ms，PDF 除外 |
| public resume p95 | 公开分享页接口耗时 | 后续缓存优化重点 |
| pdf export p95 | PDF 导出耗时 | 判断是否需要异步化 |
| checks | 断言通过率 | 100% |

## 基线记录模板

## 当前基线记录

测试命令：

```powershell
.\.tools\k6\k6.exe run -e BASE_URL=http://localhost:5000 -e USERNAME=alice -e PASSWORD=password123 -e SHARE_TOKEN=e9262316-428d-44cb-8bd0-84a7c334583f perf/k6/resume-baseline.js
```

测试结果：`checks=100%`，`http_req_failed=0.00%`，`http_req_duration p95=97.27ms`。

| 日期 | 场景 | VUs | 持续时间 | P95 | 失败率 | 备注 |
| --- | --- | --- | --- | --- | --- | --- |
| 2026-06-05 | public resume | read 5 + write 1 | 1m20s | 25.30ms | 0.00% | 当前公开分享接口读基线 |
| 2026-06-05 | profile api | read 5 + write 1 | 1m20s | 32.68ms | 0.00% | 当前登录后资料读取基线 |
| 2026-06-05 | save profile | read 5 + write 1 | 1m20s | 135.92ms | 0.00% | 单写 VU，避免同账号写冲突污染读基线 |
| 2026-06-05 | pdf export | read 5 + write 1 | 1m20s | 43.76ms | 0.00% | PDF 抽样比例 20% |

| 日期 | 分支/提交 | 场景 | VUs | 持续时间 | P95 | P99 | 失败率 | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
|  |  | public resume |  |  |  |  |  |  |
|  |  | profile api |  |  |  |  |  |  |
|  |  | pdf export |  |  |  |  |  |  |

## 下一步判定规则

- 公开分享接口已加入本地缓存和可选 Redis 后端。启用 Redis 后，需要重新记录 `/api/public/{shareToken}` 的 P95/P99、失败率和缓存命中率。
- 如果 PDF 导出 P95 超过 2 秒，且并发下错误率升高，优先做异步导出。
- 如果登录和 profile API 压力下连接池等待增加，再调整 HikariCP 参数。
- 如果公开接口存在异常高频访问，再引入限流。
