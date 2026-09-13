package cn.zhenxinjian.task;

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
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;

/**
 * 内置食物库初始化器（启动时幂等导入 foods_200.json，200 条《中国食物成分表》第 6 版数据）
 * 作者: wanglx
 *
 * 口径: docsFile/projectFile/04-食物库数据字典.md（每 100g 可食部、干重不泡发、生熟分列）
 * 幂等: 内置食物数 ≥200 跳过；逐条插入并按 code 唯一约束捕获冲突（仅补缺，不覆盖）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FoodLibraryInitializer implements ApplicationRunner {

    /** 内置食物目标条数（不足则拒绝启动，防止半量数据上线） */
    public static final int BUILT_IN_TOTAL = 200;

    /** 导入数据资源（后端导入唯一真源，来源 MRD-PRD/foods_200.json） */
    private static final String IMPORT_RESOURCE = "foods_200.json";

    /** 导入操作者标识（审计列） */
    private static final String CREATE_BY_IMPORT = "food-import";

    private final FoodMapper foodMapper;

    private final ObjectMapper objectMapper;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Long builtInCount = foodMapper.selectCount(
                Wrappers.<Food>lambdaQuery().eq(Food::getSource, FoodSourceEnum.BUILT_IN.getCode()));
        if (builtInCount != null && builtInCount >= BUILT_IN_TOTAL) {
            log.info("[FoodLibrary] 内置食物已就绪（{}条），跳过导入", builtInCount);
            return;
        }
        FoodLibraryFile file = loadImportFile();
        if (file.getFoods() == null || file.getFoods().size() != BUILT_IN_TOTAL) {
            throw new IllegalStateException("[FoodLibrary] 导入数据条数异常（期望" + BUILT_IN_TOTAL
                    + "，实际" + (file.getFoods() == null ? 0 : file.getFoods().size()) + "），拒绝启动");
        }
        int inserted = 0;
        int skipped = 0;
        for (FoodEntry entry : file.getFoods()) {
            try {
                foodMapper.insert(toFood(entry));
                inserted++;
            } catch (DuplicateKeyException e) {
                // 已存在同编号（重复初始化/多实例并发）：跳过不覆盖
                skipped++;
            }
        }
        log.info("[FoodLibrary] 内置食物导入完成：新增{}条，跳过{}条（冲突已存在）", inserted, skipped);
    }

    /** 解析 classpath 导入文件 */
    private FoodLibraryFile loadImportFile() throws Exception {
        try (InputStream in = new ClassPathResource(IMPORT_RESOURCE).getInputStream()) {
            return objectMapper.readValue(in, FoodLibraryFile.class);
        }
    }

    /** 导入条目 → 食物实体（分类编号/名称拆分并与字典枚举校验一致） */
    private Food toFood(FoodEntry entry) {
        String categoryCode = entry.getCategory().substring(0, 2);
        String categoryName = entry.getCategory().substring(3).trim();
        FoodCategoryEnum category = FoodCategoryEnum.of(categoryCode);
        if (category == null || !category.getDesc().equals(categoryName)) {
            throw new IllegalStateException("[FoodLibrary] 分类与字典不一致: " + entry.getCategory());
        }
        Food food = new Food();
        food.setCode(entry.getId());
        food.setCategoryCode(categoryCode);
        food.setCategoryName(categoryName);
        food.setName(entry.getName());
        food.setAlias(entry.getAlias() == null ? "" : entry.getAlias());
        food.setCarb(entry.getCarb());
        food.setProtein(entry.getProtein());
        food.setFat(entry.getFat());
        food.setKcal(entry.getKcal());
        food.setServing(entry.getServing() == null ? new BigDecimal("100") : entry.getServing());
        food.setSource(FoodSourceEnum.BUILT_IN.getCode());
        food.setCreateBy(CREATE_BY_IMPORT);
        return food;
    }

    /** 导入文件外层结构（与 foods_200.json 真源对齐） */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FoodLibraryFile {
        /** 数据来源说明 */
        private String source;
        /** 口径说明（每100g可食部） */
        private String basis;
        /** 声明总条数 */
        private Integer total;
        /** 食物条目 */
        private List<FoodEntry> foods;
    }

    /** 单条食物导入数据 */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FoodEntry {
        /** 食物编号 F001–F200 */
        private String id;
        /** 分类（「编号 名称」，如「01 谷薯杂豆·主食」） */
        private String category;
        /** 食物名称 */
        private String name;
        /** 别名（可空） */
        private String alias;
        /** 碳水 g/100g */
        private BigDecimal carb;
        /** 蛋白 g/100g */
        private BigDecimal protein;
        /** 脂肪 g/100g */
        private BigDecimal fat;
        /** 能量 kcal/100g */
        private Integer kcal;
        /** 常用单份克数 */
        private BigDecimal serving;
    }
}
