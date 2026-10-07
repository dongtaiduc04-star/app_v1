package com.getlinkdtd.auth.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

final class SecurityProblemWriter {
    private SecurityProblemWriter() {
    }

    static void write(
            HttpServletResponse response,
            int status,
            String title,
            String code,
            String detail) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write("{\"title\":\"" + title
                + "\",\"status\":" + status
                + ",\"code\":\"" + code
                + "\",\"detail\":\"" + detail
                + "\",\"requestId\":\"" + UUID.randomUUID() + "\"}");
    }
}
