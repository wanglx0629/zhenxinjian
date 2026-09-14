package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.domain.po.DietRecord;
import cn.zhenxinjian.domain.po.User;
import cn.zhenxinjian.domain.query.AdminDietRecordQuery;
import cn.zhenxinjian.domain.vo.AdminDietRecordVO;
import cn.zhenxinjian.mapper.DietRecordMapper;
import cn.zhenxinjian.mapper.UserMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 管理端饮食记录查看服务单元测试
 * 作者: wanglx
 */
class AdminDietServiceTest {

    private DietRecordMapper dietRecordMapper;
    private UserMapper userMapper;
    private AdminDietService service;

    @BeforeEach
    void setUp() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, DietRecord.class);
        TableInfoHelper.initTableInfo(assistant, User.class);

        dietRecordMapper = mock(DietRecordMapper.class);
        userMapper = mock(UserMapper.class);
        service = new AdminDietService(dietRecordMapper, userMapper);
    }

    /** 场景：startDate > endDate → 抛错 */
    @Test
    void page_startAfterEnd_throwsException() {
        AdminDietRecordQuery query = new AdminDietRecordQuery();
        query.setStartDate(LocalDate.of(2026, 9, 10));
        query.setEndDate(LocalDate.of(2026, 9, 1));

        assertThrows(BusinessException.class, () -> service.page(query));
    }

    /** 场景：正常分页查询含昵称组装 */
    @Test
    void page_validQuery_returnsPageWithNicknames() {
        AdminDietRecordQuery query = new AdminDietRecordQuery();
        query.setStartDate(LocalDate.of(2026, 9, 1));
        query.setEndDate(LocalDate.of(2026, 9, 10));

        DietRecord record = new DietRecord();
        record.setId(1L);
        record.setUserId(100L);
        record.setRecordDate(LocalDate.of(2026, 9, 5));
        record.setMealType(1);
        record.setFoodName("鸡蛋");
        record.setAmountG(100.0);
        record.setCarbG(0.0);
        record.setProteinG(13.3);
        record.setFatG(8.8);
        record.setKcal(144);
        record.setSource(1);
        Page<DietRecord> page = new Page<>(1, 10);
        page.setRecords(List.of(record));
        page.setTotal(1);

        when(dietRecordMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);

        User user = new User();
        user.setId(100L);
        user.setNickname("测试用户");
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(user));

        var result = service.page(query);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals("测试用户", result.getRecords().get(0).getUserNickname());
    }

    /** 场景：用户关键词无命中 → 返回空页 */
    @Test
    void page_keywordNoMatch_returnsEmptyPage() {
        AdminDietRecordQuery query = new AdminDietRecordQuery();
        query.setUserKeyword("不存在用户");
        query.setStartDate(LocalDate.of(2026, 9, 1));
        query.setEndDate(LocalDate.of(2026, 9, 10));

        when(userMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        var result = service.page(query);

        assertNotNull(result);
        assertEquals(0, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
    }

    /** 场景：用户已删除（昵称兜底「用户#id」） */
    @Test
    void page_userDeleted_fallbackNickname() {
        AdminDietRecordQuery query = new AdminDietRecordQuery();
        query.setStartDate(LocalDate.of(2026, 9, 1));
        query.setEndDate(LocalDate.of(2026, 9, 10));

        DietRecord record = new DietRecord();
        record.setId(1L);
        record.setUserId(999L);
        record.setRecordDate(LocalDate.of(2026, 9, 5));
        record.setMealType(1);
        record.setFoodName("鸡胸肉");
        record.setAmountG(200.0);
        record.setCarbG(5.0);
        record.setProteinG(38.8);
        record.setFatG(10.0);
        record.setKcal(266);
        record.setSource(1);
        Page<DietRecord> page = new Page<>(1, 10);
        page.setRecords(List.of(record));
        page.setTotal(1);

        when(dietRecordMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of()); // 用户不存在

        var result = service.page(query);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals("用户#999", result.getRecords().get(0).getUserNickname());
    }

    /** 场景：纯数字关键词按 ID 精确查用户 */
    @Test
    void page_numericKeyword_searchesById() {
        AdminDietRecordQuery query = new AdminDietRecordQuery();
        query.setUserKeyword("100");
        query.setStartDate(LocalDate.of(2026, 9, 1));
        query.setEndDate(LocalDate.of(2026, 9, 10));

        // 纯数字关键词不走 userMapper.selectList，直接构造 List.of(100L)
        Page<DietRecord> page = new Page<>(1, 10);
        page.setRecords(List.of());
        page.setTotal(0);

        when(dietRecordMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);

        var result = service.page(query);

        assertNotNull(result);
        assertEquals(0, result.getTotal());
    }
}