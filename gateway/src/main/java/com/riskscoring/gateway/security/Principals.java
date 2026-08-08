package com.riskscoring.gateway.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class Principals {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ROLE_PREFIX = "ROLE_";

    private Principals() {
    }

    public static Optional<String> bearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }

        return Optional.of(authorizationHeader.substring(BEARER_PREFIX.length()))
                .filter(token -> !token.isBlank());
    }

    public static UsernamePasswordAuthenticationToken authentication(Object principal, String role) {
        return new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority(ROLE_PREFIX + role)));
    }

    public static UUID requesterId(Principal principal) {
        return principal instanceof Authentication authentication
                && authentication.getPrincipal() instanceof ScanRequester requester
                ? requester.userId()
                : null;
    }
}
