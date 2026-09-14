package cn.zhenxinjian.controller;

import cn.zhenxinjian.common.utils.UserContext;
import cn.zhenxinjian.domain.dto.TrackEventBatchDTO;
import cn.zhenxinjian.security.JwtAuthenticationFilter;
import cn.zhenxinjian.service.impl.TrackEventService;
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
 * TrackController 单元测试
 * 作者: wanglx
 */
@WebMvcTest(TrackController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcTestConfig.class)
class TrackControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private TrackEventService trackEventService;
    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;

    /** 场景：埋点上报 → 200（白名单，登录可选） */
    @Test
    void events_returnsOk() throws Exception {
        mockMvc.perform(post("/track/events").contentType(MediaType.APPLICATION_JSON)
                .content("{\"events\":[{\"eventCode\":\"page_view\",\"page\":\"/pages/index/index\"}]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }
}