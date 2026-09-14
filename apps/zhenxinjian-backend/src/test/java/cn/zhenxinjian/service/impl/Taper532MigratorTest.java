package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.domain.po.AdjustLog;
import cn.zhenxinjian.domain.po.UserMenstrual;
import cn.zhenxinjian.domain.po.WeightRecord;
import cn.zhenxinjian.mapper.AdjustLogMapper;
import cn.zhenxinjian.mapper.UserMenstrualMapper;
import cn.zhenxinjian.mapper.WeightRecordMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 经期/体重/调碳 游客迁移器单元测试
 * 作者: wanglx
 */
class Taper532MigratorTest {

    private UserMenstrualMapper userMenstrualMapper;
    private WeightRecordMapper weightRecordMapper;
    private AdjustLogMapper adjustLogMapper;
    private Taper532Migrator migrator;

    @BeforeEach
    void setUp() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, UserMenstrual.class);
        TableInfoHelper.initTableInfo(assistant, WeightRecord.class);
        TableInfoHelper.initTableInfo(assistant, AdjustLog.class);

        userMenstrualMapper = mock(UserMenstrualMapper.class);
        weightRecordMapper = mock(WeightRecordMapper.class);
        adjustLogMapper = mock(AdjustLogMapper.class);
        migrator = new Taper532Migrator(userMenstrualMapper, weightRecordMapper, adjustLogMapper);
    }

    /** 场景：正式用户无经期设置 → 游客设置迁移归属 */
    @Test
    void migrate_formalNone_movesGuestMenstrual() {
        UserMenstrual guest = new UserMenstrual();
        guest.setId(1L);
        guest.setUserId(10L);

        when(userMenstrualMapper.selectOne(any(Wrapper.class)))
                .thenReturn(guest).thenReturn(null);
        when(userMenstrualMapper.update(any(), any(Wrapper.class))).thenReturn(1);
        when(weightRecordMapper.update(any(), any(Wrapper.class))).thenReturn(1);
        when(adjustLogMapper.update(any(), any(Wrapper.class))).thenReturn(1);

        migrator.migrate(10L, 20L);

        verify(userMenstrualMapper).update(any(), any(Wrapper.class));
        verify(weightRecordMapper).update(any(), any(Wrapper.class));
        verify(adjustLogMapper).update(any(), any(Wrapper.class));
    }

    /** 场景：正式用户已有经期设置 → 游客设置软删降级 */
    @Test
    void migrate_formalHas_fallsbackToDelete() {
        UserMenstrual guest = new UserMenstrual();
        guest.setId(1L);
        guest.setUserId(10L);
        UserMenstrual formal = new UserMenstrual();
        formal.setId(2L);
        formal.setUserId(20L);

        when(userMenstrualMapper.selectOne(any(Wrapper.class)))
                .thenReturn(guest).thenReturn(formal);
        when(userMenstrualMapper.deleteById(1L)).thenReturn(1);
        when(weightRecordMapper.update(any(), any(Wrapper.class))).thenReturn(1);
        when(adjustLogMapper.update(any(), any(Wrapper.class))).thenReturn(1);

        migrator.migrate(10L, 20L);

        verify(userMenstrualMapper).deleteById(1L);
    }

    /** 场景：过期游客清理 → 三表逻辑删除 */
    @Test
    void purge_deletesAllThree() {
        when(userMenstrualMapper.delete(any(Wrapper.class))).thenReturn(1);
        when(weightRecordMapper.delete(any(Wrapper.class))).thenReturn(1);
        when(adjustLogMapper.delete(any(Wrapper.class))).thenReturn(1);

        migrator.purge(10L);

        verify(userMenstrualMapper).delete(any(Wrapper.class));
        verify(weightRecordMapper).delete(any(Wrapper.class));
        verify(adjustLogMapper).delete(any(Wrapper.class));
    }
}