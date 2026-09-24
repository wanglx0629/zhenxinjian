package cn.zhenxinjian.task;

import cn.zhenxinjian.domain.po.Food;
import cn.zhenxinjian.mapper.FoodMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TFDA 基础食物初始化器单元测试（幂等跳过 / 首次全量导入 / 部分导入冲突跳过 / 条数异常拒绝启动）
 * 作者: wanglx
 */
class TfdaFoodInitializerTest {

    private FoodMapper foodMapper;
    private TfdaFoodInitializer initializer;
    /** 真源 foods_tfda.json 声明总数（随真源版本浮动，测试内动态读取） */
    private int total;

    @BeforeEach
    void setUp() throws Exception {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Food.class);
        foodMapper = mock(FoodMapper.class);
        ObjectMapper objectMapper = new ObjectMapper();
        try (InputStream in = new ClassPathResource("foods_tfda.json").getInputStream()) {
            total = objectMapper.readTree(in).get("total").asInt();
        }
        initializer = new TfdaFoodInitializer(foodMapper, objectMapper);
    }

    /** 场景：库中已有本批次全量 TFDA 食物 → 跳过导入，不发生任何插入 */
    @Test
    void run_alreadyImported_skip() throws Exception {
        when(foodMapper.selectCount(any(Wrapper.class))).thenReturn((long) total);

        initializer.run(null);

        verify(foodMapper, never()).insert(any(Food.class));
    }

    /** 场景：空库首次启动 → 全量导入，字段口径完整（来源/免审/批次/kJ/审计标识） */
    @Test
    void run_emptyDb_importsAllWithFields() throws Exception {
        when(foodMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        List<Food> inserted = new ArrayList<>();
        when(foodMapper.insert(any(Food.class))).thenAnswer(inv -> {
            inserted.add(inv.getArgument(0));
            return 1;
        });

        initializer.run(null);

        assertEquals(total, inserted.size());
        // 抽样对拍真源：A05002 稉米平均值（碳水 77.8 / 蛋白 7.0 / 脂肪 0.7 / 352kcal / 1473kJ / 18 类口径 02 谷物类）
        Food rice = inserted.stream().filter(f -> "A05002".equals(f.getCode())).findFirst().orElseThrow();
        assertEquals("稉米平均值", rice.getName());
        assertEquals("02", rice.getCategoryCode());
        assertEquals("谷物类", rice.getCategoryName());
        assertEquals("77.8", rice.getCarb().toPlainString());
        assertEquals("7.0", rice.getProtein().toPlainString());
        assertEquals("0.7", rice.getFat().toPlainString());
        assertEquals(352, rice.getKcal());
        assertEquals(1473, rice.getKj());
        assertEquals("100", rice.getServing().toPlainString());
        assertEquals("TFDA:20.5", rice.getDataBatch());
        assertEquals(1, rice.getSource());
        assertEquals(3, rice.getAuditStatus());
        assertEquals("tfda-import", rice.getCreateBy());
    }

    /** 场景：部分导入后重启 → 已有编号按唯一约束冲突跳过，不覆盖不复活不报错 */
    @Test
    void run_partialImport_skipsDuplicates() throws Exception {
        when(foodMapper.selectCount(any(Wrapper.class))).thenReturn(50L);
        when(foodMapper.insert(any(Food.class)))
                .thenThrow(new DuplicateKeyException("dup"))
                .thenReturn(1);

        initializer.run(null);

        verify(foodMapper, times(total)).insert(any(Food.class));
    }

    /** 场景：导入文件声明总数与实际条目不符 → 拒绝启动 */
    @Test
    void run_wrongCount_refuseStartup() throws Exception {
        when(foodMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        ObjectMapper badMapper = mock(ObjectMapper.class);
        TfdaFoodInitializer.TfdaLibraryFile file = new TfdaFoodInitializer.TfdaLibraryFile();
        file.setTotal(99);
        file.setFoods(new ArrayList<>());
        when(badMapper.readValue(any(java.io.InputStream.class), any(Class.class))).thenReturn(file);
        TfdaFoodInitializer badInitializer = new TfdaFoodInitializer(foodMapper, badMapper);

        assertThrows(IllegalStateException.class, () -> badInitializer.run(null));
        verify(foodMapper, never()).insert(any(Food.class));
    }
}
