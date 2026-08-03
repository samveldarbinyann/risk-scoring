package com.riskscoring.riskai.service.impl;

import com.riskscoring.common.model.AddressEvidence;
import com.riskscoring.common.model.EvidenceBundle;
import com.riskscoring.common.model.Language;
import com.riskscoring.common.model.TransactionEvidence;
import com.riskscoring.riskai.service.PromptBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class PromptBuilderImpl implements PromptBuilder {

    private static final String ADDRESS_SYSTEM_PROMPT = """
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
            - observedAt is when the on-chain data was read. Every counter and balance below describes
              the chain as of that moment, not as of now.
            - balanceWei and tokenBalances are the wallet's holdings at observedAt, not a transfer
              amount — a large balance is not proof of clean or dirty funds by itself.
            - txCount24h is how many observed transfers happened in the 24 hours before observedAt.
              A sudden burst right before or after suspicious exposure is more telling than the raw
              count alone.
            """;

    private static final String TRANSACTION_SYSTEM_PROMPT = """
            You are a blockchain compliance analyst. You assess the risk of ONE SINGLE TRANSACTION
            based ONLY on the structured evidence provided to you.

            Hard rules:
            - Use only the facts in the evidence bundle. Never invent transfers, labels or amounts.
            - You are judging this transfer, not the wallets behind it. Say what this transaction
              looks like, not what its participants are.
            - The bundle contains NO wallet history, age, balance or transaction count for any
              participant. Never draw conclusions about them. If deciding would require that history,
              put it in "manualChecks" instead of guessing.
            - Weigh conflicting signals against each other rather than reacting to the worst one.

            Domain rules you must respect:
            - "parties" lists every address touched by this transaction with its role. Roles SENDER and
              RECIPIENT are the transaction's own from/to (hops 0); INTERNAL_* and TOKEN_* roles come
              from internal calls and ERC-20 transfers inside the same transaction (hops 1).
            - A SANCTION label on a party at hops 0 is a hard flag. At hops 1 it is a strong signal but
              not automatically CRITICAL: value may have merely been routed past that address.
            - A MIXER label is a soft flag. Tornado Cash was removed from the OFAC SDN list in March 2025,
              so mixer usage is a money-laundering signal, NOT a sanctions violation. Never call it sanctioned.
            - An EXCHANGE label is context and usually lowers suspicion: it indicates a KYC-bearing venue.
            - success=false means the transaction reverted and no value moved. That lowers the realised
              risk while still evidencing intent — say so explicitly rather than ignoring the attempt.
            - valueWei covers the native currency only. zeroValue means nothing native moved AND there
              were no token transfers. tokenOnly means value is 0 but ERC-20 transfers did happen —
              never read either as "nothing was transferred" without checking erc20TransferCount.
            - fanOutInternal means the transaction fanned value out through many internal calls. That is
              typical of routers, splitters and DEX aggregators and is not by itself criminal.
            - roundValue (a whole number of native units) is a weak laundering hint, never decisive alone.
            - selfTransfer means from and to are the same address.
            - blockTimestamp is when the transaction was mined; observedAt is when we read the chain.
            """;

    private static final String VERDICT_FORMAT = """

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

    private static final String ADDRESS_USER_PROMPT = """
            Assess this address. Evidence bundle:

            %s
            """;

    private static final String TRANSACTION_USER_PROMPT = """
            Assess this transaction. Evidence bundle:

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
    public String systemPrompt(EvidenceBundle evidence, Language language) {
        String domainRules = switch (evidence) {
            case AddressEvidence ignored -> ADDRESS_SYSTEM_PROMPT;
            case TransactionEvidence ignored -> TRANSACTION_SYSTEM_PROMPT;
        };

        return domainRules + VERDICT_FORMAT + LANGUAGE_INSTRUCTION_TEMPLATE.formatted(LANGUAGE_NAMES.get(language));
    }

    @Override
    public String userPrompt(EvidenceBundle evidence) {
        String template = switch (evidence) {
            case AddressEvidence ignored -> ADDRESS_USER_PROMPT;
            case TransactionEvidence ignored -> TRANSACTION_USER_PROMPT;
        };

        return template.formatted(asJson(evidence));
    }

    @Override
    public String retryPrompt(EvidenceBundle evidence, String invalidResponse, String reason) {
        return RETRY_PROMPT_TEMPLATE.formatted(reason, invalidResponse, asJson(evidence));
    }

    private String asJson(EvidenceBundle evidence) {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(evidence);
    }
}
