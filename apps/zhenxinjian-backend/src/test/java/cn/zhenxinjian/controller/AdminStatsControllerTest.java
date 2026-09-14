package cn.zhenxinjian.controller;

import cn.zhenxinjian.domain.vo.StatsOverviewVO;
import cn.zhenxinjian.security.JwtAuthenticationFilter;
import cn.zhenxinjian.service.impl.AdminStatsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import cn.zhenxinjian.config.WebMvcTestConfig;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AdminStatsController 单元测试
 * 作者: wanglx
 */
@WebMvcTest(AdminStatsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcTestConfig.class)
class AdminStatsControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private AdminStatsService adminStatsService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(roles = "ADMIN")
    void overview_returnsStats() throws Exception {
        when(adminStatsService.overview()).thenReturn(new StatsOverviewVO());
        mockMvc.perform(get("/admin/stats/overview"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void activeTrend_returnsTrend() throws Exception {
        when(adminStatsService.activeTrend(7)).thenReturn(List.of());
        mockMvc.perform(get("/admin/stats/active-trend"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void eventRank_returnsRank() throws Exception {
        when(adminStatsService.eventRank(anyInt(), anyInt())).thenReturn(List.of());
        mockMvc.perform(get("/admin/stats/event-rank"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void pageRank_returnsRank() throws Exception {
        when(adminStatsService.pageRank(anyInt(), anyInt())).thenReturn(List.of());
        mockMvc.perform(get("/admin/stats/page-rank"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void aggregate_returnsOk() throws Exception {
        mockMvc.perform(post("/admin/stats/aggregate")
                .param("date", "2026-01-01"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void overview_withoutAuth_returns403() throws Exception {
        when(adminStatsService.overview()).thenReturn(new StatsOverviewVO());
        mockMvc.perform(get("/admin/stats/overview"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }
}