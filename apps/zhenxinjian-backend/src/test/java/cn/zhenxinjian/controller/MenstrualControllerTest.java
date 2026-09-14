package cn.zhenxinjian.controller;

import cn.zhenxinjian.common.utils.UserContext;
import cn.zhenxinjian.domain.dto.MenstrualSaveDTO;
import cn.zhenxinjian.domain.vo.MenstrualVO;
import cn.zhenxinjian.security.JwtAuthenticationFilter;
import cn.zhenxinjian.service.impl.MenstrualService;
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
 * MenstrualController 单元测试
 * 作者: wanglx
 */
@WebMvcTest(MenstrualController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcTestConfig.class)
class MenstrualControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private MenstrualService menstrualService;
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
    void get_returnsSetting() throws Exception {
        MenstrualVO vo = new MenstrualVO();
        when(menstrualService.get(any())).thenReturn(vo);
        mockMvc.perform(get("/menstrual"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void save_validDto_returnsOk() throws Exception {
        MenstrualSaveDTO dto = new MenstrualSaveDTO();
        dto.setEnabled(1);
        dto.setCycleLen(28);
        dto.setPeriodDays(5);
        MenstrualVO vo = new MenstrualVO();
        when(menstrualService.save(any(), any())).thenReturn(vo);
        mockMvc.perform(put("/menstrual").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void save_invalidDto_returns400() throws Exception {
        mockMvc.perform(put("/menstrual").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new MenstrualSaveDTO())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }
}