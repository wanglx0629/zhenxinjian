package cn.zhenxinjian.service.impl;

import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.common.sensitive.SensitiveWordFilter;
import cn.zhenxinjian.domain.dto.AdminFoodSaveDTO;
import cn.zhenxinjian.domain.po.Food;
import cn.zhenxinjian.domain.po.FoodImage;
import cn.zhenxinjian.domain.po.User;
import cn.zhenxinjian.domain.query.AdminFoodQuery;
import cn.zhenxinjian.domain.vo.FoodVO;
import cn.zhenxinjian.mapper.FoodImageMapper;
import cn.zhenxinjian.mapper.FoodMapper;
import cn.zhenxinjian.mapper.UserMapper;
import cn.zhenxinjian.service.StorageService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 管理端食物库维护服务单元测试
 * （同名 40903 / 守恒 40403 / 自定义拒写 40404 / code 顺延 / 审核状态+AI 结论筛选 / 提交人昵称）
 * 作者: wanglx
 */
@ExtendWith(MockitoExtension.class)
class AdminFoodServiceTest {

    @Mock
    private FoodMapper foodMapper;

    @Mock
    private FoodImageMapper foodImageMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private SensitiveWordFilter sensitiveWordFilter;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private AdminFoodService adminFoodService;

    @BeforeEach
    void setUp() {
        // 无 MyBatis 环境下 LambdaWrapper 列名解析依赖 TableInfo（幂等初始化；FoodImage 供预热图查询）
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Food.class);
        TableInfoHelper.initTableInfo(assistant, FoodImage.class);
        // ServiceImpl 的 baseMapper 为继承字段，构造器注入不会填充，显式装配
        ReflectionTestUtils.setField(adminFoodService, "baseMapper", foodMapper);
    }

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

    /** 场景：同名内置食物已存在 → 40903 */
    @Test
    void create_duplicateName_throw40903() {
        when(foodMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminFoodService.create(validDto()));
        assertEquals(CommonConstant.ADMIN_FOOD_CONFLICT_CODE, ex.getCode());
    }

    /** 场景：能量与 4/4/9 换算偏差超 ±10% → 40403 */
    @Test
    void create_kcalMismatch_throw40403() {
        when(foodMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        AdminFoodSaveDTO dto = validDto();
        dto.setKcal(999);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminFoodService.create(dto));
        assertEquals(CommonConstant.FOOD_KCAL_MISMATCH_CODE, ex.getCode());
    }

    /** 场景：编辑自定义食物 → 40404 拒写 */
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

    /** 场景：首个新增内置食物（库中无 F 码）→ code 从 F201 起 */
    @Test
    void create_firstCustomCode_startF201() {
        when(foodMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        adminFoodService.create(validDto());
        verify(foodMapper).insert(argThat((Food f) -> "F201".equals(f.getCode()) && f.getSource() == 1));
    }

    /** 场景：分页带审核状态 + AI 结论筛选；共建行填提交人昵称，基础食物不填 */
    @Test
    void page_auditFiltersAndSubmitterName() {
        Food custom = new Food();
        custom.setId(21L);
        custom.setSource(2);
        custom.setUserId(9L);
        custom.setAuditStatus(0);
        custom.setName("燕麦碗");
        Food builtin = new Food();
        builtin.setId(31L);
        builtin.setSource(1);
        builtin.setCode("F001");
        builtin.setAuditStatus(3);
        builtin.setName("鸡胸肉");
        Page<Food> page = new Page<>(1, 10);
        page.setRecords(List.of(custom, builtin));
        when(foodMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);
        User user = new User();
        user.setId(9L);
        user.setNickname("小明");
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(user));
        when(foodImageMapper.selectList(any())).thenReturn(List.of());

        AdminFoodQuery query = new AdminFoodQuery();
        query.setAuditStatus(0);
        query.setAiVerdict("suspect");
        IPage<FoodVO> result = adminFoodService.page(query);

        ArgumentCaptor<Wrapper<Food>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(foodMapper).selectPage(any(Page.class), captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertTrue(sql.contains("audit_status"));
        assertTrue(sql.contains("ai_verdict"));
        assertEquals("小明", result.getRecords().get(0).getSubmitterName());
        assertNull(result.getRecords().get(1).getSubmitterName());
    }

    /** 场景：共建行用户上传图（foods.image）不被内置预热图覆盖 */
    @Test
    void page_customUploadedImage_notOverwritten() {
        Food custom = new Food();
        custom.setId(21L);
        custom.setSource(2);
        custom.setUserId(9L);
        custom.setName("燕麦碗");
        custom.setImage("/upload/food/oat.png");
        Food builtin = new Food();
        builtin.setId(31L);
        builtin.setSource(1);
        builtin.setCode("F001");
        builtin.setName("鸡胸肉");
        Page<Food> page = new Page<>(1, 10);
        page.setRecords(List.of(custom, builtin));
        when(foodMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);
        cn.zhenxinjian.domain.po.FoodImage img = new cn.zhenxinjian.domain.po.FoodImage();
        img.setFoodCode("F001");
        img.setUrl("/img/f001.png");
        when(foodImageMapper.selectList(any())).thenReturn(List.of(img));
        when(userMapper.selectBatchIds(any())).thenReturn(List.of());
        // 恒等解析：验证 foods.image 优先不被预热图覆盖（解析行为由 StorageServiceImplTest 覆盖）
        when(storageService.publicUrl(any())).thenAnswer(inv -> inv.getArgument(0));

        IPage<FoodVO> result = adminFoodService.page(new AdminFoodQuery());

        assertEquals("/upload/food/oat.png", result.getRecords().get(0).getImage());
        assertEquals("/img/f001.png", result.getRecords().get(1).getImage());
    }

    /** 场景：foods.image 存相对 objectKey → 管理端分页解析为公网地址（存相对、展示绝对） */
    @Test
    void page_relativeImage_resolvedToPublicUrl() {
        Food custom = new Food();
        custom.setId(21L);
        custom.setSource(2);
        custom.setUserId(9L);
        custom.setName("燕麦碗");
        custom.setImage("upload/food/oat.png");
        Page<Food> page = new Page<>(1, 10);
        page.setRecords(List.of(custom));
        when(foodMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);
        when(userMapper.selectBatchIds(any())).thenReturn(List.of());
        when(storageService.publicUrl("upload/food/oat.png"))
                .thenReturn("http://localhost:9000/zhenxinjian/upload/food/oat.png");

        IPage<FoodVO> result = adminFoodService.page(new AdminFoodQuery());

        assertEquals("http://localhost:9000/zhenxinjian/upload/food/oat.png",
                result.getRecords().get(0).getImage());
    }
}
