package com.riskscoring.riskai.service.impl;

import com.riskscoring.common.model.EvidenceBundle;
import com.riskscoring.riskai.service.PromptBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class PromptBuilderImpl implements PromptBuilder {

    private static final String SYSTEM_PROMPT = """
            You are a blockchain compliance analyst. You assess the risk of a crypto address
            based ONLY on the structured evidence provided to you.

            Hard rules:
            - Use only the facts in the evidence bundle. Never invent transactions, labels or amounts.
            - If the evidence is thin, say so and lower your confidence instead of guessing.
            - Weigh conflicting signals against each other rather than reacting to the worst one.

            Domain rules you must respect:
            - A SANCTION label (source "OFAC SDN") is a hard flag. Direct exposure at hops 0-1 is severe.
            - A MIXER label is a soft flag. Tornado Cash was removed from the OFAC SDN list in March 2025,
              so mixer usage is a money-laundering signal, NOT a sanctions violation. Never call it sanctioned.
            - An EXCHANGE label is context and usually lowers suspicion: it indicates a KYC-bearing venue.
            - A young wallet is only suspicious together with other signals. An old, active wallet with
              clean counterparties is normally LOW even if it has many transactions.
            - Direction matters: receiving funds FROM a flagged address is worse than sending TO it.

            Scoring scale:
            - LOW 0-25, MEDIUM 26-50, HIGH 51-80, CRITICAL 81-100.
            - The score must be consistent with the risk level you assign.

            Respond with a single valid JSON object and nothing else, in exactly this shape:
            {
              "riskLevel": "LOW" | "MEDIUM" | "HIGH" | "CRITICAL",
              "score": <integer 0-100>,
              "explanation": "<2-4 sentences in plain language, naming the signals that decided the outcome>",
              "decisiveSignals": ["<short phrase>", ...],
              "manualChecks": ["<what a human should verify>", ...]
            }
            """;

    private static final String USER_PROMPT_TEMPLATE = """
            Assess this address. Evidence bundle:

            %s
            """;

    private static final String RETRY_PROMPT_TEMPLATE = """
            Your previous answer was rejected: %s

            Previous answer:
            %s

            Return a corrected JSON object for the same evidence bundle. Output JSON only.

            %s
            """;

    private final ObjectMapper objectMapper;

    @Override
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }

    @Override
    public String userPrompt(EvidenceBundle evidence) {
        return USER_PROMPT_TEMPLATE.formatted(asJson(evidence));
    }

    @Override
    public String retryPrompt(EvidenceBundle evidence, String invalidResponse, String reason) {
        return RETRY_PROMPT_TEMPLATE.formatted(reason, invalidResponse, asJson(evidence));
    }

    private String asJson(EvidenceBundle evidence) {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(evidence);
    }
}
