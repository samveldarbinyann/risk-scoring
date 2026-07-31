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

@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-Api-Key";
    private static final String API_ROLE = "ROLE_API";

    private final ApiKeyService apiKeyService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String rawKey = request.getHeader(API_KEY_HEADER);
            apiKeyService.resolveActiveKey(rawKey)
                    .ifPresent(ApiKeyAuthenticationFilter::authenticate);
        }

        filterChain.doFilter(request, response);
    }

    private static void authenticate(ApiKeyPrincipal principal) {
        var authentication = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority(API_ROLE)));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
