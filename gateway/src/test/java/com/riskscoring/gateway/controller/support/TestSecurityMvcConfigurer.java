package com.riskscoring.gateway.controller.support;

import lombok.NonNull;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * {@code @WebMvcTest} slices never load {@code @EnableWebSecurity}, so the real
 * {@code AuthenticationPrincipalArgumentResolver} is never registered. This stands in
 * for it, reading the principal straight off {@code SecurityContextHolder}.
 */
@Component
public class TestSecurityMvcConfigurer implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(@NonNull List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(@NonNull MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(@NonNull MethodParameter parameter,
                                          ModelAndViewContainer mavContainer,
                                          @NonNull NativeWebRequest webRequest,
                                          WebDataBinderFactory binderFactory) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                return authentication == null ? null : authentication.getPrincipal();
            }
        });
    }
}
