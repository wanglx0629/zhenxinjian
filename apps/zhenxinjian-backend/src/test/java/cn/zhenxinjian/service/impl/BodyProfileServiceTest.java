package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.enums.ActivityLevelEnum;
import cn.zhenxinjian.common.enums.DeficitOptionEnum;
import cn.zhenxinjian.common.enums.GenderEnum;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.domain.dto.BodyProfileSaveDTO;
import cn.zhenxinjian.domain.po.UserBody;
import cn.zhenxinjian.domain.po.UserBodyHistory;
import cn.zhenxinjian.domain.vo.BodyProfileVO;
import cn.zhenxinjian.mapper.UserBodyHistoryMapper;
import cn.zhenxinjian.mapper.UserBodyMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 身体档案服务单元测试
 * 作者: wanglx
 */
class BodyProfileServiceTest {

    private UserBodyMapper userBodyMapper;
    private UserBodyHistoryMapper userBodyHistoryMapper;
    private BodyCalcService bodyCalcService;
    private BodyProfileService service;

    @BeforeEach
    void setUp() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, UserBody.class);
        TableInfoHelper.initTableInfo(assistant, UserBodyHistory.class);

        userBodyMapper = mock(UserBodyMapper.class);
        userBodyHistoryMapper = mock(UserBodyHistoryMapper.class);
        bodyCalcService = mock(BodyCalcService.class);
        service = new BodyProfileService(userBodyMapper, userBodyHistoryMapper, bodyCalcService);
    }

    /** 场景：目标体重 > 当前体重+0.1 → 抛错 */
    @Test
    void save_targetWeightExceedsCurrentWeight_throwsException() {
        BodyProfileSaveDTO dto = validDto();
        dto.setWeight(55.0);
        dto.setTargetWeight(60.0);

        when(bodyCalcService.resolveDeficit(200)).thenReturn(DeficitOptionEnum.GENTLE);

        assertThrows(BusinessException.class, () -> service.save(1L, dto));
    }

    /** 场景：目标体重 ≤ 当前体重 → 通过交叉校验 */
    @Test
    void save_targetWeightLteCurrentWeight_passesValidation() {
        BodyProfileSaveDTO dto = validDto();
        UserBody existing = new UserBody();
        existing.setId(100L);
        existing.setUserId(1L);
        existing.setGender(1);

        when(bodyCalcService.resolveDeficit(200)).thenReturn(DeficitOptionEnum.GENTLE);
        when(bodyCalcService.compute(any(GenderEnum.class), anyInt(), anyDouble(),
                anyDouble(), anyDouble(), anyInt())).thenReturn(calcResult());
        when(userBodyMapper.selectOne(any(Wrapper.class))).thenReturn(existing);
        when(userBodyMapper.updateById(any(UserBody.class))).thenReturn(1);
        when(userBodyHistoryMapper.insert(any(UserBodyHistory.class))).thenReturn(1);
        when(bodyCalcService.isLowKcalRisk(any(GenderEnum.class), anyInt())).thenReturn(false);

        BodyProfileVO result = service.save(1L, dto);

        assertNotNull(result);
        assertTrue(result.getRecorded());
        verify(userBodyHistoryMapper).insert(any(UserBodyHistory.class));
        verify(userBodyMapper).updateById(any(UserBody.class));
    }

    /** 场景：首次保存 → 新建档案（insert） */
    @Test
    void save_firstTime_insertsNewRecord() {
        BodyProfileSaveDTO dto = validDto();
        UserBody insertedBody = new UserBody();
        insertedBody.setId(10L);
        insertedBody.setUserId(1L);
        insertedBody.setGender(1);
        insertedBody.setWeight(57.0);
        insertedBody.setTargetKcal(1600);
        insertedBody.setTargetCarb(180.0);
        insertedBody.setTargetProtein(90.0);
        insertedBody.setTargetFat(45.0);

        when(bodyCalcService.resolveDeficit(200)).thenReturn(DeficitOptionEnum.GENTLE);
        when(bodyCalcService.compute(any(GenderEnum.class), anyInt(), anyDouble(),
                anyDouble(), anyDouble(), anyInt())).thenReturn(calcResult());
        when(userBodyMapper.selectOne(any(Wrapper.class))).thenReturn(null).thenReturn(insertedBody);
        when(userBodyMapper.insert(any(UserBody.class))).thenReturn(1);
        when(bodyCalcService.isLowKcalRisk(any(GenderEnum.class), anyInt())).thenReturn(false);

        BodyProfileVO result = service.save(1L, dto);

        assertNotNull(result);
        assertTrue(result.getRecorded());
        verify(userBodyMapper).insert(any(UserBody.class));
    }

    /** 场景：并发首次录入 DuplicateKeyException → 转为覆盖已有行 */
    @Test
    void save_concurrentDuplicateKey_fallsBackToUpdate() {
        BodyProfileSaveDTO dto = validDto();
        UserBody concurrent = new UserBody();
        concurrent.setId(200L);
        concurrent.setUserId(1L);
        concurrent.setGender(1);

        when(bodyCalcService.resolveDeficit(200)).thenReturn(DeficitOptionEnum.GENTLE);
        when(bodyCalcService.compute(any(GenderEnum.class), anyInt(), anyDouble(),
                anyDouble(), anyDouble(), anyInt())).thenReturn(calcResult());
        when(userBodyMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(userBodyMapper.insert(any(UserBody.class))).thenThrow(new DuplicateKeyException("dup"));
        // 并发后再次 select 返回已有行
        when(userBodyMapper.selectOne(any(Wrapper.class)))
                .thenReturn(null).thenReturn(concurrent);
        when(userBodyMapper.updateById(any(UserBody.class))).thenReturn(1);
        when(userBodyHistoryMapper.insert(any(UserBodyHistory.class))).thenReturn(1);
        when(bodyCalcService.isLowKcalRisk(any(GenderEnum.class), anyInt())).thenReturn(false);

        BodyProfileVO result = service.save(1L, dto);

        assertNotNull(result);
        assertTrue(result.getRecorded());
        verify(userBodyMapper).updateById(any(UserBody.class));
    }

    /** 场景：脂肪系数非法值 → 抛错 */
    @Test
    void save_invalidCfc_throwsException() {
        BodyProfileSaveDTO dto = validDto();
        dto.setCfc(0.5);

        when(bodyCalcService.resolveDeficit(200)).thenReturn(DeficitOptionEnum.GENTLE);

        assertThrows(BusinessException.class, () -> service.save(1L, dto));
    }

    /** 场景：缺口档位非法 → 抛错 */
    @Test
    void save_invalidDeficit_throwsException() {
        BodyProfileSaveDTO dto = validDto();

        when(bodyCalcService.resolveDeficit(200)).thenReturn(null);

        assertThrows(BusinessException.class, () -> service.save(1L, dto));
    }

    /** 场景：未录入档案 → recorded=false，含免责声明 */
    @Test
    void getCurrent_noRecord_returnsEmptyWithDisclaimer() {
        when(userBodyMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        BodyProfileVO result = service.getCurrent(1L);

        assertNotNull(result);
        assertFalse(result.getRecorded());
        assertNotNull(result.getDisclaimer());
    }

    /** 场景：已有档案 → recorded=true，含低热量风险标记 */
    @Test
    void getCurrent_hasRecord_returnsProfileWithRiskFlag() {
        UserBody ub = new UserBody();
        ub.setId(1L);
        ub.setUserId(1L);
        ub.setGender(1);
        ub.setWeight(57.0);
        ub.setTargetKcal(1200);
        ub.setTargetCarb(150.0);
        ub.setTargetProtein(80.0);
        ub.setTargetFat(40.0);

        when(userBodyMapper.selectOne(any(Wrapper.class))).thenReturn(ub);
        when(bodyCalcService.isLowKcalRisk(GenderEnum.of(1), 1200)).thenReturn(true);

        BodyProfileVO result = service.getCurrent(1L);

        assertNotNull(result);
        assertTrue(result.getRecorded());
        assertTrue(result.getLowKcalRisk());
    }

    /** 场景：cfs 缺省时使用默认值 0.8 */
    @Test
    void save_cfcNull_usesDefault08() {
        BodyProfileSaveDTO dto = validDto();
        dto.setCfc(null);
        UserBody existing = new UserBody();
        existing.setId(100L);
        existing.setUserId(1L);
        existing.setGender(1);

        when(bodyCalcService.resolveDeficit(200)).thenReturn(DeficitOptionEnum.GENTLE);
        when(bodyCalcService.compute(any(GenderEnum.class), anyInt(), anyDouble(),
                anyDouble(), anyDouble(), anyInt())).thenReturn(calcResult());
        when(userBodyMapper.selectOne(any(Wrapper.class))).thenReturn(existing);
        when(userBodyMapper.updateById(any(UserBody.class))).thenReturn(1);
        when(userBodyHistoryMapper.insert(any(UserBodyHistory.class))).thenReturn(1);
        when(bodyCalcService.isLowKcalRisk(any(GenderEnum.class), anyInt())).thenReturn(false);

        BodyProfileVO result = service.save(1L, dto);
        assertTrue(result.getRecorded());
    }

    private BodyProfileSaveDTO validDto() {
        BodyProfileSaveDTO dto = new BodyProfileSaveDTO();
        dto.setWeight(57.0);
        dto.setTargetWeight(55.0);
        dto.setDeficit(200);
        dto.setCfc(0.8);
        dto.setGender(1);
        dto.setAge(25);
        dto.setHeight(165.0);
        dto.setActivityLevel(1);
        return dto;
    }

    private BodyCalcService.CalcResult calcResult() {
        return new BodyCalcService.CalcResult(1400, 1800, 1600, 180.0, 90.0, 45.0, false);
    }
}