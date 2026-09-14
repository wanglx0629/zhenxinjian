package cn.zhenxinjian.controller;

import cn.zhenxinjian.common.utils.UserContext;
import cn.zhenxinjian.domain.dto.DietRecordCreateDTO;
import cn.zhenxinjian.domain.dto.DietRecordUpdateDTO;
import cn.zhenxinjian.domain.vo.DietDayVO;
import cn.zhenxinjian.domain.vo.DietRecordVO;
import cn.zhenxinjian.domain.vo.DietSummaryVO;
import cn.zhenxinjian.security.JwtAuthenticationFilter;
import cn.zhenxinjian.service.impl.DietRecordService;
import cn.zhenxinjian.service.impl.DietSummaryService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DietController 单元测试
 * 作者: wanglx
 */
@WebMvcTest(DietController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcTestConfig.class)
class DietControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DietRecordService dietRecordService;
    @MockBean
    private DietSummaryService dietSummaryService;
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

    /** 场景：新增饮食记录 → 200 + 返回记录 */
    @Test
    void create_validDto_returnsRecord() throws Exception {
        DietRecordCreateDTO dto = new DietRecordCreateDTO();
        dto.setMealType(1);
        dto.setSource(3);
        dto.setName("测试食物");

        DietRecordVO vo = new DietRecordVO();
        vo.setId(1L);
        when(dietRecordService.create(any(), any())).thenReturn(vo);

        mockMvc.perform(post("/diet/records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    /** 场景：新增饮食记录参数校验失败 → 200 + code 500（GlobalExceptionHandler统一拦截） */
    @Test
    void create_invalidDto_returns400() throws Exception {
        DietRecordCreateDTO dto = new DietRecordCreateDTO();

        mockMvc.perform(post("/diet/records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    /** 场景：编辑饮食记录 → 200 */
    @Test
    void update_validDto_returnsRecord() throws Exception {
        DietRecordUpdateDTO dto = new DietRecordUpdateDTO();
        dto.setMealType(2);

        DietRecordVO vo = new DietRecordVO();
        vo.setId(1L);
        when(dietRecordService.update(any(), eq(1L), any())).thenReturn(vo);

        mockMvc.perform(put("/diet/records/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /** 场景：删除饮食记录 → 200 */
    @Test
    void remove_returnsOk() throws Exception {
        mockMvc.perform(delete("/diet/records/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /** 场景：按日查询 → 200 */
    @Test
    void listByDate_returnsRecords() throws Exception {
        DietDayVO vo = new DietDayVO();
        when(dietRecordService.listByDate(any(), any())).thenReturn(vo);

        mockMvc.perform(get("/diet/records").param("date", "2026-09-14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /** 场景：当日累计 → 200 */
    @Test
    void summary_returnsSummary() throws Exception {
        DietSummaryVO vo = new DietSummaryVO();
        when(dietSummaryService.summary(any(), any())).thenReturn(vo);

        mockMvc.perform(get("/diet/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}