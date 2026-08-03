package com.riskscoring.gateway.controller;

import com.riskscoring.gateway.dto.ChainCandidatesResponse;
import com.riskscoring.gateway.service.ChainService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chains")
@RequiredArgsConstructor
public class ChainController {

    private final ChainService chainService;

    @GetMapping("/candidates")
    public ChainCandidatesResponse candidates(@RequestParam(required = false) String target) {
        return chainService.candidatesFor(target);
    }
}
