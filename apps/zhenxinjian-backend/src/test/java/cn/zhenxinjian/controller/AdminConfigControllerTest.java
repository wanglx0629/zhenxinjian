package cn.zhenxinjian.controller;

import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.constant.ExceptionConstant;
import cn.zhenxinjian.common.sensitive.SensitiveWordFilter;
import cn.zhenxinjian.config.WebMvcTestConfig;
import cn.zhenxinjian.domain.dto.ProjectConfigCreateDTO;
import cn.zhenxinjian.domain.dto.ProjectConfigUpdateDTO;
import cn.zhenxinjian.domain.vo.ProjectConfigVO;
import cn.zhenxinjian.security.JwtAuthenticationFilter;
import cn.zhenxinjian.service.ConfigService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AdminConfigController 单元测试（分页/新增/修改 + 敏感词 key 触发词库重建 + 校验拦截）
 * 作者: wanglx
 */
@WebMvcTest(AdminConfigController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcTestConfig.class)
class AdminConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private ConfigService configService;
    @MockBean
    private SensitiveWordFilter sensitiveWordFilter;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(roles = "ADMIN")
    void page_withAdminRole_returnsPage() throws Exception {
        when(configService.page(ArgumentMatchers.any(), ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyLong())).thenReturn(new Page<>());
        mockMvc.perform(get("/admin/configs"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_plainKey_returnsOkWithoutRefresh() throws Exception {
        doReturn(voWithKey("ocr.api-key")).when(configService)
                .create(ArgumentMatchers.any(ProjectConfigCreateDTO.class));
        mockMvc.perform(post("/admin/configs").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto("ocr.api-key"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        verify(sensitiveWordFilter, never()).refresh();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_sensitiveKey_triggersRefresh() throws Exception {
        doReturn(voWithKey("sensitive.filter.extra-words")).when(configService)
                .create(ArgumentMatchers.any(ProjectConfigCreateDTO.class));
        mockMvc.perform(post("/admin/configs").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto("sensitive.filter.extra-words"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        verify(sensitiveWordFilter).refresh();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_duplicateKey_returns40904() throws Exception {
        when(configService.create(ArgumentMatchers.any(ProjectConfigCreateDTO.class)))
                .thenThrow(new BusinessException(CommonConstant.CONFIG_KEY_DUPLICATE_CODE,
                        ExceptionConstant.CONFIG_KEY_DUPLICATE));
        mockMvc.perform(post("/admin/configs").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto("sensitive.filter.enabled"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(40904))
                .andExpect(jsonPath("$.message").value(ExceptionConstant.CONFIG_KEY_DUPLICATE));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_invalidKey_rejectedByValidation() throws Exception {
        // DTO @Pattern 先行拦截（通用参数校验通道），服务层 40905 为非 HTTP 兜底
        mockMvc.perform(post("/admin/configs").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto("Bad Key"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("配置键须为小写点分格式（如 a.b.c）"));
        verify(configService, never()).create(ArgumentMatchers.any(ProjectConfigCreateDTO.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_sensitiveKey_triggersRefresh() throws Exception {
        doReturn(voWithKey("sensitive.filter.enabled")).when(configService)
                .update(ArgumentMatchers.eq(1L), ArgumentMatchers.any(ProjectConfigUpdateDTO.class));
        ProjectConfigUpdateDTO dto = new ProjectConfigUpdateDTO();
        dto.setConfigValue("true");
        mockMvc.perform(put("/admin/configs/1").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        verify(sensitiveWordFilter).refresh();
    }

    private ProjectConfigCreateDTO createDto(String key) {
        ProjectConfigCreateDTO dto = new ProjectConfigCreateDTO();
        dto.setConfigKey(key);
        dto.setValueType(1);
        dto.setConfigValue("v");
        return dto;
    }

    private ProjectConfigVO voWithKey(String key) {
        ProjectConfigVO vo = new ProjectConfigVO();
        vo.setId(1L);
        vo.setConfigKey(key);
        vo.setValueType(1);
        vo.setStatus(1);
        return vo;
    }
}
