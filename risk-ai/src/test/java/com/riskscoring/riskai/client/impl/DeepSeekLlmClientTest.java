package com.riskscoring.riskai.client.impl;

import com.riskscoring.riskai.client.dto.ChatCompletionRequest;
import com.riskscoring.riskai.client.dto.ChatCompletionResponse;
import com.riskscoring.riskai.client.dto.ChatMessage;
import com.riskscoring.riskai.config.RiskAiProperties;
import com.riskscoring.riskai.exception.LlmException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class DeepSeekLlmClientTest {

    private static final String COMPLETIONS_PATH = "/chat/completions";
    private static final RiskAiProperties PROPERTIES = new RiskAiProperties(
            new RiskAiProperties.Llm("http://localhost", "test-key", "deepseek-chat", 0.2,
                    Duration.ofSeconds(5), Duration.ofSeconds(30)),
            2, "v1");

    @Mock
    private RestClient llmRestClient;

    private RestClient.RequestBodyUriSpec uriSpec;
    private RestClient.RequestBodySpec bodySpec;
    private RestClient.ResponseSpec responseSpec;

    private DeepSeekLlmClient client;

    @BeforeEach
    void setUp() {
        uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        bodySpec = mock(RestClient.RequestBodySpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);
        client = new DeepSeekLlmClient(llmRestClient, PROPERTIES);
    }

    private void stubPostChain() {
        when(llmRestClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(COMPLETIONS_PATH)).thenReturn(bodySpec);
        when(bodySpec.body(any(Object.class))).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void completeSendsRequestWithSystemAndUserMessagesModelAndTemperature() {
        stubPostChain();
        when(responseSpec.body(ChatCompletionResponse.class)).thenReturn(response("content"));

        client.complete("system prompt", "user prompt");

        ArgumentCaptor<ChatCompletionRequest> captor = ArgumentCaptor.forClass(ChatCompletionRequest.class);
        verify(bodySpec).body(captor.capture());
        ChatCompletionRequest request = captor.getValue();
        assertThat(request.model()).isEqualTo("deepseek-chat");
        assertThat(request.temperature()).isEqualTo(0.2);
        assertThat(request.messages()).containsExactly(
                ChatMessage.system("system prompt"), ChatMessage.user("user prompt"));
    }

    @Test
    void completeSendsRequestAsNonStreamingJsonObjectFormat() {
        stubPostChain();
        when(responseSpec.body(ChatCompletionResponse.class)).thenReturn(response("content"));

        client.complete("system prompt", "user prompt");

        ArgumentCaptor<ChatCompletionRequest> captor = ArgumentCaptor.forClass(ChatCompletionRequest.class);
        verify(bodySpec).body(captor.capture());
        ChatCompletionRequest request = captor.getValue();
        assertThat(request.stream()).isFalse();
        assertThat(request.responseFormat()).isEqualTo(ChatCompletionRequest.ResponseFormat.jsonObject());
    }

    @Test
    void completeReturnsFirstChoiceMessageContent() {
        stubPostChain();
        when(responseSpec.body(ChatCompletionResponse.class)).thenReturn(response("the answer"));

        String result = client.complete("system prompt", "user prompt");

        assertThat(result).isEqualTo("the answer");
    }

    @Test
    void completeThrowsLlmExceptionWhenResponseIsNull() {
        stubPostChain();
        when(responseSpec.body(ChatCompletionResponse.class)).thenReturn(null);

        assertThatThrownBy(() -> client.complete("system prompt", "user prompt"))
                .isInstanceOf(LlmException.class);
    }

    @Test
    void completeThrowsLlmExceptionWhenChoicesIsEmpty() {
        stubPostChain();
        when(responseSpec.body(ChatCompletionResponse.class))
                .thenReturn(new ChatCompletionResponse(List.of()));

        assertThatThrownBy(() -> client.complete("system prompt", "user prompt"))
                .isInstanceOf(LlmException.class);
    }

    @Test
    void modelReturnsConfiguredModelName() {
        assertThat(client.model()).isEqualTo("deepseek-chat");
    }

    private static ChatCompletionResponse response(String content) {
        ChatCompletionResponse.Choice choice =
                new ChatCompletionResponse.Choice(new ChatMessage("assistant", content));
        return new ChatCompletionResponse(List.of(choice));
    }
}
