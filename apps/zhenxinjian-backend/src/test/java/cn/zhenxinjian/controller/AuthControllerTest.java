package cn.zhenxinjian.controller;

import cn.zhenxinjian.common.result.Result;
import cn.zhenxinjian.common.utils.UserContext;
import cn.zhenxinjian.config.WebMvcTestConfig;
import cn.zhenxinjian.domain.dto.LoginDTO;
import cn.zhenxinjian.domain.dto.RegisterDTO;
import cn.zhenxinjian.domain.vo.CaptchaVO;
import cn.zhenxinjian.domain.vo.LoginResultVO;
import cn.zhenxinjian.domain.vo.UserVO;
import cn.zhenxinjian.security.JwtAuthenticationFilter;
import cn.zhenxinjian.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuthController 单元测试
 * 作者: wanglx
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcTestConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MockedStatic<UserContext> userContextMock;

    @BeforeEach
    void setUp() {
        userContextMock = mockStatic(UserContext.class);
    }

    @AfterEach
    void tearDown() {
        userContextMock.close();
    }

    /** 场景：获取验证码 → 200 + 返回 uuid */
    @Test
    void captcha_returnsCaptcha() throws Exception {
        CaptchaVO captchaVO = new CaptchaVO();
        captchaVO.setUuid("abc123");
        captchaVO.setImage("data:image/png;base64,...");
        when(userService.getCaptcha()).thenReturn(captchaVO);

        mockMvc.perform(get("/auth/captcha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.uuid").value("abc123"));
    }

    /** 场景：正常登录 → 200 + token */
    @Test
    void login_validCredentials_returnsToken() throws Exception {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("test");
        dto.setPassword("pass1234");
        dto.setCaptcha("abcd");
        dto.setCaptchaUuid("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");

        LoginResultVO result = new LoginResultVO();
        result.setToken("jwt-token");
        when(userService.login(any(), any())).thenReturn(result);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").value("jwt-token"));
    }

    /** 场景：登录参数校验失败 → 200 + 错误码（GlobalExceptionHandler 拦截） */
    @Test
    void login_invalidDto_returnsError() throws Exception {
        LoginDTO dto = new LoginDTO();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    /** 场景：正常注册 → 200 */
    @Test
    void register_validDto_returnsOk() throws Exception {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("newuser");
        dto.setPassword("Pass@1234");
        dto.setCaptcha("abcd");
        dto.setCaptchaUuid("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /** 场景：注册参数校验失败 → 200 + 错误码（GlobalExceptionHandler 拦截） */
    @Test
    void register_invalidDto_returnsError() throws Exception {
        RegisterDTO dto = new RegisterDTO();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    /** 场景：未登录获取当前用户 → UserContext 为空，抛出 BusinessException */
    @Test
    void me_withoutUser_returnsError() throws Exception {
        userContextMock.when(UserContext::getUserId).thenReturn(null);

        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    /** 场景：已登录获取当前用户 → 200 + 用户信息 */
    @Test
    void me_withUser_returnsUserInfo() throws Exception {
        userContextMock.when(UserContext::getUserId).thenReturn(1L);
        UserVO userVO = new UserVO();
        userVO.setId(1L);
        userVO.setUsername("testuser");
        when(userService.getUserById(1L)).thenReturn(userVO);

        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    /** 场景：登出 → 200 */
    @Test
    void logout_returnsOk() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}