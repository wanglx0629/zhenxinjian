# 项目配置表 + 敏感词过滤 — Tasks

> 执行顺序即编号顺序；每步完成后勾选 `[x]`。规约：错误文案进 `ExceptionConstant`、注释 `作者: wanglx`、建表必建字典枚举。

## 1. 数据库

- [x] 1.1 新建 `sql/change11_project_config.sql`：project_config 建表（含 key_active 生成列活跃唯一）+ 3 条敏感词种子配置（INSERT IGNORE）+ schema_migrations 补账 version=11
- [x] 1.2 `data.sql` 基线同步：追加 project_config `CREATE TABLE IF NOT EXISTS` 段（food_images 之后，不含种子）
- [x] 1.3 `src/test/resources/sql/h2-schema.sql` 同步追加 project_config 建表（单测库）
- [x] 1.4 本地库执行 change11 SQL 并验证表/种子/补账（mysql CLI，参照 change10 流程）

## 2. 后端配置表

- [x] 2.1 `common/enums/ConfigValueTypeEnum.java`（STRING/NUMBER/BOOLEAN/JSON/SECRET，code+desc+of）
- [x] 2.2 `common/constant/ProjectConfigKeyConstant.java`（3 个敏感词 key 常量 + key 格式正则）
- [x] 2.3 `domain/po/ProjectConfig.java` + `mapper/ProjectConfigMapper.java`（@TableLogic/@Version/审计列）
- [x] 2.4 `CacheConstant` 新增 `CONFIG = "config"` 区
- [x] 2.5 `ZhenxinjianProperties` 增加 `config.aes-key`（环境变量 `CONFIG_AES_KEY`）；application-dev.yml 注释说明（不提交真实值）
- [x] 2.6 `service/ConfigService.java` + `service/impl/ConfigServiceImpl.java`：读接口（getValue/getBoolean/getInt/getStringList，JetCache BOTH + cacheNullValue）+ 写接口（page/create/update）；SECRET 加密（enc: 前缀）与解密；错误文案 `CONFIG_KEY_DUPLICATE`/`CONFIG_SECRET_KEY_MISSING`/`CONFIG_SECRET_DECRYPT_FAIL` 进 `ExceptionConstant`
- [x] 2.7 `domain/dto/ProjectConfigCreateDTO.java` + `ProjectConfigUpdateDTO.java` + `domain/vo/ProjectConfigVO.java`（SECRET 下发脱敏 `******`）
- [x] 2.8 `controller/AdminConfigController.java`：GET 分页 / POST 新增 / PUT /{id} 修改；修改 `sensitive.filter.*` 后触发 `SensitiveWordFilter.refresh()`；Swagger 注解
- [x] 2.9 单测 `ConfigServiceImplTest`（加密往返、key 重复、停用读取 null、SECRET `******` 不改值）

## 3. 敏感词组件（任务#3）

- [x] 3.1 词库文件 `src/main/resources/sensitive-words.txt`（基础词库数百词，一行一词 # 注释）
- [x] 3.2 `ExceptionConstant` 新增 `CONTAINS_SENSITIVE_WORD`
- [x] 3.3 `common/sensitive/SensitiveWordFilter.java`：@PostConstruct refresh()；check/contains；volatile WordTree；追加/排除差集；开关 fail-open；加载失败记 ERROR 不阻塞启动
- [x] 3.4 接入 `CustomFoodService`（name/alias）
- [x] 3.5 接入 `DietService`（source=3 name / remark）
- [x] 3.6 接入 `WechatAuthServiceImpl`（nickname 非空时）
- [x] 3.7 接入 `AdminFoodService`（name/alias）
- [x] 3.8 接入 `UserServiceImpl`（nickname/remark）
- [x] 3.9 单测 `SensitiveWordFilterTest`（内置命中/追加/排除/开关/空白/大小写/refresh）
- [x] 3.10 现有测试适配：CustomFoodServiceTest、DietServiceTest、WechatAuthService 测试、AdminFoodServiceTest、UserServiceImplTest 补 SensitiveWordFilter mock

## 4. 后台前端

- [x] 4.1 `src/api/adminConfig.ts`（类型 + 三个接口）
- [x] 4.2 `src/view/configs/index.vue`（搜索/表格/状态 switch/新增与编辑弹窗，SECRET 脱敏）
- [x] 4.3 路由 + 侧边栏菜单「系统配置」

## 5. 验证

- [x] 5.1 后端全量 `mvn test` 通过
- [x] 5.2 本地启动：建表/补账生效、change10 图片预热回归正常（仍 200/0 失败）
- [x] 5.3 端到端：admin 配置页 CRUD + SECRET 脱敏；配置追加词后，自定义食物名提交命中词被拦截、exclude 豁免、enabled=false 放行；`/api/food/hot` 图片字段正常
