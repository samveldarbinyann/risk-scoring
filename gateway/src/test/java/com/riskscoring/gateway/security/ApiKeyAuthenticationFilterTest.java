package com.riskscoring.gateway.security;

import com.riskscoring.gateway.service.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiKeyAuthenticationFilterTest {

    @Mock
    private ApiKeyService apiKeyService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    private ApiKeyAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new ApiKeyAuthenticationFilter(apiKeyService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doesNotAuthenticateWhenHeaderIsAbsent() throws Exception {
        when(request.getHeader(ApiKeyAuthenticationFilter.API_KEY_HEADER)).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verifyNoInteractions(apiKeyService);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doesNotAuthenticateWhenHeaderIsBlank() throws Exception {
        when(request.getHeader(ApiKeyAuthenticationFilter.API_KEY_HEADER)).thenReturn("   ");

        filter.doFilterInternal(request, response, filterChain);

        verifyNoInteractions(apiKeyService);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doesNotAuthenticateWhenKeyIsNotResolvedToAnActiveKey() throws Exception {
        when(request.getHeader(ApiKeyAuthenticationFilter.API_KEY_HEADER)).thenReturn("rsk_abc123");
        when(apiKeyService.resolveActiveKey("rsk_abc123")).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void authenticatesWithApiRoleWhenKeyResolves() throws Exception {
        ApiKeyPrincipal principal = new ApiKeyPrincipal(UUID.randomUUID(), UUID.randomUUID());
        when(request.getHeader(ApiKeyAuthenticationFilter.API_KEY_HEADER)).thenReturn(" rsk_abc123 ");
        when(apiKeyService.resolveActiveKey("rsk_abc123")).thenReturn(Optional.of(principal));

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertThat(authentication.getPrincipal()).isEqualTo(principal);
        assertThat(authentication.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_API");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void skipsResolutionWhenAlreadyAuthenticated() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("existing", null));

        filter.doFilterInternal(request, response, filterChain);

        verifyNoInteractions(apiKeyService);
        verify(request, never()).getHeader(ApiKeyAuthenticationFilter.API_KEY_HEADER);
        verify(filterChain).doFilter(request, response);
    }
}
