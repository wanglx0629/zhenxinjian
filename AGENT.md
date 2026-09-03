# AGENT.md

编码规范和要求

---

## 代码规范

### 通用原则

1. **命名规范**
   - 文件名：`kebab-case` (HTML/CSS), `PascalCase` (组件)
   - 变量/函数：`camelCase`
   - 常量：`UPPER_SNAKE_CASE`
   - 类/接口：`PascalCase`

2. **代码质量**
   - 函数不超过 50 行
   - 文件不超过 800 行
   - 嵌套层级不超过 4 层
   - 避免魔法数字，使用常量或配置

3. **错误处理**
   - 不忽略异常（不使用空的 catch 块）
   - 提供清晰的错误信息
   - 记录详细的错误上下文（包含异常堆栈）
   - 使用 `log.error("错误描述", exception)` 而非 `e.printStackTrace()`

### Vue (emp-shr-mobile / emp-shr-web)

- 使用组件化开发，组件职责单一
- Props 使用 kebab-case，内部使用 camelCase
- 使用 `$emit` 触发自定义事件
- 避免在模板中写复杂逻辑，使用计算属性或方法
- 状态管理使用统一的 store 模式

### Java (emp-shr-backend)

- 使用 Spring Boot 最佳实践
- Service 层处理业务逻辑，Controller 层只做参数验证和响应封装
- 使用 `@Transactional` 管理事务，涉及数据修改的方法必须添加
- 记录关键操作的日志（数据变更、重要操作）
- SQL 使用参数化查询，避免 SQL 注入风险
- 验证所有用户输入，防止 XSS 攻击

---

## 后端分层架构与风格 (emp-shr-backend)

> 这一节在通用 Java 规范基础上，固化 ygtz 后端的分层职责与所选体系。
> 老模块（`com`、部分 `glrba`）使用 `R` + `Map<String,Object>` + `PageUtils` 旧体系，
> **新代码一律采用下述新 MVC 体系**，不要混用。

### Mapper 层

**职责定位**：完成数据访问；复杂查询的过滤、聚合、排序应在 SQL 中完成，不下放到 Service。

- **默认走 BaseMapper + `QueryWrapper`**：单表 CRUD、按已知 ID 集合的简单查询，不写自定义 SQL
- **以下场景必须在 Mapper 写 `@Select` 注解 SQL**，不要在 Service 拆成多步查询拼接：
  - Oracle 层级查询（`connect by` / `sys_connect_by_path`），如部门树展开
  - 需要把"先查 A 集合再以 A 过滤 B"压缩成一次往返的查询（用子查询 / `EXISTS` / `JOIN`）
  - 涉及大量 ID 的 `IN` 过滤（避免应用层把上百个 ID 序列化回来再传回 DB；改写为子查询）
  - 跨表分页 / 聚合
- **SQL 编写规范**：
  - 一律使用 `#{}` 参数占位符，禁止 `${}` 拼接用户输入
  - 入参用 `@Param("xxx")` 显式命名，方法签名超过 1 个参数时必须加
  - 列别名与 Java 字段映射不一致时，使用 `@Results`/`@Result` 显式声明
  - 字段名小写，关键字大写，多行 SQL 用字符串拼接保持可读
- **示例参考**：`EhrDeptMapper`、`EhrEmployeeMapper`

```java
@Select("select emp_no from CMPS_CM_TRD_EMPLOYEE " +
        "where dep_id in ( " +
        "    select DEP_ID from CMPS_CM_TRD_DEPARTMENT " +
        "    start with DEP_ID = #{deptId} " +
        "    connect by nocycle prior DEP_ID = PARENT_UNIT_ID)")
List<String> listEmpNosUnderDept(@Param("deptId") String deptId);
```

### Service 层

**职责定位**：薄代理 + 入参校验 + 业务编排；不重复 SQL 已完成的工作。

- **薄代理优先**：能在 Mapper 一条 SQL 完成的，Service 只做校验后转发
- **入参校验**：null / blank / 集合空，统一抛 `IllegalArgumentException`，错误消息使用业务语义（"部门 ID 不能为空"），不抛 NPE
- **轻量包装允许**：`trim()`、`new HashSet<>(list)` 去重、`Collections.emptyXxx()` 兜底
- **禁止反模式**：
  - 在 Service 把"先查 A 再查 B"拆成两步 Mapper 调用 —— 这是 Mapper 没写好的信号，回去改 SQL
  - 在循环内调用 Mapper（N+1）
  - 在 Service 拼接 SQL 字符串
- **事务注解**：写操作必须 `@Transactional`，只读查询服务可省略；事务边界放在 Service 层，不放 Controller / Mapper
- **接口与实现分离**：`xxxService` 接口 + `xxxServiceImpl` 实现，构造器注入依赖（不要 `@Autowired` 字段注入）
- **示例参考**：`EhrDeptQueryServiceImpl`、`EhrEmployeeQueryServiceImpl`

```java
@Override
public Set<String> listEmpNosUnderDept(String deptId) {
    if (deptId == null || deptId.trim().isEmpty()) {
        throw new IllegalArgumentException("部门 ID 不能为空");
    }
    return new HashSet<>(baseMapper.listEmpNosUnderDept(deptId));
}
```

### Controller 层

**职责定位**：HTTP 协议适配；不写业务逻辑。

- **新模块统一使用新 MVC 体系**（`com.gjzq.common.mvc.dto`）：
  - 入参：`PageRequest<T>`，`current` / `size` / `condition`，`@Min(1)` 校验已经在 PageRequest 内部
  - 出参：`PageResponse<T>`，`{ total, data }`
  - 触发 `condition` 嵌套校验：`@RequestBody @Validated @Valid PageRequest<XxxQuery>`
- **不要在新代码里使用旧体系**（`R.ok().put("data", ...)` + `Map<String,Object>` 入参）—— 仅旧模块沿用
- **响应包装切面 `RestControllerAspect` 只覆盖 `com.gjzq.modules.mobile.controller`**，新模块（如 `hr`、`com` 后台）Controller 直接返回类型化 DTO，前端拿到的就是裸 JSON
- **路径与命名**：`@RequestMapping("/<模块>/<资源>")`，例如 `/hr/ehrDept`、`/glrba/thRpShareholding`
- **方法体不超过 10 行**：超出说明业务逻辑漏到了 Controller，下沉到 Service
- **依赖注入**：构造器注入 + `@AllArgsConstructor`（Lombok），字段声明为 `private final`
- **示例参考**：`EhrDeptController`

```java
@PostMapping("/search")
public PageResponse<EhrDeptPathVO> search(
        @RequestBody @Validated @Valid PageRequest<EhrDeptSearchQuery> pageRequest) {
    List<EhrDeptPathVO> data = ehrDeptQueryService.searchByNameWithFullPath(
            pageRequest.getCondition().getKeyword());
    return new PageResponse<>(data.size(), data);
}
```

### DTO / VO / Entity

**命名约定**：
- `Entity`：一一对应数据表，置于 `entity/` 包
- `Query` / `DTO`：HTTP 请求体 / Service 入参，置于 `dto/` 包；Controller 入参类必须有校验注解（`@NotBlank` / `@NotNull` / `@Min` 等）
- `VO`：HTTP 响应体 / 视图，置于 `vo/` 包

**反模式**：
- ❌ Entity 字段塞别的语义（如把 `sys_connect_by_path` 结果塞进 `name`）—— 新增 VO 显式区分
- ❌ Service 直接返回 Entity 给 Controller —— 跨层时用 VO，避免数据库字段泄漏到接口契约
- ❌ Controller 入参用 `Map<String, Object>` —— 不可校验、不可文档化，新模块禁止使用

### 单元测试

- **Service 单测**：Mock `Mapper`，验证转发与入参校验，不在测试里跑真实 SQL（DB 行为由集成测试覆盖）
- **集成测试固定连 dev 库**：`@ActiveProfiles("dev")`（本地库，见 `application-dev.yml`），**禁止**用 `test` profile（其指向远程测试库 `172.24.127.200`）
- **Controller 单测**：Mock `Service`，验证 `PageResponse` 包装与参数透传
- **测试命名**：`方法名_场景_期望行为`（如 `searchByNameWithFullPath_blankKeyword_returnsEmpty`）或描述性 camelCase
- **新方法必须先写测试（RED）再写实现（GREEN）**，参考 `EhrDeptQueryServiceImplTest` / `EhrDeptControllerTest`
- 项目 `pom.xml` 默认 `<skipTests>true</skipTests>`，本地验证用：
  ```bash
  # 临时把 <skipTests>true</skipTests> 改为 ${skipTests} 后
  mvn -pl gjzqDbGlf-Service test -Dtest=XxxTest -DskipTests=false
  # 验证完恢复 pom.xml
  ```

### 选型速查

| 维度 | 新模块（推荐） | 老模块（仅维护，不复制风格） |
|---|---|---|
| 响应封装 | `PageResponse<T>` / 类型化 DTO | `R.ok().put(...)` |
| 分页入参 | `PageRequest<T>` | `Map<String, Object>` + `Query` |
| Mapper SQL | `@Select` 注解 / XML | 主流是 `QueryWrapper`，必要时 XML |
| Lombok | 在 Entity / DTO / VO 上用 `@Data`、Controller 用 `@AllArgsConstructor` | 同 |

---

## 开发注意事项

### 不要做的事情

- ❌ 不要使用 `cnpm` 安装依赖（会有各种 bug）
- ❌ 不要提交 `node_modules/`、`target/` 等构建产物
- ❌ 不要在代码中硬编码敏感信息（密钥、密码等）
- ❌ 不要使用空的 catch 块
- ❌ 不要提交 `docs/` 目录的内容
- ❌ 不要提交 `.DS_Store` 等 IDE/系统文件
- ❌ 不要在循环内执行数据库查询（N+1 问题）

### 必须做的事情

- ✅ 所有涉及数据修改的方法必须添加 `@Transactional` 注解
- ✅ SQL 查询使用参数化，避免 SQL 注入
- ✅ 捕获异常时记录完整的异常堆栈信息
- ✅ 提交前检查是否包含敏感信息
- ✅ 使用统一的错误处理模式

### 提交代码前检查清单

- [ ] 运行 linter 检查代码风格
- [ ] 运行测试确保功能正常
- [ ] 检查是否有 console.log 或调试代码
- [ ] 确认没有敏感信息泄露
- [ ] 确认事务注解使用正确
- [ ] 确认异常处理完整（包含堆栈）

---

## Git 提交规范

使用以下格式：

```
JiraID: <jiraId> Msg: <msg>
```

**示例**：
```
JiraID: SZHGJC-147 Msg: 测试环境数据库地址修改
JiraID: SZHGJC-148 Msg: 附件关联逻辑优化
JiraID: SZHGJC-149 Msg: 公募基金解析问题修复
```

**重要**: 提交中包含的 JiraID 必须是有效的。推送时会进行严格校验，需要手动确认 JiraID 存在才能推送到远程仓库。

---

## 常用命令

### 开发相关

```bash
# 移动端开发
cd emp-shr-mobile && npm run dev

# PC 管理后台开发
cd emp-shr-web && npm run dev

# 后端开发
cd emp-shr-backend && mvn spring-boot:run
```

### Git 操作

```bash
# 提交代码（包含 JiraID）
git add .
git commit -m "JiraID: SZHGJC-150 Msg: 新增功能"

# 推送到远程（会进行 JiraID 校验）
git push origin main

# 查看 Git hooks
ls -la .git/hooks/
```

## 项目信息

员工投资申报系统（ygtz）：移动端申报 + PC 审核后台 + Java 后端服务。
详见 [docs/PROJECT_CONFIG.yml](docsFile/PROJECT_CONFIG.yml)
