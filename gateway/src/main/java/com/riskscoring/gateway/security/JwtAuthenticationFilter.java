package com.riskscoring.gateway.security;

import com.riskscoring.gateway.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        Principals.bearerToken(request.getHeader(HttpHeaders.AUTHORIZATION))
                .flatMap(tokenService::resolveAccessToken)
                .ifPresent(JwtAuthenticationFilter::authenticate);

        filterChain.doFilter(request, response);
    }

    private static void authenticate(AuthenticatedUser user) {
        SecurityContextHolder.getContext()
                .setAuthentication(Principals.authentication(user, user.role().name()));
    }
}
