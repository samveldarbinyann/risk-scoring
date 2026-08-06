package com.riskscoring.gateway.controller.support;

import com.riskscoring.gateway.security.ApiKeyAuthenticationFilter;
import com.riskscoring.gateway.security.JwtAuthenticationFilter;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @WebMvcTest} slice for a single gateway controller. Excludes the JWT/API-key
 * servlet filters (their real work is exercised in {@code addFilters = false}, and
 * loading the filter beans would otherwise pull in {@code TokenService}/{@code ApiKeyService}
 * for no reason) and disables filter execution on the {@link org.springframework.test.web.servlet.MockMvc}
 * instance. {@code @AuthenticationPrincipal} resolution is provided by
 * {@link TestSecurityMvcConfigurer}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@WebMvcTest(excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
        classes = {JwtAuthenticationFilter.class, ApiKeyAuthenticationFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
public @interface GatewayControllerTest {

    @AliasFor(annotation = WebMvcTest.class, attribute = "controllers")
    Class<?>[] value() default {};
}
