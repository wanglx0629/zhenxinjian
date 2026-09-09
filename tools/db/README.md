# db/ — MySQL 连接与 SQL 执行经验

## 连接信息

- 服务：`127.0.0.1:3306`，库 `zhenxinjian`（utf8mb4）
- 凭据真源：`apps/zhenxinjian-backend/src/main/resources/application-dev.yml`（gitignored）
- 本工具凭据：同目录 `db.local.properties`（gitignored；缺失时复制 `db.local.properties.example` 填真实值）

## 三种执行方式

| 方式 | 命令 | 适用 |
| ---- | ---- | ---- |
| 本工具（推荐） | `tools\db\run-sql.cmd sql\change3_user_body.sql` | 迁移脚本/验证查询，输出整洁，无需客户端 |
| 原生客户端 | `D:\App\MySQL\MySQLServer8\bin\mysql.exe -h127.0.0.1 -uroot -p zhenxinjian` | 交互式探查 |
| 容错执行 | `tools\db\run-sql.cmd xxx.sql --allow-error` | 验证约束（预期某条报错，如唯一键冲突） |

## SqlRunner 特性

- 按 `;` 切分语句、剥离 `--` 注释；DDL/DQL 通吃；SELECT 结果以 `|` 分隔打印
- 中文乱码防护：脚本已带 `-Dfile.encoding=UTF-8`；SQL 文件须 UTF-8 保存
- 凭据解析：`db.local.properties` → 环境变量 `MYSQL_URL/MYSQL_USERNAME/MYSQL_PASSWORD`

## 坑（实证）

1. **带引号的路径参数在本 shell 会被吞**：`java -cp "D:\...jar;C:\..."` 报 ClassNotFoundException——路径无空格就去引号（run-sql.cmd 内已处理）
2. `java -cp x.jar SqlRunner.java`（源码单文件模式）**类加载器不加载 cp 里的驱动**——必须先 `javac` 编译再 `java -cp` 运行（run-sql.cmd 已自动化）
3. 驱动 jar 在 Maven 本地仓库：`D:\App\apache-maven-3.9.16\repository\com\mysql\mysql-connector-j\9.7.0\`
4. 长输出重定向到 `%TEMP%\xx.log` 再用编辑器/Grep 查看，别用 `findstr /C:`（会被错误分词）
