# redis/ — Redis 启动与连接经验

## 启动（本机绿色版，无 Windows 服务）

- 安装目录：`D:\App\Redis`（控制台进程，重启电脑后需手动拉起）
- 一键启动：`tools\redis\start-redis.cmd`（已监听则直接跳过；在新窗口起 `redis-server.exe redis.windows-service.conf`，最多等 15s 探活）
- 也可经 `tools\start-infra.cmd` 与 MySQL 一起拉起
- 配置文件：`D:\App\Redis\redis.windows-service.conf`（`requirepass` 为密码真源之一）
- 关闭窗口即停服；如需停服：`D:\App\Redis\redis-cli.exe -a <密码> shutdown nosave`

## 连接信息

- 服务：`127.0.0.1:6379`，db `0`
- 密码：同目录 `redis.local.txt`（gitignored，仅一行密码；与 `requirepass`、`application-dev.yml` 一致）
- 客户端：`D:\App\Redis\redis-cli.exe`

## 常用命令

```cmd
rem 交互式（-a 后会告警密码明文，本机可接受）
D:\App\Redis\redis-cli.exe -a <密码>

rem 单条命令（本项目 Key 前缀速查）
D:\App\Redis\redis-cli.exe -a <密码> KEYS zhenxinjian:token:*
D:\App\Redis\redis-cli.exe -a <密码> KEYS zhenxinjian:captcha:*
D:\App\Redis\redis-cli.exe -a <密码> TTL zhenxinjian:token:<userId>
```

## 本项目 Key 分布（application.yml）

| 前缀 | 用途 |
| ---- | ---- |
| `zhenxinjian:token:` | 登录 token（JWT 过滤器 Redis 比对） |
| `zhenxinjian:captcha:` | 图形验证码 |
| `zhenxinjian:login:fail:` | 登录失败计数（锁定用） |
| `zhenxinjian:cache:` | JetCache 远程缓存（java 序列化，值不可直接读） |

## 坑与规约

1. 密码含 `@` 等特殊字符：JetCache URI 里须 URL 编码（`@` → `%40`），redis-cli `-a` 直接给原值
2. 会话/验证码 Key 均有 TTL，勿手动 `DEL` 正在使用的 token（会把用户踢下线）
3. `FLUSHDB` 仅限本机调试，执行前确认 db 编号为 0
