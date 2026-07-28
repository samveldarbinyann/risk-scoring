package com.riskscoring.riskai.service.impl;

import com.riskscoring.common.model.EvidenceBundle;
import com.riskscoring.common.model.Language;
import com.riskscoring.riskai.service.PromptBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

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
            - txCount and counterparties are what was actually observed in a capped sample of the most
              recent activity. When sampleTruncated is true the address is busier than shown: treat
              txCount as a lower bound and never conclude the address is low-activity or dormant.
            - totalValueWei covers native-currency transfers only. A counterparty with value "0" may
              still have moved tokens, so a zero amount is not evidence that nothing was transferred.
            - balanceWei and tokenBalances are the wallet's CURRENT holdings (a snapshot right now), not
              a transfer amount — a large balance is not proof of clean or dirty funds by itself.
            - txCount24h is how many observed transfers happened in the last 24 hours. A sudden burst
              right before or after suspicious exposure is more telling than the raw count alone.

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

    private static final String LANGUAGE_INSTRUCTION_TEMPLATE = """

            Write the values of "explanation", "decisiveSignals" and "manualChecks" in %s.
            Keep "riskLevel" exactly as one of LOW, MEDIUM, HIGH, CRITICAL — never translate it.
            """;

    private static final Map<Language, String> LANGUAGE_NAMES = Map.of(
            Language.EN, "English",
            Language.RU, "Russian"
    );

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
    public String systemPrompt(Language language) {
        return SYSTEM_PROMPT + LANGUAGE_INSTRUCTION_TEMPLATE.formatted(LANGUAGE_NAMES.get(language));
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
