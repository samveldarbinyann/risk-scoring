package com.riskscoring.gateway.util;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientIpResolverTest {

    @Mock
    private HttpServletRequest request;

    @Test
    void resolveFallsBackToRemoteAddrWhenHeaderIsAbsent() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");

        assertThat(ClientIpResolver.resolve(request)).isEqualTo("192.168.1.1");
    }

    @Test
    void resolveFallsBackToRemoteAddrWhenHeaderIsBlank() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("   ");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");

        assertThat(ClientIpResolver.resolve(request)).isEqualTo("192.168.1.1");
    }

    @Test
    void resolveReturnsSingleForwardedIpTrimmed() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("  203.0.113.10  ");

        assertThat(ClientIpResolver.resolve(request)).isEqualTo("203.0.113.10");
    }

    @Test
    void resolveReturnsFirstIpFromCommaSeparatedList() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.10, 198.51.100.5");

        assertThat(ClientIpResolver.resolve(request)).isEqualTo("203.0.113.10");
    }

    @Test
    void resolveTrimsWhitespaceAroundTheFirstIp() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("  203.0.113.10  ,198.51.100.5");

        assertThat(ClientIpResolver.resolve(request)).isEqualTo("203.0.113.10");
    }
}
