package com.example.racetransport.security;

import com.example.racetransport.common.response.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** Trả JSON 401/403 thống nhất với ApiError thay vì trang lỗi mặc định. */
@Component
public class RestAuthHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper mapper;

    public RestAuthHandlers(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        write(response, request, 401, "UNAUTHORIZED", "Bạn cần đăng nhập để tiếp tục");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        write(response, request, 403, "FORBIDDEN", "Bạn không có quyền truy cập chức năng này");
    }

    private void write(HttpServletResponse response, HttpServletRequest request, int status, String code, String msg)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        mapper.writeValue(response.getOutputStream(),
                new ApiError(LocalDateTime.now(), status, code, msg, request.getRequestURI(), null));
    }
}
