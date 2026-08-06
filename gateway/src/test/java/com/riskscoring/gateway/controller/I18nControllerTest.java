package com.riskscoring.gateway.controller;

import com.riskscoring.gateway.controller.support.AbstractControllerTest;
import com.riskscoring.gateway.controller.support.GatewayControllerTest;
import com.riskscoring.gateway.service.I18nService;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@GatewayControllerTest(I18nController.class)
class I18nControllerTest extends AbstractControllerTest {

    @MockitoBean
    private I18nService i18nService;

    @Test
    void getMessagesReturnsResolvedMessages() throws Exception {
        given(i18nService.messagesFor(any())).willReturn(Map.of("error.unexpected", "Unexpected error"));

        mockMvc.perform(get("/api/i18n"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['error.unexpected']").value("Unexpected error"));
    }
}
