package com.riskscoring.gateway.security;

import com.riskscoring.gateway.service.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-Api-Key";
    public static final String API_ROLE = "API";
    public static final String API_PATH_PREFIX = "/api/v1/";

    private static final String ROLE_PREFIX = "ROLE_";

    private final ApiKeyService apiKeyService;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !request.getRequestURI().startsWith(API_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            apiKey(request)
                    .flatMap(apiKeyService::resolveActiveKey)
                    .ifPresent(ApiKeyAuthenticationFilter::authenticate);
        }

        filterChain.doFilter(request, response);
    }

    private Optional<String> apiKey(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader(API_KEY_HEADER))
                .map(String::trim)
                .filter(key -> !key.isEmpty());
    }

    private static void authenticate(ApiKeyPrincipal principal) {
        var authority = new SimpleGrantedAuthority(ROLE_PREFIX + API_ROLE);
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, List.of(authority));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
