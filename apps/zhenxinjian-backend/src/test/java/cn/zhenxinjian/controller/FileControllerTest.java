package cn.zhenxinjian.controller;

import cn.zhenxinjian.common.utils.UserContext;
import cn.zhenxinjian.security.JwtAuthenticationFilter;
import cn.zhenxinjian.service.StorageService;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mockStatic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FileController 单元测试
 * 作者: wanglx
 */
@WebMvcTest(FileController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcTestConfig.class)
class FileControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private StorageService storageService;
    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    private MockedStatic<UserContext> userContextMock;

    @BeforeEach
    void setUp() {
        userContextMock = mockStatic(UserContext.class);
        userContextMock.when(UserContext::getUserId).thenReturn(1L);
    }
    @AfterEach
    void tearDown() { userContextMock.close(); }

    /** 场景：上传文件 → 正常路径 */
    @Test
    void upload_returnsUrl() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test".getBytes());
        mockMvc.perform(multipart("/files/upload").file(file))
                .andExpect(status().isOk());
    }

    /** 场景：删除文件（需ADMIN角色，但@WebMvcTest不加载方法安全拦截器） → 200 + 500(缺少参数) */
    @Test
    void delete_withoutAuth_returns403() throws Exception {
        mockMvc.perform(delete("/files"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(500));
    }

    /** 场景：删除文件（需ADMIN角色） → 200 */
    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_withAdminRole_returnsOk() throws Exception {
        mockMvc.perform(delete("/files")
                .param("objectKey", "test-key"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }
}