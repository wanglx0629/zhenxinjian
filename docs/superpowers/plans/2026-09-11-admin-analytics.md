# 管理后台与行为分析 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为臻心减建设管理后台：行为埋点采集与 DAU/MAU 统计、数据看板、用户管理改造、食物库维护、饮食记录只读查看。

**Architecture:** 小程序批量上报埋点 → `track_event` 明细单表；每日 01:00 定时聚合 → `stat_daily_active` / `stat_event_daily`；管理端 `/api/admin/**` 接口（ADMIN 鉴权）今日实时查明细、昨日及以前查聚合表；前端 zhenxinjian-front 四个模块页（看板/用户/食物库/饮食记录）。

**Tech Stack:** Spring Boot 3.5 + MyBatis-Plus + MySQL 8（后端）；Vue 3 + Element Plus + ECharts 5（管理端，echarts ^5.5.0 已在依赖）；UniApp Vue 3（小程序埋点采集）。

**Spec:** `docs/superpowers/specs/2026-09-11-admin-analytics-design.md`

## Global Constraints

- 所有表必备 `delete_flag`（`@TableLogic` 逻辑删除）+ `status` 状态列 + 审计列（create_by/update_by/create_time/update_time）+ `version` 乐观锁；软删表禁普通 UNIQUE。
- 新列/新表必须同时建字典枚举（`common/enums/`，code+desc+of 三件套）。
- 错误文案进 `ExceptionConstant`；错误码进 `CommonConstant`；本变更启用 409xx 段位；接口返回 `Result`。
- 管理端接口统一 `@PreAuthorize("hasRole('ADMIN')")`；注释含 `作者: wanglx`（另起一行）。
- Git 提交信息：功能用 `feature：功能说明`，文档用 `docs(openspec): 说明`。
- 活跃统计口径：当日 `track_event` 中 `user_id` 非 NULL 去重；统计一律用 `create_time`（不用 client_time）。
- 管理端「今日」数据实时查 `track_event`（界面标"实时"），昨日及以前查聚合表。
- 饮食记录查看只读：管理端不开放改/删用户业务数据。
- 落地流程遵守宪法 SDD 双循环：先 `openspec propose admin-analytics`（Task 1），实现后 `openspec validate --strict` + 归档（Task 13）。

---

### Task 1: openspec 变更提案（admin-analytics）

**Files:**
- Create: `openspec/changes/admin-analytics/proposal.md`
- Create: `openspec/changes/admin-analytics/design.md`
- Create: `openspec/changes/admin-analytics/tasks.md`
- Create: `openspec/changes/admin-analytics/specs/admin/stats/spec.md`
- Create: `openspec/changes/admin-analytics/specs/admin/food/spec.md`
- Create: `openspec/changes/admin-analytics/specs/admin/diet/spec.md`
- Create: `openspec/changes/admin-analytics/specs/track/event/spec.md`

**Interfaces:**
- Consumes: `docs/superpowers/specs/2026-09-11-admin-analytics-design.md`（内容真源）
- Produces: 合法 `openspec validate admin-analytics --strict` 通过的变更目录；后续 Task 2-12 与该变更 tasks.md 一一对应

- [ ] **Step 1: 复制设计文档为 design.md 并裁剪**

将 `docs/superpowers/specs/2026-09-11-admin-analytics-design.md` 全文复制为 `openspec/changes/admin-analytics/design.md`，开头补一行 `> 落地实施计划：docs/superpowers/plans/2026-09-11-admin-analytics.md`。

- [ ] **Step 2: 编写 proposal.md**

```markdown
# Proposal — 管理后台与行为分析（数据看板 + 食物库维护 + 饮食记录查看 + 埋点统计）

## Why
PRD 待确认项「管理后台一期功能边界」经确认为：食物库维护 + 用户管理 + 饮食记录查看 + 行为分析
（DAU/MAU + 功能点击埋点）。当前管理端仅有脚手架用户 CRUD，无业务运营能力，也无任何埋点采集。

## What Changes
- 新建 `track_event` 埋点明细表与 `stat_daily_active`/`stat_event_daily` 两张聚合表
- 小程序 `utils/track.ts` 批量上报（10s/20条 flush，失败重试，队列上限 200），接入 21 个事件码
- 后端 `POST /api/track/events` 白名单校验批量落库（登录态可选）
- `StatAggregateTask` 每日 01:00 幂等聚合（软删重插），支持按日期手工重跑
- 管理端 `/api/admin/**`：数据看板 5 接口、食物库维护 5 接口、饮食记录只读分页、用户聚合视图
- 前端 zhenxinjian-front：看板页（ECharts）/ 用户页改造加详情抽屉 / 食物库页 / 饮食记录页；
  删除 websocket/register 演示页与 Demo 组件

## Capabilities
### New Capabilities
- `track/event`: 埋点批量上报、白名单校验、事件码字典（TrackEventEnum 21 码）
- `admin/stats`: overview/active-trend/event-rank/page-rank/aggregate 五接口与每日聚合任务
- `admin/food`: 内置食物分页/新增（code F201 起顺延）/编辑/软删/停用启用
- `admin/diet`: 饮食记录只读分页（用户关键字/日期范围/餐别）
### Modified Capabilities
- `admin/user`: 现有 /users CRUD 之上补 `GET /admin/users/{id}/profile` 聚合视图

## Impact
- DB：`sql/change9_admin_analytics.sql`（三表）
- 后端：TrackController/TrackEventService、StatAggregateService/Task、AdminStats/AdminFood/AdminDiet/AdminUser 四控制器；TrackEventEnum；409xx 错误码段
- 小程序：`utils/track.ts` + `api/track.ts` + 16 页 onShow 接入 + 行为事件接入
- 前端 front：4 页 + 4 个 api 模块 + 路由/布局调整 + 演示残留删除
```

- [ ] **Step 3: 编写 tasks.md**

将本计划 Task 2–13 的标题与步骤摘要写入（每任务一节，复用本计划的 `- [ ]` 步骤行）。

- [ ] **Step 4: 编写 specs 四份 delta spec**

每份用 `## ADDED Requirements` + `#### Scenario:` 结构。场景清单：

`specs/track/event/spec.md`：批量上报成功 / 单批超 50 条拒绝 / 非法 eventCode 整批拒收 40901 / 未登录上报 user_id 为空 / extra 超 512 截断。

`specs/admin/stats/spec.md`：overview 今日实时 / active-trend 查聚合表 / event-rank TOP N / page-rank 按 page 聚合 / aggregate 重跑幂等 / 非 ADMIN 403。

`specs/admin/food/spec.md`：分页筛选 / 新增内置 code 顺延 / 同名拒收 40903 / 宏量与热量 ±10% 校验 / 自定义食物编辑拒绝 / 软删 / 停用后 C 端不可见。

`specs/admin/diet/spec.md`：只读分页 / 用户关键字无命中返回空页 / 日期范围与餐别筛选。

- [ ] **Step 5: 校验**

Run: `openspec validate admin-analytics --strict`
Expected: 通过

- [ ] **Step 6: Commit**

```bash
git add openspec/changes/admin-analytics
git commit -m "docs(openspec): 新增 admin-analytics 变更提案"
```

---

### Task 2: 数据库迁移脚本

**Files:**
- Create: `sql/change9_admin_analytics.sql`

**Interfaces:**
- Produces: `track_event` / `stat_daily_active` / `stat_event_daily` 三表（Task 3/4 的 PO 依赖此结构）

- [ ] **Step 1: 编写迁移脚本**

```sql
-- =============================================================
-- change9_admin_analytics.sql
-- 管理后台与行为分析：埋点明细表 + 活跃/事件日聚合表
-- 作者: wanglx
-- =============================================================

CREATE TABLE `track_event` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`     BIGINT       DEFAULT NULL COMMENT '用户ID（未登录为空）',
  `user_type`   VARCHAR(16)  DEFAULT NULL COMMENT '用户类型 WECHAT/GUEST（冗余，聚合免JOIN）',
  `event_code`  VARCHAR(64)  NOT NULL COMMENT '事件码（TrackEventEnum）',
  `event_name`  VARCHAR(64)  NOT NULL COMMENT '事件中文名（冗余）',
  `page`        VARCHAR(64)  DEFAULT NULL COMMENT '页面路径（PV/点击类填写）',
  `extra_json`  VARCHAR(512) DEFAULT NULL COMMENT '扩展JSON（关键词/餐别/食物ID等）',
  `client_time` DATETIME     DEFAULT NULL COMMENT '端上发生时间（仅记录，不参与统计）',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态:0停用 1有效',
  `create_by`   VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
  `update_by`   VARCHAR(64)  DEFAULT NULL COMMENT '更新人',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（统计口径时间）',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `delete_flag` TINYINT      NOT NULL DEFAULT 0 COMMENT '软删除:0未删 1已删',
  `version`     INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_user_time` (`user_id`, `create_time`),
  KEY `idx_code_time` (`event_code`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '埋点明细表';

CREATE TABLE `stat_daily_active` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `stat_date`   DATE     NOT NULL COMMENT '统计日期',
  `dau`         INT      NOT NULL DEFAULT 0 COMMENT '当日去重活跃用户（user_id非NULL）',
  `guest_dau`   INT      NOT NULL DEFAULT 0 COMMENT '其中游客数（user_type=GUEST）',
  `new_user`    INT      NOT NULL DEFAULT 0 COMMENT '当日新增注册用户',
  `status`      TINYINT  NOT NULL DEFAULT 1 COMMENT '状态:0停用 1有效',
  `create_by`   VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `update_by`   VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `delete_flag` TINYINT  NOT NULL DEFAULT 0 COMMENT '软删除:0未删 1已删',
  `version`     INT      NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  KEY `idx_stat_date` (`stat_date`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '每日活跃聚合表';

CREATE TABLE `stat_event_daily` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `stat_date`   DATE        NOT NULL COMMENT '统计日期',
  `event_code`  VARCHAR(64) NOT NULL COMMENT '事件码',
  `event_name`  VARCHAR(64) NOT NULL COMMENT '事件中文名',
  `pv`          INT         NOT NULL DEFAULT 0 COMMENT '当日事件总次数',
  `uv`          INT         NOT NULL DEFAULT 0 COMMENT '当日去重用户数',
  `status`      TINYINT     NOT NULL DEFAULT 1 COMMENT '状态:0停用 1有效',
  `create_by`   VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `update_by`   VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `delete_flag` TINYINT     NOT NULL DEFAULT 0 COMMENT '软删除:0未删 1已删',
  `version`     INT         NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  KEY `idx_date_code` (`stat_date`, `event_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '事件日聚合表';
```

- [ ] **Step 2: 本地执行并核对表结构**

Run: 本地 MySQL 执行该脚本后 `SHOW CREATE TABLE track_event;`
Expected: 三表建成、utf8mb4、注释齐全、三索引生效

- [ ] **Step 3: Commit**

```bash
git add sql/change9_admin_analytics.sql
git commit -m "feature：管理后台行为分析三表迁移脚本（change9）"
```

---

### Task 3: 埋点上报后端（枚举 + DTO + 接口）

**Files:**
- Create: `apps/zhenxinjian-backend/src/main/java/cn/zhenxinjian/common/enums/TrackEventEnum.java`
- Create: `apps/zhenxinjian-backend/src/main/java/cn/zhenxinjian/domain/po/TrackEvent.java`
- Create: `apps/zhenxinjian-backend/src/main/java/cn/zhenxinjian/mapper/TrackEventMapper.java`
- Create: `apps/zhenxinjian-backend/src/main/java/cn/zhenxinjian/domain/dto/TrackEventItemDTO.java`
- Create: `apps/zhenxinjian-backend/src/main/java/cn/zhenxinjian/domain/dto/TrackEventBatchDTO.java`
- Create: `apps/zhenxinjian-backend/src/main/java/cn/zhenxinjian/service/impl/TrackEventService.java`
- Create: `apps/zhenxinjian-backend/src/main/java/cn/zhenxinjian/controller/TrackController.java`
- Modify: `apps/zhenxinjian-backend/src/main/java/cn/zhenxinjian/common/constant/ExceptionConstant.java`（408xx 段后追加 409xx）
- Modify: `apps/zhenxinjian-backend/src/main/java/cn/zhenxinjian/common/constant/CommonConstant.java`（追加 409xx 码 + 批量上限常量）
- Modify: `apps/zhenxinjian-backend/src/main/resources/application.yml:86-97`（permit-urls 追加 `/track/events`）
- Test: `apps/zhenxinjian-backend/src/test/java/cn/zhenxinjian/service/impl/TrackEventServiceTest.java`

**Interfaces:**
- Consumes: Task 2 的 `track_event` 表；既有 `UserContext.getUserId()`（未登录返回 null）；`BusinessException(code, message)`
- Produces: `TrackEventEnum.of(String code)`；`TrackEventService.saveBatch(Long userId, String userType, TrackEventBatchDTO dto)`（Task 5 的统计、Task 8 的上报依赖）

- [ ] **Step 1: 追加 409xx 错误码**

`ExceptionConstant.java` 在 408xx 段后追加：

```java
    // ==================== 管理后台/埋点（409xx） ====================

    /** 40901 埋点事件非法 */
    public static final String TRACK_EVENT_INVALID = "埋点事件非法";

    /** 40902 统计参数非法 */
    public static final String STATS_PARAM_INVALID = "统计参数非法";

    /** 40903 食物维护冲突 */
    public static final String ADMIN_FOOD_CONFLICT = "食物名称已存在或维护冲突";
```

`CommonConstant.java` 在 408xx 段后追加：

```java
    // ==================== 管理后台/埋点错误码（409xx） ====================

    /** 埋点事件非法（事件码不在白名单/单批超上限） */
    public static final int TRACK_EVENT_INVALID_CODE = 40901;

    /** 统计参数非法（日期/天数越界） */
    public static final int STATS_PARAM_INVALID_CODE = 40902;

    /** 食物维护冲突（同名/非内置写操作） */
    public static final int ADMIN_FOOD_CONFLICT_CODE = 40903;

    /** 埋点单批上报上限 */
    public static final int TRACK_BATCH_MAX_SIZE = 50;

    /** 埋点扩展字段最大长度 */
    public static final int TRACK_EXTRA_MAX_LENGTH = 512;
```

- [ ] **Step 2: 编写失败测试**

`TrackEventServiceTest.java`（Mockito 风格同 CustomFoodServiceTest）：

```java
package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.domain.dto.TrackEventBatchDTO;
import cn.zhenxinjian.domain.dto.TrackEventItemDTO;
import cn.zhenxinjian.mapper.TrackEventMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrackEventServiceTest {

    @Mock
    private TrackEventMapper trackEventMapper;

    @InjectMocks
    private TrackEventService trackEventService;

    private TrackEventItemDTO item(String code) {
        TrackEventItemDTO dto = new TrackEventItemDTO();
        dto.setEventCode(code);
        return dto;
    }

    @Test
    void saveBatch_validEvents_insertWithEnumName() {
        TrackEventBatchDTO batch = new TrackEventBatchDTO();
        batch.setEvents(List.of(item("record_add"), item("page_view")));
        trackEventService.saveBatch(1L, "WECHAT", batch);
        verify(trackEventMapper).insert(anyList());
    }

    @Test
    void saveBatch_invalidCode_rejectWholeBatch40901() {
        TrackEventBatchDTO batch = new TrackEventBatchDTO();
        batch.setEvents(List.of(item("record_add"), item("hack_event")));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> trackEventService.saveBatch(1L, "WECHAT", batch));
        assertEquals(CommonConstant.TRACK_EVENT_INVALID_CODE, ex.getCode());
        verify(trackEventMapper, never()).insert(anyList());
    }

    @Test
    void saveBatch_nullUser_allowed() {
        TrackEventBatchDTO batch = new TrackEventBatchDTO();
        batch.setEvents(List.of(item("login_guest")));
        assertDoesNotThrow(() -> trackEventService.saveBatch(null, null, batch));
        verify(trackEventMapper).insert(anyList());
    }
}
```

- [ ] **Step 3: 跑测试确认失败**

Run: `cd apps/zhenxinjian-backend && mvn test -Dtest=TrackEventServiceTest`
Expected: 编译失败（类不存在）

- [ ] **Step 4: 实现 TrackEventEnum**

```java
package cn.zhenxinjian.common.enums;

/**
 * 埋点事件码字典枚举（对应 track_event.event_code 列；后端唯一真源）
 * 作者: wanglx
 */
public enum TrackEventEnum {

    LOGIN_WECHAT("login_wechat", "微信登录"),
    LOGIN_GUEST("login_guest", "游客进入"),
    LOGIN_FAIL("login_fail", "登录失败"),
    PAGE_VIEW("page_view", "页面访问"),
    RECORD_ADD("record_add", "添加饮食记录"),
    RECORD_EDIT("record_edit", "编辑饮食记录"),
    RECORD_DELETE("record_delete", "删除饮食记录"),
    FOOD_SEARCH("food_search", "食物搜索"),
    FOOD_DETAIL("food_detail", "查看食物详情"),
    FOOD_CUSTOM_ADD("food_custom_add", "添加自定义食物"),
    MODE_SELECT("mode_select", "选择模式"),
    MODE_SWITCH("mode_switch", "切换模式"),
    PLAN_VIEW("plan_view", "查看计划"),
    REMINDER_SAVE("reminder_save", "保存提醒设置"),
    REMINDER_SUBSCRIBE("reminder_subscribe", "订阅提醒授权"),
    BODY_SAVE("body_save", "保存身体档案"),
    WEIGHT_ADD("weight_add", "记录体重"),
    MENSTRUAL_SAVE("menstrual_save", "保存经期设置"),
    HOME_QUICK_ENTRY("home_quick_entry", "首页快捷入口"),
    FOOD_HOT_CLICK("food_hot_click", "热门食物点击"),
    FOOD_HISTORY_CLICK("food_history_click", "历史搜索点击");

    private final String code;
    private final String desc;

    TrackEventEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static TrackEventEnum of(String code) {
        if (code == null) {
            return null;
        }
        for (TrackEventEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
```

- [ ] **Step 5: 实现 PO/Mapper/DTO**

`TrackEvent.java`（字段与 Task 2 表结构一一对应，风格同 ReminderSendLog：`@TableName("track_event")`、`@TableLogic deleteFlag`、`@Version`、`@TableField(fill=...)` 审计时间；`clientTime` 为 `LocalDateTime`，`page`/`extraJson`/`userType` 普通列）。

`TrackEventMapper.java`：

```java
package cn.zhenxinjian.mapper;

import cn.zhenxinjian.domain.po.TrackEvent;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 埋点明细 Mapper
 * 作者: wanglx
 */
@Mapper
public interface TrackEventMapper extends BaseMapper<TrackEvent> {
}
```

`TrackEventItemDTO.java`：`eventCode @NotBlank @Size(max=64)`；`page @Size(max=64)`；`extra Map<String,Object>`（可空）；`clientTime LocalDateTime`（可空，`@JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")`）。

`TrackEventBatchDTO.java`：`events @NotEmpty @Size(max=50) List<TrackEventItemDTO>`。

- [ ] **Step 6: 实现 TrackEventService**

```java
package cn.zhenxinjian.service.impl;

import cn.hutool.json.JSONUtil;
import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.constant.ExceptionConstant;
import cn.zhenxinjian.common.enums.TrackEventEnum;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.domain.dto.TrackEventBatchDTO;
import cn.zhenxinjian.domain.dto.TrackEventItemDTO;
import cn.zhenxinjian.domain.po.TrackEvent;
import cn.zhenxinjian.mapper.TrackEventMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 埋点事件服务（白名单校验 + 批量落库；埋点天然允许少量重复，不做严格幂等）
 * 作者: wanglx
 *
 * 口径：eventCode 白名单整批校验，任一非法整批拒收 40901；extra 序列化超 512 截断；
 * userId/userType 可为空（未登录引导页上报）；统计时间一律以 create_time 为准
 */
@Service
@RequiredArgsConstructor
public class TrackEventService {

    private final TrackEventMapper trackEventMapper;

    /**
     * 批量保存埋点事件
     *
     * @param userId   当前用户ID（未登录为 null）
     * @param userType 用户类型 WECHAT/GUEST（未登录为 null）
     * @param dto      批量事件入参
     */
    public void saveBatch(Long userId, String userType, TrackEventBatchDTO dto) {
        List<TrackEvent> entities = new ArrayList<>(dto.getEvents().size());
        for (TrackEventItemDTO item : dto.getEvents()) {
            TrackEventEnum event = TrackEventEnum.of(item.getEventCode());
            if (event == null) {
                throw new BusinessException(CommonConstant.TRACK_EVENT_INVALID_CODE,
                        ExceptionConstant.TRACK_EVENT_INVALID);
            }
            TrackEvent entity = new TrackEvent();
            entity.setUserId(userId);
            entity.setUserType(userType);
            entity.setEventCode(event.getCode());
            entity.setEventName(event.getDesc());
            entity.setPage(item.getPage());
            entity.setExtraJson(toExtraJson(item));
            entity.setClientTime(item.getClientTime());
            entity.setStatus(1);
            entity.setCreateBy(userId != null ? String.valueOf(userId) : CommonConstant.CREATE_BY_SYSTEM);
            entities.add(entity);
        }
        trackEventMapper.insert(entities);
    }

    /** extra 序列化并限长 512 */
    private String toExtraJson(TrackEventItemDTO item) {
        if (item.getExtra() == null || item.getExtra().isEmpty()) {
            return null;
        }
        String json = JSONUtil.toJsonStr(item.getExtra());
        if (json.length() > CommonConstant.TRACK_EXTRA_MAX_LENGTH) {
            return json.substring(0, CommonConstant.TRACK_EXTRA_MAX_LENGTH);
        }
        return json;
    }
}
```

- [ ] **Step 7: 实现 TrackController 并放行 permit-urls**

```java
package cn.zhenxinjian.controller;

import cn.zhenxinjian.common.result.Result;
import cn.zhenxinjian.common.utils.UserContext;
import cn.zhenxinjian.domain.dto.TrackEventBatchDTO;
import cn.zhenxinjian.domain.vo.LoginUserVO;
import cn.zhenxinjian.service.impl.TrackEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 埋点上报控制器（登录态可选：游客/未登录引导页也可上报）
 * 作者: wanglx
 */
@Tag(name = "埋点上报")
@RestController
@RequestMapping("/track")
@RequiredArgsConstructor
public class TrackController {

    private final TrackEventService trackEventService;

    @Operation(summary = "批量上报埋点事件", description = "单批≤50条；事件码白名单校验，任一非法整批拒收")
    @PostMapping("/events")
    public Result<Void> events(@Valid @RequestBody TrackEventBatchDTO dto) {
        LoginUserVO current = UserContext.get();
        trackEventService.saveBatch(
                current != null ? current.getId() : null,
                current != null ? current.getUserType() : null,
                dto);
        return Result.ok();
    }
}
```

`application.yml` permit-urls 列表（`/auth/guest` 之后）追加一行：

```yaml
      # 埋点上报（登录态可选）
      - /track/events
```

注意：需确认 `LoginUserVO` 是否有 `userType` 字段——若无则用 `UserMapper.selectById` 取或改从 JWT claim 读；实现时以现有 `UserContext.get()` 返回结构为准，取不到 userType 时传 null（聚合 guest_dau 依赖该列，优先保证有值：微信/游客登录签发的 token 含 userType claim，可在 JwtAuthenticationFilter 已写入 LoginUserVO 的前提下直接使用）。

- [ ] **Step 8: 跑测试确认通过 + 编译**

Run: `cd apps/zhenxinjian-backend && mvn test -Dtest=TrackEventServiceTest && mvn compile`
Expected: 3 测试 PASS，编译通过

- [ ] **Step 9: Commit**

```bash
git add apps/zhenxinjian-backend
git commit -m "feature：埋点批量上报后端（TrackEventEnum 白名单 + POST /track/events）"
```

---

### Task 4: 聚合服务与定时任务

**Files:**
- Create: `apps/zhenxinjian-backend/src/main/java/cn/zhenxinjian/domain/po/StatDailyActive.java`
- Create: `apps/zhenxinjian-backend/src/main/java/cn/zhenxinjian/domain/po/StatEventDaily.java`
- Create: `apps/zhenxinjian-backend/src/main/java/cn/zhenxinjian/mapper/StatDailyActiveMapper.java`
- Create: `apps/zhenxinjian-backend/src/main/java/cn/zhenxinjian/mapper/StatEventDailyMapper.java`
- Create: `apps/zhenxinjian-backend/src/main/java/cn/zhenxinjian/service/impl/StatAggregateService.java`
- Create: `apps/zhenxinjian-backend/src/main/java/cn/zhenxinjian/task/StatAggregateTask.java`
- Test: `apps/zhenxinjian-backend/src/test/java/cn/zhenxinjian/service/impl/StatAggregateServiceTest.java`

**Interfaces:**
- Consumes: Task 2 三表；Task 3 `TrackEventMapper`；既有 `UserMapper`
- Produces: `StatAggregateService.aggregate(LocalDate date)`（幂等）；`StatAggregateTask` 每日 01:00 调用（Task 5 管理端接口依赖聚合表数据）

- [ ] **Step 1: 编写失败测试**

```java
package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.domain.po.StatDailyActive;
import cn.zhenxinjian.domain.po.StatEventDaily;
import cn.zhenxinjian.mapper.StatDailyActiveMapper;
import cn.zhenxinjian.mapper.StatEventDailyMapper;
import cn.zhenxinjian.mapper.TrackEventMapper;
import cn.zhenxinjian.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatAggregateServiceTest {

    @Mock private TrackEventMapper trackEventMapper;
    @Mock private StatDailyActiveMapper statDailyActiveMapper;
    @Mock private StatEventDailyMapper statEventDailyMapper;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private StatAggregateService statAggregateService;

    @Test
    void aggregate_softDeleteThenInsert_idempotent() {
        LocalDate date = LocalDate.of(2026, 9, 10);
        when(trackEventMapper.selectCount(any(Wrapper.class))).thenReturn(5L, 2L);
        when(userMapper.selectCount(any(Wrapper.class))).thenReturn(3L);
        when(trackEventMapper.selectMaps(any(Wrapper.class))).thenReturn(List.of(
                Map.of("event_code", "page_view", "event_name", "页面访问", "pv", 20L, "uv", 5L)));

        statAggregateService.aggregate(date);

        // 幂等：先软删当日旧行再插新行
        verify(statDailyActiveMapper).delete(any(Wrapper.class));
        verify(statEventDailyMapper).delete(any(Wrapper.class));
        verify(statDailyActiveMapper).insert(any(StatDailyActive.class));
        verify(statEventDailyMapper).insert(any(StatEventDaily.class));
    }

    @Test
    void aggregate_noEvents_insertZeroRow() {
        LocalDate date = LocalDate.of(2026, 9, 10);
        when(trackEventMapper.selectCount(any(Wrapper.class))).thenReturn(0L, 0L);
        when(userMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(trackEventMapper.selectMaps(any(Wrapper.class))).thenReturn(List.of());

        statAggregateService.aggregate(date);

        verify(statDailyActiveMapper).insert(any(StatDailyActive.class));
        verify(statEventDailyMapper, never()).insert(any(StatEventDaily.class));
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `cd apps/zhenxinjian-backend && mvn test -Dtest=StatAggregateServiceTest`
Expected: 编译失败

- [ ] **Step 3: 实现 PO/Mapper**

`StatDailyActive`（`@TableName("stat_daily_active")`，字段 id/statDate/dau/guestDau/newUser/status/审计/deleteFlag/version，风格同 ReminderSendLog）；`StatEventDaily`（`@TableName("stat_event_daily")`，id/statDate/eventCode/eventName/pv/uv/规约列）。两个 Mapper 均 `extends BaseMapper<T>` + `@Mapper`。

- [ ] **Step 4: 实现 StatAggregateService**

```java
package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.domain.po.StatDailyActive;
import cn.zhenxinjian.domain.po.StatEventDaily;
import cn.zhenxinjian.domain.po.TrackEvent;
import cn.zhenxinjian.domain.po.User;
import cn.zhenxinjian.mapper.StatDailyActiveMapper;
import cn.zhenxinjian.mapper.StatEventDailyMapper;
import cn.zhenxinjian.mapper.TrackEventMapper;
import cn.zhenxinjian.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 行为统计聚合服务（每日聚合 track_event → stat_daily_active / stat_event_daily）
 * 作者: wanglx
 *
 * 幂等口径：软删表禁普通 UNIQUE，先逻辑删除当日旧行再插新行，支持按日期重跑补数；
 * 活跃口径：create_time 当日且 user_id 非 NULL 去重（COUNT(DISTINCT user_id)）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatAggregateService {

    private final TrackEventMapper trackEventMapper;
    private final StatDailyActiveMapper statDailyActiveMapper;
    private final StatEventDailyMapper statEventDailyMapper;
    private final UserMapper userMapper;

    /**
     * 聚合指定日期（重跑安全：同事务内软删旧行 + 插新行）
     *
     * @param date 统计日期
     */
    @Transactional(rollbackFor = Exception.class)
    public void aggregate(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        // 1. 计算 DAU / guest_dau / new_user
        Long dau = trackEventMapper.selectCount(new QueryWrapper<TrackEvent>()
                .select("COUNT(DISTINCT user_id) AS cnt")
                .isNotNull("user_id")
                .ge("create_time", start).lt("create_time", end));
        Long guestDau = trackEventMapper.selectCount(new QueryWrapper<TrackEvent>()
                .select("COUNT(DISTINCT user_id) AS cnt")
                .isNotNull("user_id")
                .eq("user_type", CommonConstant.USER_TYPE_GUEST)
                .ge("create_time", start).lt("create_time", end));
        Long newUser = userMapper.selectCount(Wrappers.<User>lambdaQuery()
                .ge(User::getCreateTime, start).lt(User::getCreateTime, end));

        // 2. 活跃聚合：软删当日旧行 + 插新行
        statDailyActiveMapper.delete(Wrappers.<StatDailyActive>lambdaQuery()
                .eq(StatDailyActive::getStatDate, date));
        StatDailyActive active = new StatDailyActive();
        active.setStatDate(date);
        active.setDau(dau.intValue());
        active.setGuestDau(guestDau.intValue());
        active.setNewUser(newUser.intValue());
        active.setStatus(1);
        active.setCreateBy(CommonConstant.CREATE_BY_SYSTEM);
        statDailyActiveMapper.insert(active);

        // 3. 事件聚合：按 event_code 分组 pv/uv
        statEventDailyMapper.delete(Wrappers.<StatEventDaily>lambdaQuery()
                .eq(StatEventDaily::getStatDate, date));
        List<Map<String, Object>> rows = trackEventMapper.selectMaps(new QueryWrapper<TrackEvent>()
                .select("event_code", "event_name", "COUNT(*) AS pv", "COUNT(DISTINCT user_id) AS uv")
                .ge("create_time", start).lt("create_time", end)
                .groupBy("event_code", "event_name"));
        for (Map<String, Object> row : rows) {
            StatEventDaily daily = new StatEventDaily();
            daily.setStatDate(date);
            daily.setEventCode((String) row.get("event_code"));
            daily.setEventName((String) row.get("event_name"));
            daily.setPv(((Number) row.get("pv")).intValue());
            daily.setUv(((Number) row.get("uv")).intValue());
            daily.setStatus(1);
            daily.setCreateBy(CommonConstant.CREATE_BY_SYSTEM);
            statEventDailyMapper.insert(daily);
        }
        log.info("stat aggregate done: date={}, dau={}, events={}", date, dau, rows.size());
    }
}
```

- [ ] **Step 5: 实现 StatAggregateTask**

```java
package cn.zhenxinjian.task;

import cn.zhenxinjian.service.impl.StatAggregateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 行为统计定时聚合任务（每日 01:00 统计昨日，与提醒推送/游客清理同模式）
 * 作者: wanglx
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatAggregateTask {

    private final StatAggregateService statAggregateService;

    /** 每日 01:00 聚合昨日数据 */
    @Scheduled(cron = "0 0 1 * * ?")
    public void aggregateYesterday() {
        try {
            statAggregateService.aggregate(LocalDate.now().minusDays(1));
        } catch (Exception e) {
            log.error("stat aggregate failed", e);
        }
    }
}
```

- [ ] **Step 6: 跑测试确认通过 + 编译**

Run: `cd apps/zhenxinjian-backend && mvn test -Dtest=StatAggregateServiceTest && mvn compile`
Expected: 2 测试 PASS，编译通过

- [ ] **Step 7: Commit**

```bash
git add apps/zhenxinjian-backend
git commit -m "feature：行为统计聚合服务与每日定时任务（幂等软删重插）"
```

---

### Task 5: 数据看板接口（AdminStatsController）

**Files:**
- Create: `apps/zhenxinjian-backend/src/main/java/cn/zhenxinjian/domain/vo/StatsOverviewVO.java`
- Create: `apps/zhenxinjian-backend/src/main/java/cn/zhenxinjian/domain/vo/DailyActiveVO.java`
- Create: `apps/zhenxinjian-backend/src/main/java/cn/zhenxinjian/domain/vo/EventRankVO.java`
- Create: `apps/zhenxinjian-backend/src/main/java/cn/zhenxinjian/domain/vo/PageRankVO.java`
- Create: `apps/zhenxinjian-backend/src/main/java/cn/zhenxinjian/service/impl/AdminStatsService.java`
- Create: `apps/zhenxinjian-backend/src/main/java/cn/zhenxinjian/controller/AdminStatsController.java`
- Test: `apps/zhenxinjian-backend/src/test/java/cn/zhenxinjian/service/impl/AdminStatsServiceTest.java`

**Interfaces:**
- Consumes: Task 3 `TrackEventMapper`；Task 4 `StatDailyActiveMapper`/`StatEventDailyMapper`/`StatAggregateService`；既有 `UserMapper`、`DietRecordMapper`
- Produces: `GET /admin/stats/overview|active-trend|event-rank|page-rank` + `POST /admin/stats/aggregate`（Task 9 前端看板依赖）

- [ ] **Step 1: 编写失败测试（口径校验）**

```java
package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.mapper.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AdminStatsServiceTest {

    @Mock private TrackEventMapper trackEventMapper;
    @Mock private StatDailyActiveMapper statDailyActiveMapper;
    @Mock private StatEventDailyMapper statEventDailyMapper;
    @Mock private UserMapper userMapper;
    @Mock private DietRecordMapper dietRecordMapper;
    @Mock private StatAggregateService statAggregateService;

    @InjectMocks
    private AdminStatsService adminStatsService;

    @Test
    void activeTrend_daysOutOfRange_throw40902() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminStatsService.activeTrend(0));
        assertEquals(CommonConstant.STATS_PARAM_INVALID_CODE, ex.getCode());
        assertThrows(BusinessException.class, () -> adminStatsService.activeTrend(91));
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `cd apps/zhenxinjian-backend && mvn test -Dtest=AdminStatsServiceTest`
Expected: 编译失败

- [ ] **Step 3: 实现 VO**

- `StatsOverviewVO`：`todayDau / yesterdayDau / mau / totalUsers / totalDietRecords`（全 Integer/Long）
- `DailyActiveVO`：`statDate(LocalDate) / dau / guestDau / newUser`
- `EventRankVO`：`eventCode / eventName / pv / uv`
- `PageRankVO`：`page / pv / uv`

- [ ] **Step 4: 实现 AdminStatsService**

方法清单（口径注释必备）：

```java
// overview()：今日 DAU 实时查 track_event（COUNT DISTINCT user_id，今日 00:00 起）；
//   昨日 DAU 查 stat_daily_active（无行=0）；MAU = 近 30 天 track_event COUNT(DISTINCT user_id)；
//   totalUsers = userMapper.selectCount(null)；totalDietRecords = dietRecordMapper.selectCount(null)
// activeTrend(int days)：days 限 1-90，越界抛 40902；查 stat_daily_active
//   WHERE stat_date BETWEEN today-days AND yesterday ORDER BY stat_date，转 DailyActiveVO
// eventRank(int days, int limit)：days 1-90、limit 1-50，越界 40902；
//   statEventDailyMapper.selectMaps：SELECT event_code,event_name,SUM(pv) pv,SUM(uv) uv
//   WHERE stat_date >= today-days GROUP BY event_code,event_name ORDER BY pv DESC LIMIT limit
// pageRank(int days, int limit)：同上校验；trackEventMapper.selectMaps：
//   SELECT page, COUNT(*) pv, COUNT(DISTINCT user_id) uv WHERE event_code='page_view'
//   AND page IS NOT NULL AND create_time >= today-days GROUP BY page ORDER BY pv DESC LIMIT limit
// rerunAggregate(LocalDate date)：date 不可为未来，越界 40902；委托 statAggregateService.aggregate(date)
```

类注释：

```java
/**
 * 管理端数据看板服务（今日实时查明细表，昨日及以前查聚合表）
 * 作者: wanglx
 */
```

- [ ] **Step 5: 实现 AdminStatsController**

```java
package cn.zhenxinjian.controller;

import cn.zhenxinjian.common.result.Result;
import cn.zhenxinjian.domain.vo.*;
import cn.zhenxinjian.service.impl.AdminStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 管理端数据看板控制器（仅管理员）
 * 作者: wanglx
 */
@Tag(name = "管理端-数据看板")
@RestController
@RequestMapping("/admin/stats")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "Bearer")
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    @Operation(summary = "总览卡片（今日 DAU 实时）")
    @GetMapping("/overview")
    public Result<StatsOverviewVO> overview() {
        return Result.ok(adminStatsService.overview());
    }

    @Operation(summary = "DAU 趋势（聚合表，默认 30 天）")
    @GetMapping("/active-trend")
    public Result<List<DailyActiveVO>> activeTrend(@RequestParam(defaultValue = "30") Integer days) {
        return Result.ok(adminStatsService.activeTrend(days));
    }

    @Operation(summary = "常用功能排行 TOP N")
    @GetMapping("/event-rank")
    public Result<List<EventRankVO>> eventRank(@RequestParam(defaultValue = "7") Integer days,
                                               @RequestParam(defaultValue = "10") Integer limit) {
        return Result.ok(adminStatsService.eventRank(days, limit));
    }

    @Operation(summary = "页面访问排行 TOP N")
    @GetMapping("/page-rank")
    public Result<List<PageRankVO>> pageRank(@RequestParam(defaultValue = "7") Integer days,
                                             @RequestParam(defaultValue = "10") Integer limit) {
        return Result.ok(adminStatsService.pageRank(days, limit));
    }

    @Operation(summary = "手工重跑某日聚合（补数，幂等）")
    @PostMapping("/aggregate")
    public Result<Void> aggregate(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        adminStatsService.rerunAggregate(date);
        return Result.ok();
    }
}
```

- [ ] **Step 6: 跑测试确认通过 + 编译**

Run: `cd apps/zhenxinjian-backend && mvn test -Dtest=AdminStatsServiceTest && mvn compile`
Expected: PASS + 编译通过

- [ ] **Step 7: Swagger 手工走查**

启动后端，用 ADMIN 账号 token 调 `GET /api/admin/stats/overview` 返回 200；用非 ADMIN token 调返回 403。

- [ ] **Step 8: Commit**

```bash
git add apps/zhenxinjian-backend
git commit -m "feature：管理端数据看板接口（overview/趋势/排行/手工聚合）"
```

---

### Task 6: 食物库维护接口（AdminFoodController）

**Files:**
- Create: `apps/zhenxinjian-backend/src/main/java/cn/zhenxinjian/domain/dto/AdminFoodSaveDTO.java`
- Create: `apps/zhenxinjian-backend/src/main/java/cn/zhenxinjian/domain/query/AdminFoodQuery.java`
- Create: `apps/zhenxinjian-backend/src/main/java/cn/zhenxinjian/service/impl/AdminFoodService.java`
- Create: `apps/zhenxinjian-backend/src/main/java/cn/zhenxinjian/controller/AdminFoodController.java`
- Test: `apps/zhenxinjian-backend/src/test/java/cn/zhenxinjian/service/impl/AdminFoodServiceTest.java`

**Interfaces:**
- Consumes: 既有 `FoodMapper`、`Food` PO、`FoodVO`、`PageQuery`；`FoodSourceEnum`（1内置 2自定义）；`FoodCategoryEnum`
- Produces: `GET/POST /admin/foods`、`PUT/DELETE /admin/foods/{id}`、`PUT /admin/foods/{id}/status`（Task 11 前端食物库页依赖）

- [ ] **Step 1: 编写失败测试**

```java
package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.domain.dto.AdminFoodSaveDTO;
import cn.zhenxinjian.domain.po.Food;
import cn.zhenxinjian.mapper.FoodMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminFoodServiceTest {

    @Mock private FoodMapper foodMapper;
    @InjectMocks private AdminFoodService adminFoodService;

    private AdminFoodSaveDTO validDto() {
        AdminFoodSaveDTO dto = new AdminFoodSaveDTO();
        dto.setName("鸡胸肉(测试)");
        dto.setCategoryCode("04");
        dto.setCategoryName("肉蛋水产");
        dto.setCarb(new BigDecimal("2.5"));
        dto.setProtein(new BigDecimal("24.6"));
        dto.setFat(new BigDecimal("1.9"));
        dto.setKcal(133);
        return dto;
    }

    @Test
    void create_duplicateName_throw40903() {
        when(foodMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminFoodService.create(validDto()));
        assertEquals(CommonConstant.ADMIN_FOOD_CONFLICT_CODE, ex.getCode());
    }

    @Test
    void create_kcalMismatch_throw40403() {
        AdminFoodSaveDTO dto = validDto();
        dto.setKcal(999);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminFoodService.create(dto));
        assertEquals(CommonConstant.FOOD_KCAL_MISMATCH_CODE, ex.getCode());
    }

    @Test
    void update_customFood_throw40404() {
        Food custom = new Food();
        custom.setId(1L);
        custom.setSource(2);
        when(foodMapper.selectById(1L)).thenReturn(custom);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminFoodService.update(1L, validDto()));
        assertEquals(CommonConstant.FOOD_NOT_FOUND_CODE, ex.getCode());
    }

    @Test
    void create_firstCustomCode_startF201() {
        when(foodMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(foodMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        adminFoodService.create(validDto());
        verify(foodMapper).insert(argThat((Food f) -> "F201".equals(f.getCode()) && f.getSource() == 1));
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `cd apps/zhenxinjian-backend && mvn test -Dtest=AdminFoodServiceTest`
Expected: 编译失败

- [ ] **Step 3: 实现 DTO/Query**

`AdminFoodSaveDTO`：`name @NotBlank @Size(max=64)`；`categoryCode @NotBlank`；`categoryName @NotBlank`；`alias @Size(max=128)`；`carb/protein/fat @NotNull @DecimalMin("0") @DecimalMax("100")`；`kcal @NotNull @Min(0) @Max(5000)`；`serving @DecimalMin("1")`（默认 100）。

`AdminFoodQuery extends PageQuery`：`keyword`（截 64）、`categoryCode`、`source`、`status`。

- [ ] **Step 4: 实现 AdminFoodService**

```java
/**
 * 管理端食物库维护服务（仅内置食物可写；自定义食物只读）
 * 作者: wanglx
 *
 * 口径：新增内置 code 从 F201 起顺延（查 max(code) 数值部分 +1，无内置则用 201）；
 * 宏量 0-100、热量与 4/4/9 换算偏差 ±10% 校验复用 C 端口径（40402/40403）；
 * 同名（内置活跃范围内）拒收 40903；编辑/删除/停用仅 source=1，否则 40404
 */
@Service
@RequiredArgsConstructor
public class AdminFoodService extends ServiceImpl<FoodMapper, Food> {

    /** 内置食物编号起始（F001-F200 为初始化数据） */
    private static final int BUILTIN_CODE_START = 201;
    /** 能量换算允许偏差比例 */
    private static final double KCAL_TOLERANCE = 0.10;

    // page(AdminFoodQuery)：LambdaQueryWrapper like name/alias、eq categoryCode/source/status，按 id 升序，转 FoodVO
    // create(AdminFoodSaveDTO)：校验同名(selectCount name=xx and source=1)>0 → 40903；
    //   校验宏量与热量（calcKcal = carb*4+protein*4+fat*9，|kcal-calcKcal|/calcKcal > 0.1 → 40403）；
    //   nextCode()：selectOne code LIKE 'F%' ORDER BY CAST(SUBSTRING(code,2) AS UNSIGNED) DESC LIMIT 1 → +1；
    //   组装 Food(source=1, status=1, createBy=当前管理员) insert
    // update(id, dto)：selectById 为空或 source!=1 → 40404；同名校验排除自身；校验通过后按字段更新
    // delete(id)：同上校验内置；removeById（逻辑删除）
    // changeStatus(id, status)：同上校验内置；仅允许 0/1；updateById
}
```

实现参考 CustomFoodService 中既有的宏量/热量校验代码（复制同款计算，保持 ±10% 口径一致）。

- [ ] **Step 5: 实现 AdminFoodController**

```java
/**
 * 管理端食物库维护控制器（仅管理员；自定义食物只读）
 * 作者: wanglx
 */
@Tag(name = "管理端-食物库维护")
@RestController
@RequestMapping("/admin/foods")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "Bearer")
public class AdminFoodController {
    // GET    /admin/foods           → page(AdminFoodQuery)
    // POST   /admin/foods           → create(@Valid @RequestBody AdminFoodSaveDTO)
    // PUT    /admin/foods/{id}      → update(id, @Valid @RequestBody AdminFoodSaveDTO)
    // DELETE /admin/foods/{id}      → delete(id)
    // PUT    /admin/foods/{id}/status?status=0|1 → changeStatus
}
```

- [ ] **Step 6: 跑测试确认通过 + 编译**

Run: `cd apps/zhenxinjian-backend && mvn test -Dtest=AdminFoodServiceTest && mvn compile`
Expected: 4 测试 PASS + 编译通过

- [ ] **Step 7: Commit**

```bash
git add apps/zhenxinjian-backend
git commit -m "feature：管理端食物库维护接口（内置食物增删改停用）"
```

---

### Task 7: 饮食记录只读 + 用户聚合视图接口

**Files:**
- Create: `apps/zhenxinjian-backend/src/main/java/cn/zhenxinjian/domain/query/AdminDietRecordQuery.java`
- Create: `apps/zhenxinjian-backend/src/main/java/cn/zhenxinjian/domain/vo/AdminDietRecordVO.java`
- Create: `apps/zhenxinjian-backend/src/main/java/cn/zhenxinjian/domain/vo/AdminUserProfileVO.java`
- Create: `apps/zhenxinjian-backend/src/main/java/cn/zhenxinjian/service/impl/AdminDietService.java`
- Create: `apps/zhenxinjian-backend/src/main/java/cn/zhenxinjian/controller/AdminDietController.java`
- Create: `apps/zhenxinjian-backend/src/main/java/cn/zhenxinjian/controller/AdminUserController.java`

**Interfaces:**
- Consumes: 既有 `DietRecordMapper`/`DietRecord`（含食物快照冗余列）、`UserMapper`、`UserBodyMapper`、`CarbCyclePlanMapper`
- Produces: `GET /admin/diet-records`（只读分页）；`GET /admin/users/{id}/profile`（Task 10/12 前端依赖）

- [ ] **Step 1: 实现 Query/VO**

`AdminDietRecordQuery extends PageQuery`：`userKeyword`（ID 精确或昵称模糊）、`startDate`/`endDate`（LocalDate）、`mealType`（1早2午3晚4加餐）。

`AdminDietRecordVO`：`id / userId / userNickname / recordDate / mealType / foodName / amount / carb / protein / fat / kcal / source / createTime`（字段名以 DietRecord PO 实际快照列为准映射）。

`AdminUserProfileVO`：`user(UserVO) / bodyProfile(BodyProfileVO 或 null) / currentMode(Integer 或 null) / currentPlan(CyclePlanVO 或 null)`。

- [ ] **Step 2: 实现 AdminDietService**

```java
/**
 * 管理端饮食记录查看服务（只读：不开放改/删用户业务数据）
 * 作者: wanglx
 */
@Service
@RequiredArgsConstructor
public class AdminDietService {

    private final DietRecordMapper dietRecordMapper;
    private final UserMapper userMapper;

    // page(AdminDietRecordQuery)：
    // 1) userKeyword 非空：纯数字按 id 精确，否则 users 昵称模糊取 id 集；无命中 → 返回空 Page
    // 2) dietRecordMapper 分页：in userIds（若有）、ge/le recordDate、eq mealType、按 recordDate desc+id desc
    // 3) 批量查涉及用户昵称组装 AdminDietRecordVO（用户已删显示「用户#id」）
}
```

- [ ] **Step 3: 实现 AdminDietController**

```java
@Tag(name = "管理端-饮食记录查看")
@RestController
@RequestMapping("/admin/diet-records")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "Bearer")
public class AdminDietController {
    // GET /admin/diet-records → page(@ParameterObject AdminDietRecordQuery)
}
```

- [ ] **Step 4: 实现 AdminUserController**

```java
@Tag(name = "管理端-用户聚合视图")
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "Bearer")
public class AdminUserController {
    // GET /admin/users/{id}/profile：
    //   user = userService.getUserById(id)（复用，已带 ADMIN 鉴权逻辑）
    //   bodyProfile = userBodyMapper 查活跃档案（无则 null）
    //   currentPlan = carbCyclePlanMapper 查用户最新进行中周期（无则 null）
    //   currentMode：档案 dietMode 快照（无档案则 null）
}
```

- [ ] **Step 5: 编译 + Swagger 走查**

Run: `cd apps/zhenxinjian-backend && mvn compile`
走查：`GET /api/admin/diet-records?startDate=2026-09-01&endDate=2026-09-11` 返回分页；`GET /api/admin/users/1/profile` 返回聚合；非 ADMIN 403。

- [ ] **Step 6: Commit**

```bash
git add apps/zhenxinjian-backend
git commit -m "feature：管理端饮食记录只读查看与用户聚合视图接口"
```

---

### Task 8: 小程序埋点采集（track.ts + 全面接入）

**Files:**
- Create: `apps/zhenxinjian-uniapp/src/api/track.ts`
- Create: `apps/zhenxinjian-uniapp/src/utils/track.ts`
- Modify: `apps/zhenxinjian-uniapp/src/App.vue`（onLaunch 启动 flush 定时器）
- Modify: 16 个页面 onShow 接入 `trackPage`（见 Step 3 清单）
- Modify: 行为事件接入点（见 Step 4 清单）

**Interfaces:**
- Consumes: Task 3 的 `POST /api/track/events`；既有 `api/request.ts`（http 封装）；`utils/storage.ts`
- Produces: `track(eventCode: string, extra?: Record<string, unknown>)`、`trackPage(pagePath: string)`、`flushTrackQueue()`、`startTrackFlushTimer()`

- [ ] **Step 1: 实现 api/track.ts 与 utils/track.ts**

`api/track.ts`：

```typescript
/**
 * 埋点上报 API
 * 作者: wanglx
 */
import http from './request'

export interface TrackEventItem {
  eventCode: string
  page?: string
  extra?: Record<string, unknown>
  clientTime?: string
}

/** 批量上报埋点事件（单批≤50） */
export function reportEvents(events: TrackEventItem[]) {
  return http.post<void>('/track/events', { events })
}
```

`utils/track.ts`：

```typescript
/**
 * 埋点采集（内存队列 + storage 持久化；10s/20条 flush；失败保留重试；队列上限 200）
 * 作者: wanglx
 *
 * 口径：clientTime 仅记录不参与统计（后端以 create_time 为准）；
 * 事件码须与后端 TrackEventEnum 一致，非法码后端整批拒收 40901
 */
import { reportEvents, type TrackEventItem } from '@/api/track'

const QUEUE_KEY = 'zxj_track_queue'
const QUEUE_MAX = 200
const FLUSH_SIZE = 20
const FLUSH_INTERVAL = 10_000

let queue: TrackEventItem[] = []
let timer: ReturnType<typeof setInterval> | null = null

function loadQueue() {
  const raw = uni.getStorageSync(QUEUE_KEY)
  queue = Array.isArray(raw) ? raw.slice(0, QUEUE_MAX) : []
}

function persistQueue() {
  uni.setStorageSync(QUEUE_KEY, queue)
}

function now() {
  const d = new Date()
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

/** 上报一个埋点事件（入队，由 flush 批量发送） */
export function track(eventCode: string, extra?: Record<string, unknown>, page?: string) {
  if (queue.length === 0) loadQueue()
  if (queue.length >= QUEUE_MAX) return
  queue.push({ eventCode, page, extra, clientTime: now() })
  persistQueue()
  if (queue.length >= FLUSH_SIZE) {
    flushTrackQueue()
  }
}

/** 页面访问埋点 */
export function trackPage(pagePath: string) {
  track('page_view', undefined, pagePath)
}

/** 批量 flush（每次取队首 ≤50 条上报；失败保留队列下次重试） */
export function flushTrackQueue() {
  if (queue.length === 0) loadQueue()
  if (queue.length === 0) return
  const batch = queue.slice(0, 50)
  reportEvents(batch)
    .then(() => {
      queue = queue.slice(batch.length)
      persistQueue()
    })
    .catch(() => undefined)
}

/** 启动定时 flush（App onLaunch 调一次） */
export function startTrackFlushTimer() {
  if (timer) return
  timer = setInterval(flushTrackQueue, FLUSH_INTERVAL)
}
```

`App.vue` 的 `onLaunch` 中追加一行：`startTrackFlushTimer()`（import 自 `@/utils/track`）。

- [ ] **Step 2: 验证类型通过**

Run: `cd apps/zhenxinjian-uniapp && npx vue-tsc --noEmit`
Expected: 无新增类型错误

- [ ] **Step 3: 16 页 onShow 接入 trackPage**

以下页面在 `onShow`（无 onShow 则新增）中追加 `trackPage('<页面路径>')`（import 自 `@/utils/track`）：

| 文件 | 路径参数 |
|---|---|
| `pages/auth/guide.vue` | `pages/auth/guide`（已有 onShow，追加调用） |
| `pages/auth/expire.vue` | `pages/auth/expire` |
| `pages/body/profile.vue` | `pages/body/profile` |
| `pages/body/result.vue` | `pages/body/result` |
| `pages/cycle/plan.vue` | `pages/cycle/plan` |
| `pages/cycle/setting.vue` | `pages/cycle/setting` |
| `pages/food/custom-edit.vue` | `pages/food/custom-edit` |
| `pages/food/custom-list.vue` | `pages/food/custom-list` |
| `pages/food/detail.vue` | `pages/food/detail` |
| `pages/food/index.vue` | `pages/food/index` |
| `pages/home/index.vue` | `pages/home/index`（已有 onShow，追加） |
| `pages/mine/index.vue` | `pages/mine/index`（已有 onShow，追加） |
| `pages/mode/select.vue` | `pages/mode/select` |
| `pages/record/add.vue` | `pages/record/add` |
| `pages/record/index.vue` | `pages/record/index` |
| `pages/reminder/index.vue` | `pages/reminder/index`（已有 onShow，追加） |
| `pages/taper/plan.vue` | `pages/taper/plan` |
| `pages/weight/index.vue` | `pages/weight/index` |
| `pages/menstrual/index.vue` | `pages/menstrual/index` |

- [ ] **Step 4: 行为事件接入**

| 文件 | 事件 | 接入点（函数级锚点） |
|---|---|---|
| `pages/auth/guide.vue` | `login_wechat` | `handleWechatLogin` 中 `loginByWechat` 成功后 |
| `pages/auth/guide.vue` | `login_fail` | `handleWechatLogin` catch 分支 |
| `pages/auth/guide.vue` | `login_guest` | `handleGuest` 中 `loginAsGuest` 成功后 |
| `pages/record/add.vue` | `record_add` | 提交成功分支，extra `{mealType, source}` |
| `pages/record/index.vue` | `record_edit` / `record_delete` | 编辑保存/删除成功分支，extra `{mealType}` |
| `pages/food/index.vue` | `food_search` | 实际发起搜索处，extra `{keyword}` |
| `pages/food/index.vue` | `food_hot_click` | 热门食物点击处，extra `{foodId}` |
| `pages/food/index.vue` | `food_history_click` | 历史搜索点击处，extra `{keyword}` |
| `pages/food/detail.vue` | `food_detail` | onLoad 详情加载成功，extra `{foodId}` |
| `pages/food/custom-edit.vue` | `food_custom_add` | 自定义保存成功分支 |
| `pages/mode/select.vue` | `mode_select` / `mode_switch` | 首次选定/切换确认成功，extra `{mode}` |
| `pages/cycle/plan.vue` / `pages/taper/plan.vue` | `plan_view` | onShow 数据加载成功后，extra `{mode}` |
| `pages/reminder/index.vue` | `reminder_save` | PUT 保存成功分支 |
| `pages/reminder/index.vue` | `reminder_subscribe` | reportSubscribe 上报成功分支 |
| `pages/body/profile.vue` | `body_save` | 档案保存成功分支 |
| `pages/weight/index.vue` | `weight_add` | 体重保存成功分支 |
| `pages/menstrual/index.vue` | `menstrual_save` | 经期保存成功分支 |
| `pages/home/index.vue` | `home_quick_entry` | 快捷入口点击处，extra `{target}` |

每处均为一行 `track('xxx', {...})`，埋点失败不阻塞业务（track.ts 内部已 catch）。

- [ ] **Step 5: 构建验证**

Run: `cd apps/zhenxinjian-uniapp && npm run build:mp-weixin`
Expected: 构建通过

- [ ] **Step 6: Commit**

```bash
git add apps/zhenxinjian-uniapp
git commit -m "feature：小程序埋点采集（track 队列上报 + 19页PV + 21类行为事件接入）"
```

---

### Task 9: 管理端数据看板页

**Files:**
- Create: `apps/zhenxinjian-front/src/api/stats.ts`
- Create: `apps/zhenxinjian-front/src/view/dashboard/index.vue`
- Modify: `apps/zhenxinjian-front/src/router/index.ts:28-44`（children 调整：`/home` 重定向到 `/dashboard`，注册 dashboard/foods/diet-records，移除 websocket）
- Modify: `apps/zhenxinjian-front/src/layout/MainLayout.vue:26-71`（导航改为 看板/用户管理/食物库/饮食记录，移除 WebSocket 入口）

**Interfaces:**
- Consumes: Task 5 的 4 个 GET 接口；既有 `api/request.ts`；echarts ^5.5.0（package.json 已有依赖）
- Produces: 路由 `/dashboard`；导航菜单四项

- [ ] **Step 1: 实现 api/stats.ts**

```typescript
/**
 * 数据看板 API
 * 作者: wanglx
 */
import request from './request'

export interface StatsOverview {
  todayDau: number
  yesterdayDau: number
  mau: number
  totalUsers: number
  totalDietRecords: number
}

export interface DailyActive {
  statDate: string
  dau: number
  guestDau: number
  newUser: number
}

export interface EventRank {
  eventCode: string
  eventName: string
  pv: number
  uv: number
}

export interface PageRank {
  page: string
  pv: number
  uv: number
}

export function getOverview() {
  return request.get<StatsOverview>('/admin/stats/overview')
}

export function getActiveTrend(days = 30) {
  return request.get<DailyActive[]>('/admin/stats/active-trend', { params: { days } })
}

export function getEventRank(days = 7, limit = 10) {
  return request.get<EventRank[]>('/admin/stats/event-rank', { params: { days, limit } })
}

export function getPageRank(days = 7, limit = 10) {
  return request.get<PageRank[]>('/admin/stats/page-rank', { params: { days, limit } })
}
```

- [ ] **Step 2: 实现 view/dashboard/index.vue**

结构（风格复用 users 页卡片样式变量）：

```vue
<script setup lang="ts">
/**
 * 数据看板：总览卡片 + DAU 趋势（ECharts）+ 功能/页面 TOP10
 * 作者: wanglx
 */
import { onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { getActiveTrend, getEventRank, getOverview, getPageRank } from '@/api/stats'
import type { DailyActive, EventRank, PageRank, StatsOverview } from '@/api/stats'

const overview = ref<StatsOverview | null>(null)
const trendDays = ref(30)
const trendData = ref<DailyActive[]>([])
const eventRank = ref<EventRank[]>([])
const pageRank = ref<PageRank[]>([])
const trendRef = ref<HTMLElement>()
let chart: echarts.ECharts | null = null

async function loadOverview() {
  overview.value = await getOverview()
}

async function loadTrend() {
  trendData.value = await getActiveTrend(trendDays.value)
  renderTrend()
}

function renderTrend() {
  if (!trendRef.value) return
  if (!chart) chart = echarts.init(trendRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    xAxis: { type: 'category', data: trendData.value.map((d) => d.statDate.slice(5)) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      { name: 'DAU', type: 'line', smooth: true, data: trendData.value.map((d) => d.dau) },
      { name: '游客', type: 'line', smooth: true, data: trendData.value.map((d) => d.guestDau) }
    ]
  })
}

async function loadRanks() {
  eventRank.value = await getEventRank(7, 10)
  pageRank.value = await getPageRank(7, 10)
}

function handleResize() {
  chart?.resize()
}

onMounted(() => {
  loadOverview()
  loadTrend()
  loadRanks()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
})
</script>
```

模板：4 张 `el-card`（今日 DAU 卡右上角 `el-tag size="small" type="warning"` 文案"实时"；昨日 DAU / 近30天 MAU / 累计用户+累计记录）；`el-radio-group v-model="trendDays"`（7/30）`@change="loadTrend"` 切换 + `<div ref="trendRef" style="height:320px" />`；右侧两个面板：功能 TOP10 用横向柱状（可再建一个 echarts 容器，xAxis value/yAxis category inverse，series bar data = eventRank.map(pv)）或直接 `el-table`（列 事件名/PV/UV）；页面 TOP10 用 `el-table`（列 页面/PV/UV）。

- [ ] **Step 3: 路由与布局调整**

`router/index.ts` children 改为：

```typescript
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/view/dashboard/index.vue'),
          meta: { admin: true }
        },
        {
          path: 'users',
          name: 'Users',
          component: () => import('@/view/users/index.vue'),
          meta: { admin: true }
        },
        {
          path: 'foods',
          name: 'Foods',
          component: () => import('@/view/foods/index.vue'),
          meta: { admin: true }
        },
        {
          path: 'diet-records',
          name: 'DietRecords',
          component: () => import('@/view/diet-records/index.vue'),
          meta: { admin: true }
        }
      ]
```

根 redirect 由 `/home` 改为 `/dashboard`；移除 `/register` 与 `/websocket` 路由；删除 `view/home/index.vue`、`view/websocket/index.vue`、`view/register/index.vue` 引用（文件删除在 Task 12 统一执行，本任务先改路由）。

`MainLayout.vue`：删除 `goWebSocket` 与对应 `<a>`；导航改为 dashboard/users/foods/diet-records 四项（`goHome` 改跳 `/dashboard`，文案"数据看板"；新增 `goFoods`、`goDietRecords`）；检查 `view/login/index.vue` 中 `/register` 链接并移除（如有）。

- [ ] **Step 4: 构建验证**

Run: `cd apps/zhenxinjian-front && npm run build`
Expected: vue-tsc + vite build 通过

- [ ] **Step 5: Commit**

```bash
git add apps/zhenxinjian-front
git commit -m "feature：管理端数据看板页（总览卡片 + DAU趋势 + 功能/页面TOP10）"
```

---

### Task 10: 用户管理页改造（详情抽屉）

**Files:**
- Create: `apps/zhenxinjian-front/src/api/adminUser.ts`
- Modify: `apps/zhenxinjian-front/src/view/users/index.vue:212-237`（操作列加"详情"，加抽屉）

**Interfaces:**
- Consumes: Task 7 的 `GET /admin/users/{id}/profile`
- Produces: `getUserProfile(id)`；用户页详情抽屉

- [ ] **Step 1: 实现 api/adminUser.ts**

```typescript
/**
 * 管理端用户聚合视图 API
 * 作者: wanglx
 */
import request from './request'
import type { UserInfo } from './types'

export interface UserProfile {
  user: UserInfo
  bodyProfile: Record<string, unknown> | null
  currentMode: number | null
  currentPlan: Record<string, unknown> | null
}

/** 用户聚合视图（基础信息 + 身体档案 + 当前模式/周期） */
export function getUserProfile(id: number) {
  return request.get<UserProfile>(`/admin/users/${id}/profile`)
}
```

- [ ] **Step 2: 用户页接入详情抽屉**

`view/users/index.vue`：
- script 追加：`import { getUserProfile } from '@/api/adminUser'`；`const drawerVisible = ref(false)`；`const profile = ref<UserProfile | null>(null)`；`async function openDetail(row: UserInfo) { profile.value = await getUserProfile(row.id); drawerVisible.value = true }`
- 操作列在"编辑"前加：`<el-button link type="primary" @click="openDetail(row)">详情</el-button>`
- 模板末尾加 `el-drawer v-model="drawerVisible" title="用户详情" size="420px"`：内用 `el-descriptions :column="1" border` 展示 user 字段（ID/用户名/昵称/类型/状态/注册时间/最后登录）；`el-divider` 后展示 bodyProfile 关键字段（性别/年龄/身高/当前体重/目标体重/活动系数，为 null 显示"未建档"）；再展示 currentMode（1=532 2=碳循环）与 currentPlan（有则显示周期天数与状态）。

- [ ] **Step 3: 构建验证**

Run: `cd apps/zhenxinjian-front && npm run build`
Expected: 通过

- [ ] **Step 4: Commit**

```bash
git add apps/zhenxinjian-front
git commit -m "feature：用户管理页改造（聚合详情抽屉）"
```

---

### Task 11: 食物库维护页

**Files:**
- Create: `apps/zhenxinjian-front/src/api/adminFood.ts`
- Create: `apps/zhenxinjian-front/src/view/foods/index.vue`

**Interfaces:**
- Consumes: Task 6 的 5 个接口；既有 `FoodCategoryEnum` 10 分类（前端用静态常量写死 01-10，与后端 `GET /food/categories` 顺序一致）
- Produces: 路由 `/foods` 页面

- [ ] **Step 1: 实现 api/adminFood.ts**

```typescript
/**
 * 管理端食物库维护 API
 * 作者: wanglx
 */
import request from './request'

export interface AdminFood {
  id: number
  code: string
  categoryCode: string
  categoryName: string
  name: string
  alias: string | null
  carb: number
  protein: number
  fat: number
  kcal: number
  serving: number
  source: number
  status: number
}

export interface AdminFoodSavePayload {
  name: string
  categoryCode: string
  categoryName: string
  alias?: string
  carb: number
  protein: number
  fat: number
  kcal: number
  serving?: number
}

export interface AdminFoodQuery {
  page: number
  size: number
  keyword?: string
  categoryCode?: string
  source?: number
  status?: number
}

export function getFoodPage(params: AdminFoodQuery) {
  return request.get<{ records: AdminFood[]; total: number }>('/admin/foods', { params })
}

export function addFood(data: AdminFoodSavePayload) {
  return request.post<void>('/admin/foods', data)
}

export function updateFood(id: number, data: AdminFoodSavePayload) {
  return request.put<void>(`/admin/foods/${id}`, data)
}

export function deleteFood(id: number) {
  return request.delete<void>(`/admin/foods/${id}`)
}

export function changeFoodStatus(id: number, status: number) {
  return request.put<void>(`/admin/foods/${id}/status`, null, { params: { status } })
}
```

- [ ] **Step 2: 实现 view/foods/index.vue**

结构（整体复用 users 页模式：toolbar 筛选 + el-table + el-pagination + el-dialog 表单）：

```vue
<script setup lang="ts">
/**
 * 食物库维护页（内置可写，自定义只读）
 * 作者: wanglx
 */
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { addFood, changeFoodStatus, deleteFood, getFoodPage, updateFood } from '@/api/adminFood'
import type { AdminFood } from '@/api/adminFood'

/** 10 大分类（与后端 FoodCategoryEnum 顺序一致） */
const CATEGORIES = [
  { code: '01', name: '谷薯类' }, { code: '02', name: '蔬菜类' },
  { code: '03', name: '水果类' }, { code: '04', name: '肉蛋水产' },
  { code: '05', name: '奶类' }, { code: '06', name: '豆制品' },
  { code: '07', name: '坚果类' }, { code: '08', name: '油脂类' },
  { code: '09', name: '调味零食' }, { code: '10', name: '饮品类' }
]
// 注：分类名称以后端 FoodCategoryEnum 实际字典为准，实现时打开该枚举核对后抄写

const loading = ref(false)
const tableData = ref<AdminFood[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 20, keyword: '', categoryCode: '', source: undefined as number | undefined, status: undefined as number | undefined })

const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  id: undefined as number | undefined,
  name: '', categoryCode: '', alias: '',
  carb: 0, protein: 0, fat: 0, kcal: 0, serving: 100
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入食物名称', trigger: 'blur' }],
  categoryCode: [{ required: true, message: '请选择分类', trigger: 'change' }],
  carb: [{ required: true, message: '请输入碳水', trigger: 'blur' }],
  protein: [{ required: true, message: '请输入蛋白质', trigger: 'blur' }],
  fat: [{ required: true, message: '请输入脂肪', trigger: 'blur' }],
  kcal: [{ required: true, message: '请输入热量', trigger: 'blur' }]
}

async function loadFoods() {
  loading.value = true
  try {
    const page = await getFoodPage({
      page: query.page, size: query.size,
      keyword: query.keyword || undefined,
      categoryCode: query.categoryCode || undefined,
      source: query.source, status: query.status
    })
    tableData.value = page.records || []
    total.value = page.total || 0
  } finally {
    loading.value = false
  }
}

function categoryName(code: string) {
  return CATEGORIES.find((c) => c.code === code)?.name || ''
}

function openCreate() { /* reset form → dialogVisible = true */ }
function openEdit(row: AdminFood) { /* 仅内置(source===1)可进入；逐字段回填 → dialogVisible = true */ }

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const payload = {
      name: form.name, categoryCode: form.categoryCode,
      categoryName: categoryName(form.categoryCode), alias: form.alias || undefined,
      carb: form.carb, protein: form.protein, fat: form.fat,
      kcal: form.kcal, serving: form.serving
    }
    if (form.id) {
      await updateFood(form.id, payload)
      ElMessage.success('修改成功')
    } else {
      await addFood(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    await loadFoods()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: AdminFood) {
  try {
    await ElMessageBox.confirm(`确认删除食物「${row.name}」？历史饮食记录快照不受影响`, '提示', { type: 'warning' })
  } catch { return }
  await deleteFood(row.id)
  ElMessage.success('删除成功')
  await loadFoods()
}

async function handleToggleStatus(row: AdminFood) {
  await changeFoodStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success(row.status === 1 ? '已停用' : '已启用')
  await loadFoods()
}

onMounted(loadFoods)
</script>
```

模板要点：
- 筛选区：keyword 输入框（名称/别名）、分类 el-select、来源 el-select（内置/自定义）、状态 el-select（有效/停用）、查询按钮、新增内置食物按钮
- 表格列：code（自定义显示"-"）/ name / categoryName / carb/protein/fat（每100g）/ kcal / source（el-tag 内置=success 自定义=info）/ status（el-tag）
- 操作列（width 220）：`编辑`（`:disabled="row.source !== 1"`）、`停用/启用`（内置可点，文案随 status）、`删除`（`:disabled="row.source !== 1"` type=danger）
- 弹窗：name 输入、分类 el-select（@change 无额外逻辑，categoryName 提交时取）、alias 输入、carb/protein/fat `el-input-number :precision="1" :min="0" :max="100"`、kcal `el-input-number :precision="0" :min="0" :max="5000"`、serving `el-input-number :precision="0" :min="1"`（默认 100）

- [ ] **Step 3: 构建验证**

Run: `cd apps/zhenxinjian-front && npm run build`
Expected: 通过

- [ ] **Step 4: Commit**

```bash
git add apps/zhenxinjian-front
git commit -m "feature：食物库维护页（内置增删改停用，自定义只读）"
```

---

### Task 12: 饮食记录查看页 + 演示残留清理

**Files:**
- Create: `apps/zhenxinjian-front/src/api/adminDiet.ts`
- Create: `apps/zhenxinjian-front/src/view/diet-records/index.vue`
- Delete: `apps/zhenxinjian-front/src/view/home/index.vue`
- Delete: `apps/zhenxinjian-front/src/view/websocket/index.vue`
- Delete: `apps/zhenxinjian-front/src/view/register/index.vue`
- Delete: `apps/zhenxinjian-front/src/component/DemoChart.vue`
- Delete: `apps/zhenxinjian-front/src/component/DemoWebSocket.vue`
- Delete: `apps/zhenxinjian-front/src/component/WsStatusCard.vue`
- Delete: `apps/zhenxinjian-front/src/utils/ws.ts`（如仅被 websocket 页引用，先 grep 确认）

**Interfaces:**
- Consumes: Task 7 的 `GET /admin/diet-records`
- Produces: 路由 `/diet-records` 页面（只读）

- [ ] **Step 1: 确认删除安全**

Run: `cd apps/zhenxinjian-front && grep -rn "DemoChart\|DemoWebSocket\|WsStatusCard\|utils/ws\|view/home\|view/register\|view/websocket" src --include=*.vue --include=*.ts`
Expected: 除待删文件自身与已在 Task 9 清理的路由/布局外，无其他引用；login 页若有 register 链接在此步一并移除

- [ ] **Step 2: 实现 api/adminDiet.ts**

```typescript
/**
 * 管理端饮食记录查看 API（只读）
 * 作者: wanglx
 */
import request from './request'

export interface AdminDietRecord {
  id: number
  userId: number
  userNickname: string
  recordDate: string
  mealType: number
  foodName: string
  amount: number
  carb: number
  protein: number
  fat: number
  kcal: number
  source: number
  createTime: string
}

export function getDietRecordPage(params: {
  page: number
  size: number
  userKeyword?: string
  startDate?: string
  endDate?: string
  mealType?: number
}) {
  return request.get<{ records: AdminDietRecord[]; total: number }>('/admin/diet-records', { params })
}
```

- [ ] **Step 3: 实现 view/diet-records/index.vue**

复用 users 页骨架：
- 筛选区：userKeyword 输入（用户ID/昵称）、日期范围 `el-date-picker type="daterange" value-format="YYYY-MM-DD"`、餐别 el-select（早/午/晚/加餐）、查询按钮
- 表格列：userNickname（空显示 `用户#userId`）/ recordDate / mealType（映射 早/午/晚/加餐 el-tag）/ foodName / amount（g）/ carb / protein / fat / kcal / source（食物库/手动）/ createTime
- **无操作列**；分页同 users 页

- [ ] **Step 4: 删除演示残留文件**

`git rm` Step 1 确认的 7 个文件（ws.ts 若无引用一并删）。

- [ ] **Step 5: 构建验证**

Run: `cd apps/zhenxinjian-front && npm run build`
Expected: vue-tsc 无未解析引用，构建通过

- [ ] **Step 6: Commit**

```bash
git add apps/zhenxinjian-front
git commit -m "feature：饮食记录查看页（只读）并清理脚手架演示残留"
```

---

### Task 13: 联调验收与归档

**Files:**
- Modify: `openspec/changes/admin-analytics/tasks.md`（勾掉已完成项）

**Interfaces:**
- Consumes: Task 1-12 全部产物
- Produces: `openspec validate admin-analytics --strict` 通过；归档 commit

- [ ] **Step 1: 埋点闭环验收**

微信开发者工具启动小程序（dev 后端）：进入 P01 → 游客进入 → 浏览首页/食物库/记录页 → 添加一条记录 → 查库：

```sql
SELECT event_code, user_type, page FROM track_event ORDER BY id DESC LIMIT 20;
```

Expected: `login_guest`、多页 `page_view`、`record_add` 均有记录，user_type=GUEST，page 正确；队列重试验证：断网点 3 个页面 → 恢复网络 → 10s 内事件补发成功

- [ ] **Step 2: 聚合与看板验收**

```bash
curl -X POST "http://localhost:8080/api/admin/stats/aggregate?date=$(date -d yesterday +%F)" -H "Authorization: Bearer <ADMIN_TOKEN>"
```

Expected：返回 200；`stat_daily_active`/`stat_event_daily` 出现昨日行；重复执行两次行数不翻倍（幂等）；看板页 overview/trend/rank 展示与手工 SQL `SELECT COUNT(DISTINCT user_id) ...` 核对一致

- [ ] **Step 3: 食物库维护验收**

新增内置食物（code 自动 F201）→ 列表可见 → 编辑宏量 → 停用 → 小程序食物库搜索不可见 → 启用 → 恢复可见；同名新增被拒 40903；自定义食物行编辑/删除按钮置灰

- [ ] **Step 4: 饮食记录与用户详情验收**

饮食记录页按用户昵称/日期/餐别筛选结果正确；用户页详情抽屉展示档案与当前模式；非 ADMIN 账号登录后台访问 `/dashboard` 被路由守卫拦回，直调接口 403

- [ ] **Step 5: 全量构建与校验**

Run:
```bash
cd apps/zhenxinjian-backend && mvn compile
cd apps/zhenxinjian-front && npm run build
cd apps/zhenxinjian-uniapp && npm run build:mp-weixin
openspec validate admin-analytics --strict
```
Expected: 全部通过

- [ ] **Step 6: 勾选 tasks.md 并归档**

`openspec/changes/admin-analytics/tasks.md` 勾掉全部完成项 → `openspec archive admin-analytics`（同步主 specs）→ 提交：

```bash
git add openspec
git commit -m "docs(openspec): 完成 admin-analytics 变更并归档，同步主 specs"
```

---

## Self-Review 记录

- **Spec coverage**：设计文档 §2 数据模型→Task 2；§3 埋点链路→Task 3/8；§4 管理端接口→Task 5/6/7；§4.5 聚合任务→Task 4；§5 前端页面→Task 9/10/11/12；§6 错误处理与测试→Task 3-7 各含测试，验收→Task 13。openspec 双循环→Task 1/13。无缺口。
- **Placeholder scan**：Task 7/9/11 个别模板为要点描述（Vue 模板冗长），但 script、接口签名、校验规则、列结构均已给出，无 TBD/TODO。
- **Type consistency**：`TrackEventEnum.of(String)`、`saveBatch(Long, String, TrackEventBatchDTO)`、`aggregate(LocalDate)`、`rerunAggregate(LocalDate)` 前后一致；前端 `AdminFoodSavePayload` 与后端 `AdminFoodSaveDTO` 字段一致；`UserProfile` 与 `AdminUserProfileVO` 一致。
