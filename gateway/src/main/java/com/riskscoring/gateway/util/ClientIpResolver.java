package com.riskscoring.gateway.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

public final class ClientIpResolver {

    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        String forwardedFor = request.getHeader(FORWARDED_FOR_HEADER);
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
