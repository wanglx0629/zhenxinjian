package cn.zhenxinjian.controller;

import cn.zhenxinjian.common.utils.UserContext;
import cn.zhenxinjian.config.WebMvcTestConfig;
import cn.zhenxinjian.domain.vo.FoodRecognizeVO;
import cn.zhenxinjian.security.JwtAuthenticationFilter;
import cn.zhenxinjian.service.impl.AiRecognizeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FoodRecognizeController 单元测试（addFilters=false：仅验证 200 与响应体/code）
 * 作者: wanglx
 */
@WebMvcTest(FoodRecognizeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcTestConfig.class)
class FoodRecognizeControllerTest {

    @Autowired private org.springframework.test.web.servlet.MockMvc mockMvc;
    @MockBean private AiRecognizeService aiRecognizeService;
    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;
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

    /** 场景：上传图片 → 200 + 候选列表 */
    @Test
    void recognize_returnsCandidates() throws Exception {
        FoodRecognizeVO vo = new FoodRecognizeVO();
        vo.setName("白米饭");
        vo.setCarb(25.9);
        vo.setProtein(2.6);
        vo.setFat(0.3);
        vo.setKcal(116.7);
        when(aiRecognizeService.recognize(eq(1L), any())).thenReturn(List.of(vo));

        MockMultipartFile file = new MockMultipartFile("file", "rice.jpg", "image/jpeg", "x".getBytes());
        mockMvc.perform(multipart("/food/recognize").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].name").value("白米饭"))
                .andExpect(jsonPath("$.data[0].kcal").value(116.7));
    }

    /** 场景：非食物图 → 200 + 空候选 */
    @Test
    void recognize_nonFood_returnsEmpty() throws Exception {
        when(aiRecognizeService.recognize(eq(1L), any())).thenReturn(List.of());

        MockMultipartFile file = new MockMultipartFile("file", "scene.jpg", "image/jpeg", "x".getBytes());
        mockMvc.perform(multipart("/food/recognize").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
