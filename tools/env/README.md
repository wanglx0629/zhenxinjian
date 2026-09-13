# env/ — JDK25 切换经验

## 背景

- 本项目（zhenxinjian-backend）用 **JDK 25**（`maven-compiler-plugin` release 25）
- 本机 JDK25 真身：`D:\App\Java\jdk-25.0.4.1`（版本化目录，小版本升级目录名会变——届时同步脚本兜底值或改设 `ZXJ_JDK25_HOME`）；PATH 上 `java` 经 Oracle javapath shim 解析到该 JDK
- Maven 只认 `JAVA_HOME`、不看 `java -version`——故须用下方三法之一显式指定 JDK25

## 三种用法（按场景选）

| 场景 | 用法 |
| ---- | ---- |
| 只跑一条 Maven 命令 | `tools\env\mvn25.cmd test -Dtest=XxxTest`（推荐，零副作用） |
| 连续跑多条命令的会话 | 先 `tools\env\jdk25.cmd`，之后会话内 java/mvn 都是 25 |
| 手动内联（AI/脚本里） | `set JAVA_HOME=D:\App\Java\jdk-25.0.4.1&& mvn test` |

## 路径解析（B-T35 环境解耦）

两脚本统一「环境变量优先 → 本机默认值兜底 → 缺失报错提示」三级解析：

- `ZXJ_JDK25_HOME`：JDK25 根目录。未设置时兜底 `D:\App\Java\jdk-25.0.4.1`（本机真身）；换机/新成员设此变量即可，无需改脚本
- 兜底值与设定值均校验 `<root>\bin\java.exe` 存在，缺失即报错并提示设变量（不再静默带出后续 Maven 怪异报错）
- `jdk25.cmd` 无 setlocal（故意影响当前会话），会把兜底值写回 `ZXJ_JDK25_HOME`——同会话后续 `mvn25.cmd` 自动继承

## 坑

1. **`set` 尾部空格陷阱**：`set JAVA_HOME=D:\App\Java\jdk-25.0.4.1 && mvn ...`（`&&` 前有空格）会把空格写进变量，Maven 报
   「The JAVA_HOME environment variable is not defined correctly」。正确：`set JAVA_HOME=D:\App\Java\jdk-25.0.4.1&& mvn ...`
2. `java -version` 显示 25 不代表 `mvn` 用 25——Maven 只认 `JAVA_HOME`
3. 验证生效：`mvn -version` 输出的 `Java version` 应为 25.x

## Maven 本地仓库

- 路径：`D:\App\apache-maven-3.9.15\repository`（非默认 `~/.m2`）
- 查询命令：`mvn help:evaluate -Dexpression=settings.localRepository -q -DforceStdout`
