# tools/ — 本机工具与连接经验（臻心减）

> 目的：沉淀本机环境的连接方式、踩坑记录与可直接复用的脚本，下次同类任务开箱即用。
> 规约：真实凭据一律放 `*.local.*` 文件（已 gitignore），提交件只留占位符。

| 目录 | 内容 | 快速入口 |
| ---- | ---- | -------- |
| [env/](./env/) | JDK25 切换（系统默认 JAVA_HOME=JDK8，本项目必须 25） | `tools\env\mvn25.cmd <mvn 参数>` |
| [db/](./db/) | MySQL 连接与 SQL 执行器（JDBC 单文件，无需客户端） | `tools\db\run-sql.cmd <sql文件> [--allow-error]` |
| [redis/](./redis/) | Redis 连接经验 | `D:\App\Redis\redis-cli.exe -a <密码>` |
| [http/](./http/) | HTTP 接口测试（api-test.ps1 + BodyFile，curl/内联引号全踩坑） | `powershell -File tools\http\api-test.ps1 -Method Post -Path /auth/guest -BodyFile tools\http\bodies\empty.json` |
| [browser/](./browser/) | 浏览器/小程序自动化测试方案（首次任务补充实证） | — |

## 本机环境速查

| 项 | 值 |
| ---- | ---- |
| JDK 布局 | `D:\App\java\{8,17,21,25,jre8}`；系统 JAVA_HOME=`D:\App\java\8`（另一项目占用） |
| Maven 本地仓库 | `D:\App\apache-maven-3.9.16\repository` |
| MySQL 服务 | 127.0.0.1:3306，库 `zhenxinjian`；客户端 `D:\App\MySQL\MySQLServer8\bin\mysql.exe` |
| MySQL 驱动 jar | `<maven仓库>\com\mysql\mysql-connector-j\9.7.0\mysql-connector-j-9.7.0.jar` |
| Redis | 127.0.0.1:6379 db0；客户端 `D:\App\Redis\redis-cli.exe` |
| 凭据真源 | `apps/zhenxinjian-backend/src/main/resources/application-dev.yml`（gitignored） |

## 本 shell 环境踩坑清单（必读）

1. **带引号的路径参数会被吞**：`jar tf "D:\..."`、`java -cp "..."` 报 FileNotFound/CNFE——路径无空格时**去掉引号**直接传
2. `findstr /C:"多个词"` 会被错误分词——改用 Grep 工具读日志
3. `powershell -Command "..."` 输出不回显——输出重定向到文件再 Read
4. CMD 里 `;` 不是命令分隔符，会原样传给程序；串行命令用 `&&`
5. `set VAR=value && cmd` 的 value 会带上 `&&` 前的空格——写成 `set VAR=value&& cmd` 或用 `tools\env\mvn25.cmd`
6. 长命令输出到 `%TEMP%\xxx.log` 再用 Read/Grep 工具查看，避免管道 findstr
7. **`.cmd` 文件禁止中文**：cmd.exe 按 GBK 代码页解析批处理，UTF-8 中文注释的字节流可能被拆成可执行片段（报 `'xx' is not recognized`）——批处理一律 ASCII 注释，中文写进同名 README.md
8. 相对路径跨目录调用 `.cmd` 时注意基准目录（`apps\zhenxinjian-backend\tools` 不存在）——仓库根工具一律用绝对路径 `E:\project\zhenxinjian\tools\...`
9. **JSON 接口测试禁止 curl/内联 JSON**：curl `-H` 引号被吞回退 form 编码（415/500）；`powershell -Command` 的 `$变量` 被吞；`-Body "{}"`/`'{}'` 引号原样传入致 Jackson 解析失败——一律 `tools\http\api-test.ps1 -BodyFile`（详见 [http/README.md](./http/README.md)）
