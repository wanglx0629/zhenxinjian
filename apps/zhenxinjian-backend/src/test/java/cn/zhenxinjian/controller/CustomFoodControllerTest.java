package cn.zhenxinjian.controller;

import cn.zhenxinjian.common.utils.UserContext;
import cn.zhenxinjian.domain.dto.CustomFoodSaveDTO;
import cn.zhenxinjian.domain.vo.FoodVO;
import cn.zhenxinjian.security.JwtAuthenticationFilter;
import cn.zhenxinjian.service.impl.CustomFoodService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CustomFoodController 单元测试
 * 作者: wanglx
 */
@WebMvcTest(CustomFoodController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcTestConfig.class)
class CustomFoodControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private CustomFoodService customFoodService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    private MockedStatic<UserContext> userContextMock;

    @BeforeEach
    void setUp() {
        userContextMock = mockStatic(UserContext.class);
        userContextMock.when(UserContext::getUserId).thenReturn(1L);
    }
    @AfterEach
    void tearDown() { userContextMock.close(); }

    @Test
    void save_validDto_returnsFood() throws Exception {
        CustomFoodSaveDTO dto = new CustomFoodSaveDTO();
        dto.setName("自定义食物");
        dto.setCarb(new BigDecimal("0"));
        dto.setProtein(new BigDecimal("0"));
        dto.setFat(new BigDecimal("0"));
        dto.setKcal(0);
        FoodVO vo = new FoodVO(); vo.setId(1L);
        when(customFoodService.save(any(), any())).thenReturn(vo);
        mockMvc.perform(post("/food/custom").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void save_invalidDto_returns400() throws Exception {
        mockMvc.perform(post("/food/custom").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CustomFoodSaveDTO())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void listMine_returnsList() throws Exception {
        when(customFoodService.listMine(any())).thenReturn(List.of());
        mockMvc.perform(get("/food/custom/mine"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void remove_returnsOk() throws Exception {
        mockMvc.perform(delete("/food/custom/1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }
}