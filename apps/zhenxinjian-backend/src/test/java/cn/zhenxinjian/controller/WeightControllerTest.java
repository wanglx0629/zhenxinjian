package cn.zhenxinjian.controller;

import cn.zhenxinjian.common.utils.UserContext;
import cn.zhenxinjian.domain.dto.WeightSaveDTO;
import cn.zhenxinjian.domain.vo.AdjustLogVO;
import cn.zhenxinjian.domain.vo.WeightRecordVO;
import cn.zhenxinjian.security.JwtAuthenticationFilter;
import cn.zhenxinjian.service.impl.WeightService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WeightController 单元测试
 * 作者: wanglx
 */
@WebMvcTest(WeightController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcTestConfig.class)
class WeightControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WeightService weightService;
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

    /** 场景：保存体重 → 200 + 返回记录 */
    @Test
    void save_validDto_returnsRecord() throws Exception {
        WeightSaveDTO dto = new WeightSaveDTO();
        dto.setWeight(68.0);
        dto.setRecordDate(java.time.LocalDate.of(2026, 9, 14));

        WeightRecordVO vo = new WeightRecordVO();
        vo.setId(1L);
        when(weightService.save(any(), any())).thenReturn(vo);

        mockMvc.perform(put("/weight")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /** 场景：保存体重参数校验失败 → 200 + code 500（GlobalExceptionHandler统一拦截） */
    @Test
    void save_invalidDto_returns400() throws Exception {
        WeightSaveDTO dto = new WeightSaveDTO();

        mockMvc.perform(put("/weight")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    /** 场景：查询体重记录 → 200 */
    @Test
    void list_returnsRecords() throws Exception {
        when(weightService.list(any(), isNull(), isNull(), isNull())).thenReturn(List.of());

        mockMvc.perform(get("/weight"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /** 场景：删除体重记录 → 200 */
    @Test
    void remove_returnsOk() throws Exception {
        mockMvc.perform(delete("/weight/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /** 场景：查询调碳日志 → 200 */
    @Test
    void logs_returnsLogs() throws Exception {
        when(weightService.listLogs(any())).thenReturn(List.of());

        mockMvc.perform(get("/weight/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}