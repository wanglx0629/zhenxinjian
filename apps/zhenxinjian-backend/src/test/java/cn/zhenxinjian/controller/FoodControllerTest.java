package cn.zhenxinjian.controller;

import cn.zhenxinjian.common.utils.UserContext;
import cn.zhenxinjian.domain.vo.FoodCalcVO;
import cn.zhenxinjian.domain.vo.FoodCategoryVO;
import cn.zhenxinjian.domain.vo.FoodVO;
import cn.zhenxinjian.security.JwtAuthenticationFilter;
import cn.zhenxinjian.service.impl.FoodService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import cn.zhenxinjian.config.WebMvcTestConfig;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FoodController 单元测试
 * 作者: wanglx
 */
@WebMvcTest(FoodController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcTestConfig.class)
class FoodControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FoodService foodService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MockedStatic<UserContext> userContextMock;

    @BeforeEach
    void setUp() {
        userContextMock = mockStatic(UserContext.class);
        userContextMock.when(UserContext::getUserId).thenReturn(1L);
    }

    @AfterEach
    void tearDown() {
        userContextMock.close();
    }

    /** 场景：食物搜索 → 200 */
    @Test
    void search_returnsPage() throws Exception {
        IPage<FoodVO> page = new Page<>();
        when(foodService.search(any(), any())).thenReturn(page);

        mockMvc.perform(get("/food/search").param("keyword", "米饭"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /** 场景：分类列表 → 200 */
    @Test
    void categories_returnsList() throws Exception {
        when(foodService.categories()).thenReturn(List.of());

        mockMvc.perform(get("/food/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /** 场景：热门食物 → 200 */
    @Test
    void hot_returnsList() throws Exception {
        when(foodService.hot()).thenReturn(List.of());

        mockMvc.perform(get("/food/hot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /** 场景：食物详情 → 200 */
    @Test
    void detail_returnsFood() throws Exception {
        FoodVO vo = new FoodVO();
        vo.setId(1L);
        vo.setName("米饭");
        when(foodService.detail(any(), anyLong())).thenReturn(vo);

        mockMvc.perform(get("/food/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /** 场景：份量试算 → 200 */
    @Test
    void calc_validGrams_returnsCalc() throws Exception {
        FoodCalcVO vo = new FoodCalcVO();
        when(foodService.calc(any(), anyLong(), anyInt())).thenReturn(vo);

        mockMvc.perform(get("/food/1/calc").param("grams", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /** 场景：份量试算参数校验失败 → 200 + code 500（GlobalExceptionHandler统一拦截，grams 为 0） */
    @Test
    void calc_invalidGrams_returns400() throws Exception {
        mockMvc.perform(get("/food/1/calc").param("grams", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }
}