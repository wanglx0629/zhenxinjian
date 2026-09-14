package cn.zhenxinjian.controller;

import cn.zhenxinjian.common.utils.UserContext;
import cn.zhenxinjian.domain.dto.BodyProfileSaveDTO;
import cn.zhenxinjian.domain.dto.ModeSwitchDTO;
import cn.zhenxinjian.domain.vo.BodyProfileVO;
import cn.zhenxinjian.security.JwtAuthenticationFilter;
import cn.zhenxinjian.service.impl.BodyProfileService;
import cn.zhenxinjian.service.impl.CyclePlanService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BodyController 单元测试
 * 作者: wanglx
 */
@WebMvcTest(BodyController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcTestConfig.class)
class BodyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BodyProfileService bodyProfileService;

    @MockBean
    private CyclePlanService cyclePlanService;
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

    /** 场景：保存身体档案 → 200 + 返回档案 */
    @Test
    void saveProfile_validDto_returnsProfile() throws Exception {
        BodyProfileSaveDTO dto = new BodyProfileSaveDTO();
        dto.setGender(1);
        dto.setAge(30);
        dto.setHeight(170.0);
        dto.setWeight(70.0);
        dto.setTargetWeight(65.0);
        dto.setActivityLevel(2);
        dto.setDeficit(400);

        BodyProfileVO vo = new BodyProfileVO();
        vo.setGender(1);
        when(bodyProfileService.save(any(), any())).thenReturn(vo);

        mockMvc.perform(put("/body/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /** 场景：保存档案参数校验失败 → 200 + code 500（GlobalExceptionHandler统一拦截） */
    @Test
    void saveProfile_invalidDto_returns400() throws Exception {
        BodyProfileSaveDTO dto = new BodyProfileSaveDTO();

        mockMvc.perform(put("/body/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    /** 场景：切换减脂模式 → 200 */
    @Test
    void switchMode_validDto_returnsOk() throws Exception {
        ModeSwitchDTO dto = new ModeSwitchDTO();
        dto.setMode(1);

        mockMvc.perform(put("/body/mode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /** 场景：切换模式参数校验失败 → 200 + code 500（GlobalExceptionHandler统一拦截） */
    @Test
    void switchMode_invalidDto_returns400() throws Exception {
        ModeSwitchDTO dto = new ModeSwitchDTO();

        mockMvc.perform(put("/body/mode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    /** 场景：查询身体档案 → 200 */
    @Test
    void getProfile_returnsProfile() throws Exception {
        BodyProfileVO vo = new BodyProfileVO();
        when(bodyProfileService.getCurrent(any())).thenReturn(vo);

        mockMvc.perform(get("/body/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}