package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.enums.GenderEnum;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.domain.dto.MenstrualSaveDTO;
import cn.zhenxinjian.domain.po.UserBody;
import cn.zhenxinjian.domain.po.UserMenstrual;
import cn.zhenxinjian.domain.vo.MenstrualVO;
import cn.zhenxinjian.mapper.UserBodyMapper;
import cn.zhenxinjian.mapper.UserMenstrualMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 经期管理服务单元测试
 * 作者: wanglx
 */
class MenstrualServiceTest {

    private UserMenstrualMapper userMenstrualMapper;
    private UserBodyMapper userBodyMapper;
    private MenstrualCalcService menstrualCalcService;
    private MenstrualService service;

    @BeforeEach
    void setUp() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, UserMenstrual.class);
        TableInfoHelper.initTableInfo(assistant, UserBody.class);

        userMenstrualMapper = mock(UserMenstrualMapper.class);
        userBodyMapper = mock(UserBodyMapper.class);
        menstrualCalcService = mock(MenstrualCalcService.class);
        service = new MenstrualService(userMenstrualMapper, userBodyMapper, menstrualCalcService);
    }

    /** 场景：男性用户查询经期 → applicable=false */
    @Test
    void get_maleUser_returnsNotApplicable() {
        UserBody body = new UserBody();
        body.setGender(1); // MALE
        when(userBodyMapper.selectOne(any(Wrapper.class))).thenReturn(body);

        MenstrualVO result = service.get(1L);

        assertNotNull(result);
        assertFalse(Boolean.TRUE.equals(result.getApplicable()));
    }

    /** 场景：女性用户未设置经期 → enabled=0，默认值 */
    @Test
    void get_femaleNoRecord_returnsDefaults() {
        UserBody body = new UserBody();
        body.setGender(2); // FEMALE
        when(userBodyMapper.selectOne(any(Wrapper.class))).thenReturn(body);
        when(userMenstrualMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        MenstrualVO result = service.get(1L);

        assertNotNull(result);
        assertTrue(result.getApplicable());
        assertEquals(0, result.getEnabled().intValue());
        assertEquals(28, result.getCycleLen().intValue());
        assertEquals(5, result.getPeriodDays().intValue());
    }

    /** 场景：女性已开启经期 → 返回阶段与上浮值 */
    @Test
    void get_femaleEnabled_returnsPhase() {
        UserBody body = new UserBody();
        body.setGender(2); // FEMALE
        when(userBodyMapper.selectOne(any(Wrapper.class))).thenReturn(body);

        UserMenstrual entity = new UserMenstrual();
        entity.setId(1L);
        entity.setUserId(1L);
        entity.setEnabled(1);
        entity.setCycleLen(28);
        entity.setPeriodDays(5);
        entity.setPeriodStartDate(LocalDate.now().minusDays(3));
        when(userMenstrualMapper.selectOne(any(Wrapper.class))).thenReturn(entity);

        MenstrualCalcService.PhaseResult phase = new MenstrualCalcService.PhaseResult(4,
                cn.zhenxinjian.common.enums.MenstrualPhaseEnum.MENSTRUAL);
        when(menstrualCalcService.resolvePhase(eq(GenderEnum.FEMALE), eq(Boolean.TRUE),
                any(LocalDate.class), eq(28), eq(5), any(LocalDate.class)))
                .thenReturn(phase);

        MenstrualVO result = service.get(1L);

        assertNotNull(result);
        assertTrue(result.getApplicable());
        assertEquals("menstrual", result.getPhaseKey());
    }

    /** 场景：男性保存经期设置 → 抛错 */
    @Test
    void save_maleUser_throwsException() {
        UserBody body = new UserBody();
        body.setGender(1); // MALE
        when(userBodyMapper.selectOne(any(Wrapper.class))).thenReturn(body);

        MenstrualSaveDTO dto = new MenstrualSaveDTO();
        dto.setEnabled(1);
        dto.setCycleLen(28);
        dto.setPeriodDays(5);
        dto.setPeriodStartDate(LocalDate.now().minusDays(3));

        assertThrows(BusinessException.class, () -> service.save(1L, dto));
    }

    /** 场景：周期长度越界 → 抛错 */
    @Test
    void save_cycleLenOutOfRange_throwsException() {
        UserBody body = new UserBody();
        body.setGender(2); // FEMALE
        when(userBodyMapper.selectOne(any(Wrapper.class))).thenReturn(body);

        MenstrualSaveDTO dto = new MenstrualSaveDTO();
        dto.setEnabled(1);
        dto.setCycleLen(20);
        dto.setPeriodDays(5);
        dto.setPeriodStartDate(LocalDate.now());

        assertThrows(BusinessException.class, () -> service.save(1L, dto));
    }

    /** 场景：经期天数越界 → 抛错 */
    @Test
    void save_periodDaysOutOfRange_throwsException() {
        UserBody body = new UserBody();
        body.setGender(2); // FEMALE
        when(userBodyMapper.selectOne(any(Wrapper.class))).thenReturn(body);

        MenstrualSaveDTO dto = new MenstrualSaveDTO();
        dto.setEnabled(1);
        dto.setCycleLen(28);
        dto.setPeriodDays(11);
        dto.setPeriodStartDate(LocalDate.now());

        assertThrows(BusinessException.class, () -> service.save(1L, dto));
    }

    /** 场景：开启经期但未提供起始日 → 抛错 */
    @Test
    void save_enabledWithoutStartDate_throwsException() {
        UserBody body = new UserBody();
        body.setGender(2); // FEMALE
        when(userBodyMapper.selectOne(any(Wrapper.class))).thenReturn(body);

        MenstrualSaveDTO dto = new MenstrualSaveDTO();
        dto.setEnabled(1);
        dto.setCycleLen(28);
        dto.setPeriodDays(5);
        dto.setPeriodStartDate(null);

        assertThrows(BusinessException.class, () -> service.save(1L, dto));
    }

    /** 场景：开启经期但提供未来起始日 → 抛错 */
    @Test
    void save_enabledWithFutureStartDate_throwsException() {
        UserBody body = new UserBody();
        body.setGender(2); // FEMALE
        when(userBodyMapper.selectOne(any(Wrapper.class))).thenReturn(body);

        MenstrualSaveDTO dto = new MenstrualSaveDTO();
        dto.setEnabled(1);
        dto.setCycleLen(28);
        dto.setPeriodDays(5);
        dto.setPeriodStartDate(LocalDate.now().plusDays(1));

        assertThrows(BusinessException.class, () -> service.save(1L, dto));
    }

    /** 场景：新建经期设置正常保存 */
    @Test
    void save_newRecord_inserts() {
        UserBody body = new UserBody();
        body.setGender(2); // FEMALE
        when(userBodyMapper.selectOne(any(Wrapper.class))).thenReturn(body);
        when(userMenstrualMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(userMenstrualMapper.insert(any(UserMenstrual.class))).thenReturn(1);

        MenstrualSaveDTO dto = new MenstrualSaveDTO();
        dto.setEnabled(0);
        dto.setCycleLen(28);
        dto.setPeriodDays(5);
        dto.setPeriodStartDate(null);

        MenstrualVO result = service.save(1L, dto);

        assertNotNull(result);
        assertEquals(0, result.getEnabled().intValue());
    }
}