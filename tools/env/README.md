# env/ — JDK25 切换经验

## 背景

- 本项目（zhenxinjian-backend）用 **JDK 25**（`maven-compiler-plugin` release 25）
- 系统 `JAVA_HOME=D:\App\java\8`（另一项目占用），直接 `mvn` 会报 **「无效的目标发行版: 25」**
- 本机 JDK 布局：`D:\App\java\{8, 17, 21, 25, jre8}`

## 三种用法（按场景选）

| 场景 | 用法 |
| ---- | ---- |
| 只跑一条 Maven 命令 | `tools\env\mvn25.cmd test -Dtest=XxxTest`（推荐，零副作用） |
| 连续跑多条命令的会话 | 先 `tools\env\jdk25.cmd`，之后会话内 java/mvn 都是 25 |
| 手动内联（AI/脚本里） | `set JAVA_HOME=D:\App\java\25&& mvn test` |

## 坑

1. **`set` 尾部空格陷阱**：`set JAVA_HOME=D:\App\java\25 && mvn ...`（`&&` 前有空格）会把空格写进变量，Maven 报
   「The JAVA_HOME environment variable is not defined correctly」。正确：`set JAVA_HOME=D:\App\java\25&& mvn ...`
2. `java -version` 显示 25 不代表 `mvn` 用 25——Maven 只认 `JAVA_HOME`
3. 验证生效：`mvn -version` 输出的 `Java version` 应为 25.x

## Maven 本地仓库

- 路径：`D:\App\apache-maven-3.9.16\repository`（非默认 `~/.m2`）
- 查询命令：`mvn help:evaluate -Dexpression=settings.localRepository -q -DforceStdout`
