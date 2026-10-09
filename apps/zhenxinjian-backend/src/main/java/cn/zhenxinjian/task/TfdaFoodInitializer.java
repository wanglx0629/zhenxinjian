package cn.zhenxinjian.task;

import cn.zhenxinjian.common.enums.FoodAuditStatusEnum;
import cn.zhenxinjian.common.enums.FoodCategoryEnum;
import cn.zhenxinjian.common.enums.FoodSourceEnum;
import cn.zhenxinjian.domain.po.Food;
import cn.zhenxinjian.mapper.FoodMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;

/**
 * TFDA 基础食物初始化器（启动时幂等导入 foods_tfda.json，台湾卫福部食药署开放数据 OGL 须署名）
 * 作者: wanglx
 *
 * 口径: 每条为每 100g 可食部（kJ=kcal×4.184 换算）；繁简/去重/分类映射/营养校验在离线清洗脚本完成
 *      （python/tfda_clean.py，真源 data-import/tfda/raw/，溯源见 raw/SOURCE.json）
 * 幂等: 按数据批次（data_batch=TFDA:版本）计数判就绪跳过；逐条插入按 code 唯一约束捕获冲突
 *      （仅补缺，不覆盖人工修正，软删条目不复活）
 * 顺序: 后于内置 200 导入（@Order(2)）——内置就绪检查按 source=1 计数，TFDA 先导入会干扰其判定
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class TfdaFoodInitializer implements ApplicationRunner {

    /** 导入数据资源（后端导入唯一真源，来源 data-import/tfda/clean/foods_tfda.json） */
    private static final String IMPORT_RESOURCE = "foods_tfda.json";

    /** 导入操作者标识（审计列） */
    private static final String CREATE_BY_IMPORT = "tfda-import";

    private final FoodMapper foodMapper;

    private final ObjectMapper objectMapper;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        TfdaLibraryFile file = loadImportFile();
        if (file.getFoods() == null || file.getFoods().size() != file.getTotal()) {
            throw new IllegalStateException("[TfdaLibrary] 导入数据条数异常（声明" + file.getTotal()
                    + "，实际" + (file.getFoods() == null ? 0 : file.getFoods().size()) + "），拒绝启动");
        }
        Long imported = foodMapper.selectCount(
                Wrappers.<Food>lambdaQuery().eq(Food::getDataBatch, file.getDataBatch()));
        if (imported != null && imported >= file.getFoods().size()) {
            log.info("[TfdaLibrary] TFDA 基础食物已就绪（{}条，批次{}），跳过导入", imported, file.getDataBatch());
            return;
        }
        int inserted = 0;
        int skipped = 0;
        for (TfdaEntry entry : file.getFoods()) {
            try {
                foodMapper.insert(toFood(entry));
                inserted++;
            } catch (DuplicateKeyException e) {
                // 已存在同编号（重复初始化/软删后复跑/多实例并发）：跳过不覆盖不复活
                skipped++;
            }
        }
        log.info("[TfdaLibrary] TFDA 基础食物导入完成：批次{} 新增{}条，跳过{}条（冲突已存在）",
                file.getDataBatch(), inserted, skipped);
    }

    /** 解析 classpath 导入文件 */
    private TfdaLibraryFile loadImportFile() throws Exception {
        try (InputStream in = new ClassPathResource(IMPORT_RESOURCE).getInputStream()) {
            return objectMapper.readValue(in, TfdaLibraryFile.class);
        }
    }

    /** 导入条目 → 食物实体（分类编号/名称拆分并与字典枚举校验一致；无图走前端分类占位） */
    private Food toFood(TfdaEntry entry) {
        String categoryCode = entry.getCategory().substring(0, 2);
        String categoryName = entry.getCategory().substring(3).trim();
        FoodCategoryEnum category = FoodCategoryEnum.of(categoryCode)
                .filter(c -> c.getDesc().equals(categoryName))
                .orElseThrow(() -> new IllegalStateException("[TfdaLibrary] 分类与字典不一致: " + entry.getCategory()));
        Food food = new Food();
        food.setCode(entry.getCode());
        food.setCategoryCode(categoryCode);
        food.setCategoryName(categoryName);
        food.setName(entry.getName());
        food.setAlias(entry.getAlias() == null ? "" : entry.getAlias());
        food.setCarb(entry.getCarb());
        food.setProtein(entry.getProtein());
        food.setFat(entry.getFat());
        food.setKcal(entry.getKcal());
        food.setKj(entry.getKj());
        food.setServing(entry.getServing() == null ? new BigDecimal("100") : entry.getServing());
        food.setSource(FoodSourceEnum.BUILT_IN.getCode());
        food.setAuditStatus(FoodAuditStatusEnum.NOT_REQUIRED.getCode());
        food.setDataBatch(entry.getDataBatch());
        food.setCreateBy(CREATE_BY_IMPORT);
        return food;
    }

    /** 导入文件外层结构（与 foods_tfda.json 真源对齐） */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TfdaLibraryFile {
        /** 数据来源与授权说明（OGL 署名） */
        private String source;
        /** 口径说明（每100g可食部；kJ换算） */
        private String basis;
        /** TFDA 官方数据版本 */
        private String version;
        /** 数据批次（TFDA:版本） */
        private String dataBatch;
        /** 声明总条数 */
        private Integer total;
        /** 食物条目 */
        private List<TfdaEntry> foods;
    }

    /** 单条食物导入数据 */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TfdaEntry {
        /** TFDA 整合编号（6–8 位） */
        private String code;
        /** 分类（「编号 名称」，如「01 谷薯杂豆·主食」） */
        private String category;
        /** 食物名称（简体） */
        private String name;
        /** 别名（顿号分隔，可空） */
        private String alias;
        /** 碳水 g/100g */
        private BigDecimal carb;
        /** 蛋白 g/100g */
        private BigDecimal protein;
        /** 脂肪 g/100g */
        private BigDecimal fat;
        /** 能量 kcal/100g */
        private Integer kcal;
        /** 能量 kJ/100g（kcal×4.184 取整） */
        private Integer kj;
        /** 常用单份克数 */
        private BigDecimal serving;
        /** 数据批次（TFDA:版本） */
        private String dataBatch;
    }
}
