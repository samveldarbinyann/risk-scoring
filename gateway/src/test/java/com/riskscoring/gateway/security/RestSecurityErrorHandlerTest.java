package com.riskscoring.gateway.security;

import com.riskscoring.gateway.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.servlet.LocaleResolver;
import tools.jackson.databind.ObjectMapper;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestSecurityErrorHandlerTest {

    private static final Locale LOCALE = Locale.ENGLISH;

    @Mock
    private MessageSource messageSource;
    @Mock
    private LocaleResolver localeResolver;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private StringWriter writer;

    private RestSecurityErrorHandler handler;

    @BeforeEach
    void setUp() throws Exception {
        handler = new RestSecurityErrorHandler(messageSource, localeResolver, objectMapper);
        writer = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(writer));
        when(localeResolver.resolveLocale(request)).thenReturn(LOCALE);
    }

    @Test
    void commenceWritesUnauthorizedResponse() throws Exception {
        when(messageSource.getMessage(eq("error.unauthorized"), any(), eq(LOCALE))).thenReturn("Unauthorized");

        handler.commence(request, response, new BadCredentialsException("bad credentials"));

        verify(response).setStatus(401);
        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("UTF-8");
        ErrorResponse body = objectMapper.readValue(writer.toString(), ErrorResponse.class);
        assertThat(body.error()).isEqualTo("UNAUTHORIZED");
        assertThat(body.message()).isEqualTo("Unauthorized");
    }

    @Test
    void handleWritesForbiddenResponse() throws Exception {
        when(messageSource.getMessage(eq("error.forbidden"), any(), eq(LOCALE))).thenReturn("Forbidden");

        handler.handle(request, response, new AccessDeniedException("denied"));

        verify(response).setStatus(403);
        ErrorResponse body = objectMapper.readValue(writer.toString(), ErrorResponse.class);
        assertThat(body.error()).isEqualTo("FORBIDDEN");
        assertThat(body.message()).isEqualTo("Forbidden");
    }
}
