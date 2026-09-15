# 项目配置表 + 敏感词过滤 — Design

| 项目 | 内容 |
| ---- | ---- |
| Change ID | 2026-09-15-config-sensitive-words |
| 依赖调研 | 文本输入点 9 个 DTO；change 版本账已至 10；JetCache 注解模式（UserCacheService） |

## 1. 数据库设计

### 1.1 `sql/change11_project_config.sql`（version=11）

```sql
USE zhenxinjian;

CREATE TABLE IF NOT EXISTS project_config (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    config_key   VARCHAR(100) NOT NULL COMMENT '配置键（小写点分，如 sensitive.filter.enabled）',
    config_value TEXT         DEFAULT NULL COMMENT '配置值（SECRET 类型为 AES 密文，带 enc: 前缀）',
    value_type   TINYINT      NOT NULL COMMENT '值类型：1字符串 2数字 3布尔 4JSON 5密文（见 ConfigValueTypeEnum）',
    remark       VARCHAR(255) DEFAULT NULL COMMENT '配置说明',
    -- 规约列
    status       TINYINT      DEFAULT 1 COMMENT '状态：0停用 1有效',
    create_by    VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
    update_by    VARCHAR(64)  DEFAULT NULL COMMENT '更新人',
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    delete_flag  TINYINT      DEFAULT 0 COMMENT '软删除标记：0未删除 1已删除',
    version      INT          DEFAULT 0 COMMENT '乐观锁版本号',
    -- 活跃唯一：同一 key 至多一条活跃配置（已删恒为 NULL 不占约束）
    key_active   VARCHAR(100) GENERATED ALWAYS AS (IF(delete_flag = 0, config_key, NULL)) STORED COMMENT '活跃配置生成列（config_key），兜底一键一活',
    PRIMARY KEY (id),
    UNIQUE KEY uk_project_config_key_active (key_active),
    KEY idx_project_config_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目配置表：组件凭据/运营开关，SECRET 值 AES 加密';

-- 种子配置（敏感词组件三项，幂等）
INSERT IGNORE INTO project_config (config_key, config_value, value_type, remark, create_by)
VALUES
('sensitive.filter.enabled',      'true', 3, '敏感词过滤总开关', 'system'),
('sensitive.filter.extra-words',  '[]',   4, '敏感词追加词，JSON 数组', 'system'),
('sensitive.filter.exclude-words','[]',   4, '敏感词排除词（内置词库豁免），JSON 数组', 'system');

-- 版本账（SqlRunner 自动执行 change 脚本并补账；此行幂等）
INSERT IGNORE INTO schema_migrations(version, script) VALUES (11, 'change11_project_config.sql');
```

### 1.2 `data.sql` 基线同步

在 food_images 建表段之后追加同一份 `CREATE TABLE IF NOT EXISTS project_config`（保持基线文档与库结构一致，不含种子数据）。

## 2. 后端设计

### 2.1 枚举与常量

- `common/enums/ConfigValueTypeEnum.java`：`STRING(1)/NUMBER(2)/BOOLEAN(3)/JSON(4)/SECRET(5)`，code+desc+of，规约格式同 `FoodImageSourceEnum`
- `common/constant/ProjectConfigKeyConstant.java`：已知 key 常量 + key 格式正则（`^[a-z0-9]+(\.[a-z0-9-]+)+$`，至少两段）

```java
public final class ProjectConfigKeyConstant {
    public static final String SENSITIVE_FILTER_ENABLED = "sensitive.filter.enabled";
    public static final String SENSITIVE_FILTER_EXTRA_WORDS = "sensitive.filter.extra-words";
    public static final String SENSITIVE_FILTER_EXCLUDE_WORDS = "sensitive.filter.exclude-words";
    // SECRET 类示例（任务#4 接入时使用）：ocr.api-key / llm.api-key，change12 落种子
}
```

### 2.2 PO / Mapper

- `domain/po/ProjectConfig.java`：`@TableName("project_config")`，`@TableLogic delete_flag`、`@Version version`、`status/configKey/configValue/valueType/remark` + 审计列
- `mapper/ProjectConfigMapper.java`：`extends BaseMapper<ProjectConfig>`，`@Mapper`

### 2.3 加密（ConfigService 内私有方法，不单独建组件）

- 算法：Hutool `SecureUtil.aes(key)`（AES/ECB/PKCS5Padding + Base64），密文统一加前缀 `enc:`，读取时按前缀判别明文/密文（兼容手工插入的明文值）
- 密钥来源：环境变量 `CONFIG_AES_KEY`（16/24/32 字节字符串，dev 默认在 application-dev.yml 注释说明，禁止提交真实值）
- 行为约束：
  - 保存/修改 `value_type=SECRET` 且密钥未配置 → 抛错（`ExceptionConstant.CONFIG_SECRET_KEY_MISSING`）
  - 读取 SECRET 值（内部 `getValue`）→ 解密后返回；解密失败抛 `CONFIG_SECRET_DECRYPT_FAIL`
  - admin VO 下发：SECRET 值一律脱敏为 `******`，其余类型原样下发

### 2.4 ConfigService（`service/ConfigService.java` + impl）

```java
// 读（走缓存；不暴露给 Controller，仅内部依赖注入使用）
String getValue(String configKey);              // 解密内聚：SECRET 值读取时解密；停用/软删/不存在 → null
boolean getBoolean(String configKey, boolean defaultValue);
int getInt(String configKey, int defaultValue);
List<String> getStringList(String configKey);   // JSON 数组值

// 写（仅 AdminConfigController 调用；operator 由实现内取 UserContext 当前登录用户）
IPage<ProjectConfigVO> page(String keyword, long page, size);          // 与既有 admin 控制器 IPage 风格一致
ProjectConfigVO create(ProjectConfigCreateDTO dto);                    // key 活跃唯一，重复抛 CONFIG_KEY_DUPLICATE
ProjectConfigVO update(Long id, ProjectConfigUpdateDTO dto);           // value/remark/status；key 与 valueType 不可改；返回 VO 供 Controller 取 key 判断是否触发词库重建
```

- 缓存：`@Cached(name = CacheConstant.CONFIG, key = "#configKey", cacheType = BOTH, syncLocal = true, cacheNullValue = true, expire = DEFAULT_REMOTE_EXPIRE_SECONDS, localExpire = DEFAULT_LOCAL_EXPIRE_SECONDS)`；写路径统一 `@CacheInvalidate(name = CacheConstant.CONFIG, key = "#configKey")`（`CacheConstant` 新增 `CONFIG = "config"`）
- 语义：config_value 为 NULL 也缓存（cacheNullValue）；status=0 停用时读取返回 null
- 缓存失效后敏感词组件的重建由 Controller 调用 `SensitiveWordFilter.refresh()` 触达（见 2.6）

### 2.5 AdminConfigController（`/admin/configs`）

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| GET | `/admin/configs` | 分页；`keyword` 模糊匹配 config_key/remark；返回 `PageResult<ProjectConfigVO>` |
| POST | `/admin/configs` | 新增；key 活跃唯一校验；SECRET 值此时需加密落库 |
| PUT | `/admin/configs/{id}` | 修改 value/remark/status；SECRET 传 `******` 或空 = 不改；否则按新值加密落库 |

- DTO：`ProjectConfigCreateDTO`（configKey `@NotBlank @Size(100) @Pattern(小写点分)`、valueType `@NotNull`、configValue `@Size(4000)`、remark `@Size(255)`）、`ProjectConfigUpdateDTO`（configValue、remark、status）
- 权限沿用现有 admin 拦截（`/admin/**`），`operator` 取当前登录用户
- 修改 `sensitive.filter.*` 三个 key 后（Controller 层判断 key 前缀 `sensitive.filter`）调用 `SensitiveWordFilter.refresh()`

### 2.6 敏感词组件

- 词库文件：`src/main/resources/sensitive-words.txt`，UTF-8，一行一词，`#` 开头为注释行；内容为基础词库（广告导流/辱骂/色情/赌博等类目，数百词），随版本发布
- 组件：`common/sensitive/SensitiveWordFilter.java`（`@Component`）

```java
@Component
public class SensitiveWordFilter {
    // 依赖 ConfigService；volatile WordTree + volatile boolean enabled 保证刷新可见性
    public void refresh();            // 重建：内置词库 − 排除词 + 追加词，全量小写
    public void check(String text);   // enabled=false 或空白 → 放行；命中 → BusinessException(CONTAINS_SENSITIVE_WORD)
    public boolean contains(String text); // 供测试/特殊场景
}
```

- 匹配引擎：Hutool `cn.hutool.dfa.WordTree`（hutool-all 已含，无需新增依赖）；`addWords`/`isMatch(text)` 单命中即判定（比 matchAll 全量收集更省）；文本与词统一 `toLowerCase` 处理大小写
- 排除词实现：加载内置词库后按 `exclude-words` 差集移除（WordTree 无 exclude API，集合层面做差）
- 初始化：`@PostConstruct refresh()`；ConfigService 不可用/词库读取失败 → 记 ERROR 且 enabled=false（fail-open，不阻塞启动），不抛异常
- 开关语义：`sensitive.filter.enabled` 读布尔，false 时 `check()` 直接放行（fail-open）

### 2.7 Service 层接入（5 处，统一在 Service 方法入口校验）

| Service | 方法 | 过滤字段 |
| ---- | ---- | ---- |
| `CustomFoodService` | saveCustomFood / editCustomFood | name、alias |
| `DietService` | createDietRecord / updateDietRecord | name（source=3 手动输入时）、remark |
| `WechatAuthServiceImpl` | wechatLogin | nickname（非空时） |
| `AdminFoodService` | saveFood / updateFood | name、alias |
| `UserServiceImpl`（admin 用户） | createUser / updateUser | nickname、remark |

- 构造器注入 `SensitiveWordFilter`，各构造器参数 +1；对应测试类补 mock
- 错误文案：`ExceptionConstant` 新增 `CONTAINS_SENSITIVE_WORD`（"内容包含违规词汇，请修改后重试"）

### 2.8 `ZhenxinjianProperties` 增加

```java
private final Config config = new Config();
public static class Config { private String aesKey; }   // 环境变量 CONFIG_AES_KEY
```

## 3. 前端设计（zhenxinjian-front）

- `src/api/adminConfig.ts`：`pageConfigs(params)` / `createConfig(data)` / `updateConfig(id, data)`；类型 `ProjectConfig { id, configKey, configValue, valueType, remark, status, updateTime }`，`valueType: 1|2|3|4|5`
- `src/view/configs/index.vue`：
  - 搜索栏（keyword 模糊）、表格（key / 类型 tag / 值（SECRET 脱敏显示 `******`）/ 状态 switch / 备注 / 更新时间 / 操作）
  - 新增弹窗（key、类型下拉、值 textarea（JSON/多行）、备注）；编辑弹窗（key/类型只读；SECRET 显示占位 `******`，留空不改）
  - 分页；沿用列表页 el-card + el-table 现有风格
- 路由 + 菜单：`/configs`，meta.title「系统配置」，加入后台侧边栏（仪表盘之后或末尾）

## 4. 测试设计

- `ConfigServiceImplTest`：create（key 重复抛错/SECRET 加密落库带 enc: 前缀）、update（SECRET 传 `******` 不改值/改值重加密/停用后 getValue 返回 null）、page 过滤、缓存注解语义由 verify(@CacheInvalidate) 层面简化为直调（mock mapper）
- `SensitiveWordFilterTest`：内置词命中、追加词生效、排除词豁免、开关关闭放行、空白/纯符号放行、大小写不敏感、refresh 后词集变化
- `AdminConfigControllerTest`：GET 分页 200、POST 重复 key 4xx、PUT 修改成功、SECRET 脱敏下发断言
- 现有测试适配：CustomFoodServiceTest / DietServiceTest / WechatAuthService 相关 / AdminFoodServiceTest / UserServiceImplTest 构造器补 `SensitiveWordFilter` mock（默认放行）
- 单测数据库：沿用 H2（h2-schema.sql 同步追加 project_config 建表）

## 5. 实施顺序与风险

1. change SQL + data.sql + h2-schema.sql → 2. 枚举/常量/PO/Mapper → 3. ConfigService + 加密 + 缓存 → 4. AdminConfigController + 测试 → 5. 词库文件 + SensitiveWordFilter + 接入 5 处 + 测试适配 → 6. 前端页 → 7. 本地启动端到端验证（含 change10 预热回归）

| 风险 | 缓解 |
| ---- | ---- |
| CONFIG_AES_KEY 未配置导致 SECRET 不可用 | 仅影响 SECRET 类型，普通类型不受影响；启动不校验、用时抛明确错误 |
| 词库误杀正常词（如「鸡胸」类食物词撞库） | exclude-words 配置项即时豁免；开关可整体关闭 |
| 构造器变更导致测试大面积红 | 每处接入同步补 mock，tasks 单列验证步骤 |
| JetCache 缓存了旧词库配置 | refresh() 由 Controller 写路径显式触发，且本地缓存 syncLocal 广播失效 |
