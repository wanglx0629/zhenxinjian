package cn.zhenxinjian.controller;

import cn.zhenxinjian.domain.vo.AdminUserProfileVO;
import cn.zhenxinjian.domain.vo.BodyProfileVO;
import cn.zhenxinjian.domain.vo.CyclePlanVO;
import cn.zhenxinjian.security.JwtAuthenticationFilter;
import cn.zhenxinjian.service.UserService;
import cn.zhenxinjian.service.impl.BodyProfileService;
import cn.zhenxinjian.service.impl.CyclePlanService;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AdminUserController 单元测试
 * 作者: wanglx
 */
@WebMvcTest(AdminUserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcTestConfig.class)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private UserService userService;
    @MockBean
    private BodyProfileService bodyProfileService;
    @MockBean
    private CyclePlanService cyclePlanService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(roles = "ADMIN")
    void profile_withAdminRole_returnsProfile() throws Exception {
        BodyProfileVO bodyProfile = new BodyProfileVO();
        bodyProfile.setRecorded(false);
        CyclePlanVO cyclePlan = new CyclePlanVO();
        cyclePlan.setId(1L);
        when(userService.getUserById(anyLong())).thenReturn(null);
        when(bodyProfileService.getCurrent(any())).thenReturn(bodyProfile);
        when(cyclePlanService.getCurrent(any())).thenReturn(cyclePlan);
        mockMvc.perform(get("/admin/users/1/profile"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void profile_withoutAuth_returns403() throws Exception {
        BodyProfileVO bodyProfile = new BodyProfileVO();
        bodyProfile.setRecorded(false);
        CyclePlanVO cyclePlan = new CyclePlanVO();
        cyclePlan.setId(1L);
        when(userService.getUserById(anyLong())).thenReturn(null);
        when(bodyProfileService.getCurrent(any())).thenReturn(bodyProfile);
        when(cyclePlanService.getCurrent(any())).thenReturn(cyclePlan);
        mockMvc.perform(get("/admin/users/1/profile"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }
}