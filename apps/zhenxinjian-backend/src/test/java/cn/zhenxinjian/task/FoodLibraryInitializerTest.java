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
import org.springframework.dao.DuplicateKeyException;

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
 * 内置食物库初始化器单元测试（幂等跳过 / 首次全量导入 / 条数异常拒绝启动 / 冲突跳过）
 * 作者: wanglx
 */
class FoodLibraryInitializerTest {

    private FoodMapper foodMapper;
    private FoodLibraryInitializer initializer;

    @BeforeEach
    void setUp() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Food.class);
        foodMapper = mock(FoodMapper.class);
        initializer = new FoodLibraryInitializer(foodMapper, new ObjectMapper());
    }

    /** 场景：库中已有 200 条内置食物 → 跳过导入，不发生任何插入 */
    @Test
    void run_alreadyImported_skip() throws Exception {
        when(foodMapper.selectCount(any(Wrapper.class))).thenReturn(200L);

        initializer.run(null);

        verify(foodMapper, never()).insert(any(Food.class));
    }

    /** 场景：空库首次启动 → 全量导入 200 条（真源 foods_200.json 恰为 200 条） */
    @Test
    void run_emptyDb_importsAll200() throws Exception {
        when(foodMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

        initializer.run(null);

        verify(foodMapper, times(FoodLibraryInitializer.BUILT_IN_TOTAL)).insert(any(Food.class));
    }

    /** 场景：部分导入后重启 → 已有编号按唯一约束冲突跳过，不覆盖不报错 */
    @Test
    void run_partialImport_skipsDuplicates() throws Exception {
        when(foodMapper.selectCount(any(Wrapper.class))).thenReturn(50L);
        when(foodMapper.insert(any(Food.class)))
                .thenThrow(new DuplicateKeyException("dup"))
                .thenReturn(1);

        initializer.run(null);

        verify(foodMapper, times(FoodLibraryInitializer.BUILT_IN_TOTAL)).insert(any(Food.class));
    }

    /** 场景：导入文件条数 ≠200 → 拒绝启动 */
    @Test
    void run_wrongCount_refuseStartup() throws Exception {
        when(foodMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        ObjectMapper badMapper = mock(ObjectMapper.class);
        FoodLibraryInitializer.FoodLibraryFile file = new FoodLibraryInitializer.FoodLibraryFile();
        List<FoodLibraryInitializer.FoodEntry> entries = new ArrayList<>();
        FoodLibraryInitializer.FoodEntry entry = new FoodLibraryInitializer.FoodEntry();
        entry.setId("F001");
        entries.add(entry);
        file.setFoods(entries);
        when(badMapper.readValue(any(java.io.InputStream.class), any(Class.class))).thenReturn(file);
        FoodLibraryInitializer badInitializer = new FoodLibraryInitializer(foodMapper, badMapper);

        assertThrows(IllegalStateException.class, () -> badInitializer.run(null));
        verify(foodMapper, never()).insert(any(Food.class));
    }

    /** 抽样对拍：真源 F001 大米碳水 77.9 / F002 米饭碳水 25.9 */
    @Test
    void importSource_sampleValuesMatch() throws Exception {
        when(foodMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        List<Food> inserted = new ArrayList<>();
        when(foodMapper.insert(any(Food.class))).thenAnswer(inv -> {
            inserted.add(inv.getArgument(0));
            return 1;
        });

        initializer.run(null);

        assertEquals(200, inserted.size());
        Food f001 = inserted.stream().filter(f -> "F001".equals(f.getCode())).findFirst().orElseThrow();
        Food f002 = inserted.stream().filter(f -> "F002".equals(f.getCode())).findFirst().orElseThrow();
        assertEquals("77.9", f001.getCarb().toPlainString());
        assertEquals("25.9", f002.getCarb().toPlainString());
    }
}
