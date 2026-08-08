package com.riskscoring.gateway.security;

import com.riskscoring.gateway.config.WebSocketConfig;
import com.riskscoring.gateway.model.UserRole;
import com.riskscoring.gateway.service.ScanService;
import com.riskscoring.gateway.service.TokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.security.Principal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StompAuthChannelInterceptorTest {

    private static final UUID USER_ID = UUID.fromString("8dc2f3fd-f443-4481-842e-5d70a6cb4b88");
    private static final String ACCESS_TOKEN = "valid.jwt.token";

    @Mock
    private TokenService tokenService;

    @Mock
    private ScanService scanService;

    @InjectMocks
    private StompAuthChannelInterceptor interceptor;

    private final MessageChannel channel = mock(MessageChannel.class);

    @Test
    void connectWithValidBearerTokenBindsThePrincipal() {
        AuthenticatedUser user = new AuthenticatedUser(USER_ID, "alice", UserRole.USER);
        when(tokenService.resolveAccessToken(ACCESS_TOKEN)).thenReturn(Optional.of(user));

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN);
        Message<byte[]> message = message(accessor);

        interceptor.preSend(message, channel);

        Principal principal = accessor.getUser();
        assertThat(principal).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertThat(((UsernamePasswordAuthenticationToken) principal).getPrincipal()).isEqualTo(user);
    }

    @Test
    void connectWithoutAuthorizationHeaderStaysAnonymous() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);

        interceptor.preSend(message(accessor), channel);

        assertThat(accessor.getUser()).isNull();
        verifyNoInteractions(tokenService);
    }

    @Test
    void connectWithInvalidTokenStaysAnonymous() {
        when(tokenService.resolveAccessToken(ACCESS_TOKEN)).thenReturn(Optional.empty());

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN);

        interceptor.preSend(message(accessor), channel);

        assertThat(accessor.getUser()).isNull();
    }

    @Test
    void subscribeRejectsWildcardDestination() {
        assertThatThrownBy(() -> interceptor.preSend(subscribeMessage("/topic/**", null), channel))
                .isInstanceOf(MessageDeliveryException.class);

        verifyNoInteractions(scanService);
    }

    @Test
    void subscribeRejectsSingleSegmentWildcardDestination() {
        assertThatThrownBy(() -> interceptor.preSend(subscribeMessage("/topic/scan-groups/*", null), channel))
                .isInstanceOf(MessageDeliveryException.class);

        verifyNoInteractions(scanService);
    }

    @Test
    void subscribeRejectsUnknownDestination() {
        assertThatThrownBy(() -> interceptor.preSend(subscribeMessage("/topic/secrets", null), channel))
                .isInstanceOf(MessageDeliveryException.class);

        verifyNoInteractions(scanService);
    }

    @Test
    void subscribeRejectsDestinationWithMalformedId() {
        String destination = WebSocketConfig.SCAN_GROUP_TOPIC_PREFIX + "not-a-uuid";

        assertThatThrownBy(() -> interceptor.preSend(subscribeMessage(destination, null), channel))
                .isInstanceOf(MessageDeliveryException.class);

        verifyNoInteractions(scanService);
    }

    @Test
    void subscribeRejectsAnotherUsersGroupTopic() {
        UUID groupId = UUID.randomUUID();
        when(scanService.canAccessGroup(groupId, USER_ID)).thenReturn(false);
        String destination = WebSocketConfig.SCAN_GROUP_TOPIC_PREFIX + groupId;

        assertThatThrownBy(() -> interceptor.preSend(subscribeMessage(destination, authentication()), channel))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void subscribeAllowsOwnGroupTopic() {
        UUID groupId = UUID.randomUUID();
        when(scanService.canAccessGroup(groupId, USER_ID)).thenReturn(true);
        String destination = WebSocketConfig.SCAN_GROUP_TOPIC_PREFIX + groupId;

        Message<byte[]> message = subscribeMessage(destination, authentication());

        assertThat(interceptor.preSend(message, channel)).isSameAs(message);
    }

    @Test
    void subscribeAllowsAnonymousRequesterOnAnAnonymousScanTopic() {
        UUID scanId = UUID.randomUUID();
        when(scanService.canAccessScan(scanId, null)).thenReturn(true);
        String destination = WebSocketConfig.SCAN_TOPIC_PREFIX + scanId;

        Message<byte[]> message = subscribeMessage(destination, null);

        assertThat(interceptor.preSend(message, channel)).isSameAs(message);
    }

    @Test
    void subscribeRejectsAnonymousRequesterOnAnOwnedScanTopic() {
        UUID scanId = UUID.randomUUID();
        when(scanService.canAccessScan(scanId, null)).thenReturn(false);
        String destination = WebSocketConfig.SCAN_TOPIC_PREFIX + scanId;

        assertThatThrownBy(() -> interceptor.preSend(subscribeMessage(destination, null), channel))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void nonStompMessagePassesThrough() {
        Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).build();

        assertThat(interceptor.preSend(message, channel)).isSameAs(message);
        verifyNoInteractions(tokenService, scanService);
    }

    private Message<byte[]> subscribeMessage(String destination, Principal user) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setUser(user);
        return message(accessor);
    }

    private static Message<byte[]> message(StompHeaderAccessor accessor) {
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private static UsernamePasswordAuthenticationToken authentication() {
        return new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(USER_ID, "alice", UserRole.USER), null);
    }
}
