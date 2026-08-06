package com.riskscoring.gateway.controller;

import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.ChainFamily;
import com.riskscoring.common.model.ChainSupport;
import com.riskscoring.common.model.ScanTarget;
import com.riskscoring.gateway.controller.support.AbstractControllerTest;
import com.riskscoring.gateway.controller.support.GatewayControllerTest;
import com.riskscoring.gateway.dto.ChainCandidate;
import com.riskscoring.gateway.dto.ChainCandidatesResponse;
import com.riskscoring.gateway.dto.ChainView;
import com.riskscoring.gateway.service.ChainService;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@GatewayControllerTest(ChainController.class)
class ChainControllerTest extends AbstractControllerTest {

    @MockitoBean
    private ChainService chainService;

    @Test
    void registryReturnsAllChains() throws Exception {
        given(chainService.registry()).willReturn(List.of(
                new ChainView(Chain.ETHEREUM, ChainFamily.EVM, "Ethereum", "ETH", 18, 1, ChainSupport.SUPPORTED)));

        mockMvc.perform(get("/api/chains"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].chain").value("ETHEREUM"))
                .andExpect(jsonPath("$[0].support").value("SUPPORTED"));
    }

    @Test
    void candidatesReturnsMatchingChains() throws Exception {
        String target = "0xabc";
        given(chainService.candidatesFor(target)).willReturn(new ChainCandidatesResponse(
                target, List.of(new ChainCandidate(Chain.ETHEREUM, ScanTarget.ADDRESS, target))));

        mockMvc.perform(get("/api/chains/candidates").param("target", target))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.target").value(target))
                .andExpect(jsonPath("$.candidates[0].chain").value("ETHEREUM"));
    }

    @Test
    void candidatesWorksWithoutTargetParam() throws Exception {
        given(chainService.candidatesFor(null)).willReturn(new ChainCandidatesResponse(null, List.of()));

        mockMvc.perform(get("/api/chains/candidates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidates").isEmpty());
    }
}
