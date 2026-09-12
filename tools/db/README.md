# db/ — MySQL 连接与 SQL 执行经验

## 连接信息

- 服务：`127.0.0.1:3306`，库 `zhenxinjian`（utf8mb4）
- 凭据真源：`apps/zhenxinjian-backend/src/main/resources/application-dev.yml`（gitignored）
- 本工具凭据：同目录 `db.local.properties`（gitignored；缺失时复制 `db.local.properties.example` 填真实值）

## 三种执行方式

| 方式 | 命令 | 适用 |
| ---- | ---- | ---- |
| 本工具（推荐） | `tools\db\run-sql.cmd sql\change3_user_body.sql` | 迁移脚本/验证查询，输出整洁，无需客户端 |
| 原生客户端 | `D:\App\Mysql\MySQL Server 8.4\bin\mysql.exe -h127.0.0.1 -uroot -p zhenxinjian` | 交互式探查 |
| 容错执行 | ~~`--allow-error`~~（已移除） | 失败即终止（exit 2）；需容忍报错的验证请改用幂等守卫或信息查询 |

## SqlRunner 特性

- 按 `;` 切分语句、剥离 `--` 注释；DDL/DQL 通吃；SELECT 结果以 `|` 分隔打印
- 中文乱码防护：脚本已带 `-Dfile.encoding=UTF-8`；SQL 文件须 UTF-8 保存
- 凭据解析：`db.local.properties` → 环境变量 `MYSQL_URL/MYSQL_USERNAME/MYSQL_PASSWORD`
- **版本记账**：`change<N>_*.sql` 自动纳入 `schema_migrations` 版本表——执行前查账，已应用整体跳过（SKIP）；全部语句成功后回写记账（RECORDED）；任何报错立即终止且**不记账**
- **幂等守卫**：迁移脚本自带 `IF NOT EXISTS` / information_schema 条件守卫，手工重复执行安全（change2 为逐步前置状态守卫，支持断点续跑）
- 每次运行都重新编译 SqlRunner.java，改动即时生效

## 迁移执行顺序

1. 全新环境（实证 2026-09-12）：
   - 一次性建库：原生客户端 `CREATE DATABASE zhenxinjian DEFAULT CHARACTER SET utf8mb4`（或不带库名的临时 URL 跑建库脚本）。**注意**：`db.local.properties` 的 `MYSQL_URL` 必须带默认库 `/zhenxinjian`——SqlRunner 对 change 脚本会先建/查版本表，无默认库报 `No database selected`
   - 基线：`tools\db\run-sql.cmd apps\zhenxinjian-backend\src\main\resources\data.sql`（users 主表 + 初始管理员；change1~9 的守卫假设 users 已存在）
   - 依次执行 `change0_schema_migrations.sql`（可选，runner 会自建）→ `change1` ~ `change9`，重复执行自动跳过
2. 存量环境（历史上手工跑过 change1~9、版本表无账）：先执行 `sql/change_backfill_migrations.sql` 回填账目，再正常执行后续 change

## 坑（实证）

1. **带引号的路径参数在本 shell 会被吞**：`java -cp "D:\...jar;C:\..."` 报 ClassNotFoundException——路径无空格就去引号（run-sql.cmd 内已处理）
2. `java -cp x.jar SqlRunner.java`（源码单文件模式）**类加载器不加载 cp 里的驱动**——必须先 `javac` 编译再 `java -cp` 运行（run-sql.cmd 已自动化）
3. 驱动 jar 在 Maven 本地仓库：`D:\App\apache-maven-3.9.15\repository\com\mysql\mysql-connector-j\9.7.0\`；java/javac 走 PATH（JDK 25），不硬编码 JDK 目录
4. 长输出重定向到 `%TEMP%\xx.log` 再用编辑器/Grep 查看，别用 `findstr /C:`（会被错误分词）
