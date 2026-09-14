package cn.zhenxinjian.controller;

import cn.zhenxinjian.security.JwtAuthenticationFilter;
import cn.zhenxinjian.service.WechatAuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import cn.zhenxinjian.config.WebMvcTestConfig;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WechatAuthController 单元测试
 * 作者: wanglx
 */
@WebMvcTest(WechatAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcTestConfig.class)
class WechatAuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private WechatAuthService wechatAuthService;
    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;

    /** 场景：微信登录 → 正常路径（白名单无需认证） */
    @Test
    void wechatLogin_returnsOk() throws Exception {
        mockMvc.perform(post("/auth/wechat/login").contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk());
    }

    /** 场景：游客登录 → 正常路径（白名单无需认证） */
    @Test
    void guestLogin_returnsOk() throws Exception {
        mockMvc.perform(post("/auth/guest"))
                .andExpect(status().isOk());
    }
}