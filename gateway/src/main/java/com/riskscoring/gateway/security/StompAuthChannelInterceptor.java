package com.riskscoring.gateway.security;

import com.riskscoring.gateway.config.WebSocketConfig;
import com.riskscoring.gateway.service.ScanService;
import com.riskscoring.gateway.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ROLE_PREFIX = "ROLE_";
    private static final PathMatcher PATH_MATCHER = new AntPathMatcher();

    private final TokenService tokenService;
    private final ScanService scanService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        switch (accessor.getCommand()) {
            case CONNECT -> authenticate(accessor);
            case SUBSCRIBE -> authorizeSubscription(message, accessor);
            case SEND -> throw new MessageDeliveryException(message, "Client frames are not accepted");
            case null, default -> {
            }
        }

        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        Optional.ofNullable(accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION))
                .filter(header -> header.startsWith(BEARER_PREFIX))
                .map(header -> header.substring(BEARER_PREFIX.length()))
                .filter(token -> !token.isBlank())
                .flatMap(tokenService::resolveAccessToken)
                .ifPresent(user -> accessor.setUser(new UsernamePasswordAuthenticationToken(
                        user, null, List.of(new SimpleGrantedAuthority(ROLE_PREFIX + user.role().name())))));
    }

    private void authorizeSubscription(Message<?> message, StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();

        if (destination == null || PATH_MATCHER.isPattern(destination)) {
            throw new MessageDeliveryException(message, "Subscription destination must be a single scan topic");
        }

        if (!accessible(destination, requesterId(accessor.getUser()))) {
            throw new MessageDeliveryException(message, "Subscription to %s is not allowed".formatted(destination));
        }
    }

    private boolean accessible(String destination, UUID requesterId) {
        return topicId(destination, WebSocketConfig.SCAN_GROUP_TOPIC_PREFIX)
                .map(groupId -> scanService.canAccessGroup(groupId, requesterId))
                .or(() -> topicId(destination, WebSocketConfig.SCAN_TOPIC_PREFIX)
                        .map(scanId -> scanService.canAccessScan(scanId, requesterId)))
                .orElse(false);
    }

    private static Optional<UUID> topicId(String destination, String prefix) {
        if (!destination.startsWith(prefix)) {
            return Optional.empty();
        }

        try {
            return Optional.of(UUID.fromString(destination.substring(prefix.length())));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private static UUID requesterId(Principal principal) {
        return principal instanceof UsernamePasswordAuthenticationToken token
                && token.getPrincipal() instanceof AuthenticatedUser user
                ? user.id()
                : null;
    }
}
