# HTTP 接口测试工具（tools/http）

Windows 本机对后端 `http://127.0.0.1:8080/api` 做接口级验证的标准方式。
**不要直接用 curl 或 PowerShell 内联 JSON**——下面记录了踩过的坑，直接用 `api-test.ps1`。

## 标准用法

```cmd
powershell -ExecutionPolicy Bypass -File E:\project\zhenxinjian\tools\http\api-test.ps1 -Method Post -Path /auth/guest -BodyFile E:\project\zhenxinjian\tools\http\bodies\empty.json
```

参数：

- `-Method`：Get / Post / Put / Delete（默认 Get）
- `-Path`：接口路径，自动拼 `http://127.0.0.1:8080/api` 前缀（可用 `-BaseUrl` 覆盖）
- `-BodyFile`：JSON 请求体文件路径（**只用文件，不要内联**）
- `-Token`：JWT token，自动拼 `Bearer ` 前缀

请求体文件放 `bodies/` 目录（用编辑器/Write 工具创建，内容精确无引号问题）。
常用：`empty.json`（游客登录）、`body-golden.json`（身体数据金标：女30/162/55/目标54/档2/缺口200 → 1252/1721/1521/190.1/114.1/33.8）。

返回：成功/失败都输出 Result JSON（gbk 终端中文显示为乱码属正常，看 code 与数值即可）。

## 踩坑记录（2026-09-09 实测）

### 坑 1：curl 的 -H 引号被吞，Content-Type 丢失

```cmd
curl -s -X POST http://127.0.0.1:8080/api/auth/guest -H "Content-Type: application/json" -d "{}"
```

现象：后端报 `HttpMediaTypeNotSupportedException: Content-Type 'application/x-www-form-urlencoded;charset=UTF-8' is not supported`，返回 500 系统繁忙。
原因：本环境 shell 把 `"Content-Type: application/json"` 的引号吞掉/拆开，`-H` 没生效，curl 回退到默认 form 编码。
结论：**本机 curl 不可用于 JSON POST**，放弃。

### 坑 2：PowerShell -Command 内联脚本，$ 变量被外层 shell 吞

```cmd
powershell -Command "$r = Invoke-RestMethod ...; $r | ConvertTo-Json"
```

现象：输出只剩 `= Invoke-RestMethod ...`，`$r` 被外层 shell 当变量展开成空。
结论：**不要 -Command 内联**，脚本写 `.ps1` 文件用 `-File` 执行。

### 坑 3：-Body 内联 JSON，单双引号都原样传进 PowerShell

```cmd
powershell -File api-test.ps1 -Body "{}"   → 后端: no String-argument constructor ... from String value ('{}')
powershell -File api-test.ps1 -Body '{}'   → 后端: JSON parse error: Unexpected character (''')
```

现象：本环境 shell 把引号字符原样保留进参数，Body 变成字符串 `"{}"` 或 `'{}'`，Jackson 解析失败。
结论：**请求体一律走文件**（`-BodyFile`），命令行零引号，彻底规避。

### 坑 4：PowerShell 输出被吞 / 中文乱码

- `powershell -Command "Get-Content ..."` 有时只回显命令无输出；读日志用 Read 工具直接读文件更稳。
- 终端 GBK 导致响应中文乱码（如 `æ“作æˆåŠŸ` = 操作成功），属显示问题，不影响判断；看 `code` 字段即可。

## 后端联调配套

- 后端日志：`tools/backend-run.log`（dev 开 SQL 日志，归档/覆盖流程可直接看 INSERT/UPDATE 顺序）
- 查库验证：`tools\db\run-sql.cmd <sql文件>`（见 tools/db/README.md）
- 重启后端：`taskkill /F /PID <pid>`（`netstat -ano | findstr :8080` 查 pid）后用 `tools\env\mvn25.cmd spring-boot:run` 后台启动
