package cn.zhenxinjian.controller;

import cn.zhenxinjian.common.utils.UserContext;
import cn.zhenxinjian.domain.dto.CyclePlanCreateDTO;
import cn.zhenxinjian.domain.vo.CyclePlanVO;
import cn.zhenxinjian.security.JwtAuthenticationFilter;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CyclePlanController 单元测试
 * 作者: wanglx
 */
@WebMvcTest(CyclePlanController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcTestConfig.class)
class CyclePlanControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
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
    void tearDown() { userContextMock.close(); }

    @Test
    void create_validDto_returnsPlan() throws Exception {
        CyclePlanCreateDTO dto = new CyclePlanCreateDTO();
        dto.setCycleDays(7);
        CyclePlanVO vo = new CyclePlanVO(); vo.setId(1L);
        when(cyclePlanService.create(any(), any())).thenReturn(vo);
        mockMvc.perform(post("/cycle/plans").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getCurrent_returnsPlan() throws Exception {
        when(cyclePlanService.getCurrent(any())).thenReturn(null);
        mockMvc.perform(get("/cycle/plans/current"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getById_returnsPlan() throws Exception {
        CyclePlanVO vo = new CyclePlanVO(); vo.setId(1L);
        when(cyclePlanService.getById(any(), anyLong())).thenReturn(vo);
        mockMvc.perform(get("/cycle/plans/1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void terminateCurrent_returnsOk() throws Exception {
        mockMvc.perform(post("/cycle/plans/current/terminate"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }
}