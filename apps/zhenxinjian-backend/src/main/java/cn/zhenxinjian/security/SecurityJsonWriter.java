package cn.zhenxinjian.security;

import cn.zhenxinjian.common.result.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Security 统一 JSON 响应写入
 * 作者: luote (luote) - https://luote996.cn
 */
@Component
@RequiredArgsConstructor
public class SecurityJsonWriter {

    private final ObjectMapper objectMapper;

    /**
     * 写入 JSON 错误响应
     */
    public void write(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(code);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), Result.fail(code, message));
    }
}
