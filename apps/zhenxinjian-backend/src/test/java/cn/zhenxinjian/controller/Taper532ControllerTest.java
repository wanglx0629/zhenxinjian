package cn.zhenxinjian.controller;

import cn.zhenxinjian.common.utils.UserContext;
import cn.zhenxinjian.security.JwtAuthenticationFilter;
import cn.zhenxinjian.service.impl.Taper532Service;
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

import static org.mockito.Mockito.mockStatic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Taper532Controller 单元测试
 * 作者: wanglx
 */
@WebMvcTest(Taper532Controller.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcTestConfig.class)
class Taper532ControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private Taper532Service taper532Service;
    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    private MockedStatic<UserContext> userContextMock;

    @BeforeEach
    void setUp() {
        userContextMock = mockStatic(UserContext.class);
        userContextMock.when(UserContext::getUserId).thenReturn(1L);
    }
    @AfterEach
    void tearDown() { userContextMock.close(); }

    /** 场景：获取532计划 → 200 */
    @Test
    void plan_returnsPlan() throws Exception {
        mockMvc.perform(get("/taper-532/plan"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }
}