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
 * 作者: wanglx
 */
@Component
@RequiredArgsConstructor
public class SecurityJsonWriter {

    private final ObjectMapper objectMapper;

    /**
     * 写入 JSON 错误响应
     * 业务码非合法 HTTP 状态（如 40201 游客到期）时以 HTTP 200 承载，业务码进 Result body
     */
    public void write(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(isValidHttpStatus(code) ? code : HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), Result.fail(code, message));
    }

    /**
     * 是否合法 HTTP 状态码（100-599）
     */
    private boolean isValidHttpStatus(int code) {
        return code >= 100 && code <= 599;
    }
}
