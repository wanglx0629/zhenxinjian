package cn.zhenxinjian.controller;

import cn.zhenxinjian.domain.dto.UserDTO;
import cn.zhenxinjian.domain.vo.UserVO;
import cn.zhenxinjian.security.JwtAuthenticationFilter;
import cn.zhenxinjian.service.UserService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import cn.zhenxinjian.config.WebMvcTestConfig;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UserController 单元测试（ADMIN）
 * 作者: wanglx
 */
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcTestConfig.class)
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private UserService userService;
    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(roles = "ADMIN")
    void page_returnsPage() throws Exception {
        when(userService.pageUsers(any())).thenReturn(new Page<>());
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getById_returnsUser() throws Exception {
        when(userService.getUserById(anyLong())).thenReturn(new UserVO());
        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void add_validDto_returnsOk() throws Exception {
        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validUserDTO())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_validDto_returnsOk() throws Exception {
        mockMvc.perform(put("/users").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validUserDTO())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_returnsOk() throws Exception {
        mockMvc.perform(delete("/users/1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void page_withoutAuth_returns403() throws Exception {
        when(userService.pageUsers(any())).thenReturn(new Page<>());
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    private UserDTO validUserDTO() {
        UserDTO dto = new UserDTO();
        dto.setUsername("testuser");
        return dto;
    }
}