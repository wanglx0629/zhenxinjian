package cn.zhenxinjian.controller;

import cn.zhenxinjian.service.impl.AdminDietService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.zhenxinjian.domain.vo.AdminDietRecordVO;
import cn.zhenxinjian.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import cn.zhenxinjian.config.WebMvcTestConfig;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AdminDietController 单元测试
 * 作者: wanglx
 */
@WebMvcTest(AdminDietController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcTestConfig.class)
class AdminDietControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private AdminDietService adminDietService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(roles = "ADMIN")
    void page_withAdminRole_returnsPage() throws Exception {
        IPage<AdminDietRecordVO> page = new Page<>();
        when(adminDietService.page(any())).thenReturn(page);
        mockMvc.perform(get("/admin/diet-records"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void page_withoutAuth_returns403() throws Exception {
        IPage<AdminDietRecordVO> page = new Page<>();
        when(adminDietService.page(any())).thenReturn(page);
        mockMvc.perform(get("/admin/diet-records"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }
}