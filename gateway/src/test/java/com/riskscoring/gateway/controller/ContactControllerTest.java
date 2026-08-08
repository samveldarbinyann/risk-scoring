package com.riskscoring.gateway.controller;

import com.riskscoring.gateway.controller.support.AbstractControllerTest;
import com.riskscoring.gateway.controller.support.GatewayControllerTest;
import com.riskscoring.gateway.service.ContactService;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@GatewayControllerTest(ContactController.class)
class ContactControllerTest extends AbstractControllerTest {

    @MockitoBean
    private ContactService contactService;

    @Test
    void submitDelegatesToService() throws Exception {
        mockMvc.perform(post("/api/contact")
                        .contentType("application/json")
                        .content("""
                                {"email": "alice@example.com", "subject": "Question", "message": "Hi there"}
                                """))
                .andExpect(status().isAccepted());

        verify(contactService).submit(anyString(), any());
    }

    @Test
    void submitRejectsBlankMessage() throws Exception {
        mockMvc.perform(post("/api/contact")
                        .contentType("application/json")
                        .content("""
                                {"email": "alice@example.com", "subject": "Question", "message": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void submitIgnoresForwardedForAndKeysOnTheTransportPeer() throws Exception {
        mockMvc.perform(post("/api/contact")
                        .header("X-Forwarded-For", "203.0.113.7")
                        .contentType("application/json")
                        .content("""
                                {"email": "alice@example.com", "subject": "Question", "message": "Hi there"}
                                """))
                .andExpect(status().isAccepted());

        verify(contactService).submit(eq(PEER_IP), any());
    }
}
