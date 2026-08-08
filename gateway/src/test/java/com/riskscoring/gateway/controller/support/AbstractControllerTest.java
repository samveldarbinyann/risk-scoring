package com.riskscoring.gateway.controller.support;

import com.riskscoring.gateway.security.ApiKeyAuthenticationFilter;
import com.riskscoring.gateway.security.ApiKeyPrincipal;
import com.riskscoring.gateway.security.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

public abstract class AbstractControllerTest {

    protected static final String PEER_IP = "127.0.0.1";

    @Autowired
    protected MockMvc mockMvc;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    protected static RequestPostProcessor authenticatedAs(AuthenticatedUser user) {
        return authenticated(user, "ROLE_" + user.role());
    }

    protected static RequestPostProcessor authenticatedAs(ApiKeyPrincipal principal) {
        return authenticated(principal, "ROLE_" + ApiKeyAuthenticationFilter.API_ROLE);
    }

    private static RequestPostProcessor authenticated(Object principal, String authority) {
        return request -> {
            var authentication = new UsernamePasswordAuthenticationToken(
                    principal, null, List.of(new SimpleGrantedAuthority(authority)));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            return request;
        };
    }
}
