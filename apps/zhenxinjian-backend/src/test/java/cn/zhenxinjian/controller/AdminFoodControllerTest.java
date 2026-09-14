package cn.zhenxinjian.controller;

import cn.zhenxinjian.domain.query.AdminFoodQuery;
import cn.zhenxinjian.domain.vo.FoodVO;
import cn.zhenxinjian.domain.dto.AdminFoodSaveDTO;
import cn.zhenxinjian.security.JwtAuthenticationFilter;
import cn.zhenxinjian.service.impl.AdminFoodService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import cn.zhenxinjian.config.WebMvcTestConfig;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AdminFoodController 单元测试
 * 作者: wanglx
 */
@WebMvcTest(AdminFoodController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcTestConfig.class)
class AdminFoodControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private AdminFoodService adminFoodService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(roles = "ADMIN")
    void page_withAdminRole_returnsPage() throws Exception {
        when(adminFoodService.page(any(AdminFoodQuery.class))).thenReturn(new Page<>());
        mockMvc.perform(get("/admin/foods"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_validDto_returnsOk() throws Exception {
        mockMvc.perform(post("/admin/foods").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validAdminFoodSaveDTO())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_validDto_returnsOk() throws Exception {
        mockMvc.perform(put("/admin/foods/1").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validAdminFoodSaveDTO())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void remove_returnsOk() throws Exception {
        mockMvc.perform(delete("/admin/foods/1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void changeStatus_returnsOk() throws Exception {
        mockMvc.perform(put("/admin/foods/1/status").param("status", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void page_withoutAuth_returns403() throws Exception {
        when(adminFoodService.page(any(AdminFoodQuery.class))).thenReturn(new Page<>());
        mockMvc.perform(get("/admin/foods"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    private AdminFoodSaveDTO validAdminFoodSaveDTO() {
        AdminFoodSaveDTO dto = new AdminFoodSaveDTO();
        dto.setName("测试食物");
        dto.setCategoryCode("01");
        dto.setCategoryName("主食");
        dto.setCarb(new BigDecimal("0"));
        dto.setProtein(new BigDecimal("0"));
        dto.setFat(new BigDecimal("0"));
        dto.setKcal(0);
        return dto;
    }
}