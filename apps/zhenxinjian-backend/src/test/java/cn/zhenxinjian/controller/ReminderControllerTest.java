package cn.zhenxinjian.controller;

import cn.zhenxinjian.common.utils.UserContext;
import cn.zhenxinjian.domain.dto.ReminderSaveDTO;
import cn.zhenxinjian.domain.vo.ReminderVO;
import cn.zhenxinjian.security.JwtAuthenticationFilter;
import cn.zhenxinjian.service.impl.ReminderService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ReminderController 单元测试
 * 作者: wanglx
 */
@WebMvcTest(ReminderController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcTestConfig.class)
class ReminderControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private ReminderService reminderService;
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
    void get_returnsReminder() throws Exception {
        ReminderVO vo = new ReminderVO();
        when(reminderService.getOrCreate(any())).thenReturn(vo);
        mockMvc.perform(get("/reminder"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void save_validDto_returnsOk() throws Exception {
        ReminderSaveDTO dto = new ReminderSaveDTO();
        dto.setMasterSwitch(1);
        dto.setBreakfastSwitch(1);
        dto.setBreakfastTime("08:30");
        dto.setLunchSwitch(1);
        dto.setLunchTime("12:00");
        dto.setDinnerSwitch(1);
        dto.setDinnerTime("18:30");
        ReminderVO vo = new ReminderVO();
        when(reminderService.save(any(), any())).thenReturn(vo);
        mockMvc.perform(put("/reminder").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void reportSubscribe_returnsOk() throws Exception {
        mockMvc.perform(post("/reminder/subscribe").contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }
}