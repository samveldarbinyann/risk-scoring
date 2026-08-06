package com.riskscoring.gateway.security;

import com.riskscoring.gateway.model.UserRole;
import com.riskscoring.gateway.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private TokenService tokenService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(tokenService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doesNotAuthenticateWhenAuthorizationHeaderIsAbsent() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verifyNoInteractions(tokenService);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doesNotAuthenticateWhenHeaderIsNotBearer() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic xyz");

        filter.doFilterInternal(request, response, filterChain);

        verifyNoInteractions(tokenService);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doesNotAuthenticateWhenBearerTokenIsBlank() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer    ");

        filter.doFilterInternal(request, response, filterChain);

        verifyNoInteractions(tokenService);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doesNotAuthenticateWhenTokenDoesNotResolve() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer token123");
        when(tokenService.resolveAccessToken("token123")).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void authenticatesWithRoleDerivedFromUserWhenTokenResolves() throws Exception {
        AuthenticatedUser user = new AuthenticatedUser(UUID.randomUUID(), "jane", UserRole.ADMIN);
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer token123");
        when(tokenService.resolveAccessToken("token123")).thenReturn(Optional.of(user));

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication.getPrincipal()).isEqualTo(user);
        assertThat(authentication.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_ADMIN");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void overwritesExistingAuthenticationWhenValidTokenIsPresent() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("existing", null));
        AuthenticatedUser user = new AuthenticatedUser(UUID.randomUUID(), "jane", UserRole.USER);
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer token123");
        when(tokenService.resolveAccessToken("token123")).thenReturn(Optional.of(user));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(user);
    }
}
