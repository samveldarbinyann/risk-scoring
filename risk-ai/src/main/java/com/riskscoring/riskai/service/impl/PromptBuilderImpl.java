package com.riskscoring.riskai.service.impl;

import com.riskscoring.common.model.AddressEvidence;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.ChainFamily;
import com.riskscoring.common.model.EvidenceBundle;
import com.riskscoring.common.model.Language;
import com.riskscoring.common.model.TransactionEvidence;
import com.riskscoring.riskai.exception.UnsupportedChainFamilyException;
import com.riskscoring.riskai.service.PromptBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class PromptBuilderImpl implements PromptBuilder {

    private static final String ADDRESS_CORE_PROMPT = """
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
            - ageDays, freshWallet and fundedThenDrained are null when the address's first-ever activity
              could not be determined. Null means UNKNOWN, never "young" and never "old": say the age is
              unknown and do not let it push the verdict in either direction.
            - Direction matters: receiving funds FROM a flagged address is worse than sending TO it.
            - When sampleTruncated is true the address is busier than the counterparty list shows:
              never conclude the address is low-activity or dormant.
            - observedAt is when the on-chain data was read. Every counter and balance below describes
              the chain as of that moment, not as of now.
            - balanceNative is the wallet's holdings at observedAt, not a transfer amount — a large
              balance is not proof of clean or dirty funds by itself.
            - txCount24h is how many observed transfers happened in the 24 hours before observedAt.
              A sudden burst right before or after suspicious exposure is more telling than the raw
              count alone.
            """;

    private static final String ADDRESS_EVM_PROMPT = """
            Rules specific to this chain (account model):
            - txCount and the counterparty list come from a capped sample of the most recent activity.
              Treat txCount as a lower bound on the address's real lifetime activity.
            - totalValueNative covers native-currency transfers only. A counterparty with value "0" may
              still have moved tokens, so a zero amount is not evidence that nothing was transferred.
            - tokenBalances are the wallet's token holdings at observedAt.
            """;

    private static final String ADDRESS_UTXO_PROMPT = """
            Rules specific to this chain (UTXO model):
            - txCount is the address's lifetime confirmed transaction count, not a sample.
            - The counterparty list is built from a capped page of the most recent transactions, so it
              can be far narrower than the lifetime history even when txCount is large.
            - One transaction spends several inputs and creates several outputs, so a single transaction
              yields several counterparties. A high counterparty count is normal here and is not by
              itself suspicious.
            - Wallets on this chain routinely spread funds across many addresses they control, and each
              address is meant to be used once. This address is therefore only a slice of its owner's
              activity: never describe it as "the wallet" or infer the owner's total holdings from it.
            - tokenBalances is always empty on this chain because token holdings are not tracked here.
              Never read the empty list as "the wallet holds no tokens".
            - The address's first-ever activity is not available from this data source, so ageDays,
              freshWallet and fundedThenDrained are always null here. A large txCount alone already tells
              you the address is not new.
            """;

    private static final String TRANSACTION_CORE_PROMPT = """
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
            - "parties" lists every address touched by this transaction with its role.
            - A SANCTION label on a party at hops 0 is a hard flag. At hops 1 it is a strong signal but
              not automatically CRITICAL: value may have merely been routed past that address.
            - A MIXER label is a soft flag. Tornado Cash was removed from the OFAC SDN list in March 2025,
              so mixer usage is a money-laundering signal, NOT a sanctions violation. Never call it sanctioned.
            - An EXCHANGE label is context and usually lowers suspicion: it indicates a KYC-bearing venue.
            - roundValue (a whole number of native units) is a weak laundering hint, never decisive alone.
            - selfTransfer means from and to are the same address.
            - blockTimestamp is when the transaction was mined; observedAt is when we read the chain.
            """;

    private static final String TRANSACTION_EVM_PROMPT = """
            Rules specific to this chain (account model):
            - Roles SENDER and RECIPIENT are the transaction's own from/to (hops 0); INTERNAL_* and
              TOKEN_* roles come from internal calls and token transfers inside the same transaction
              (hops 1).
            - success=false means the transaction reverted and no value moved. That lowers the realised
              risk while still evidencing intent — say so explicitly rather than ignoring the attempt.
            - valueNative covers the native currency only. zeroValue means nothing native moved AND there
              were no token transfers. tokenOnly means value is 0 but token transfers did happen —
              never read either as "nothing was transferred" without checking tokenTransferCount.
            - fanOutInternal means the transaction fanned value out through many internal calls. That is
              typical of routers, splitters and DEX aggregators and is not by itself criminal.
            """;

    private static final String TRANSACTION_UTXO_PROMPT = """
            Rules specific to this chain (UTXO model):
            - This transaction spends several inputs and creates several outputs. Every input address
              carries role SENDER and every output address role RECIPIENT; all of them are hops 0.
              There are no nested transfers on this chain.
            - Several SENDER addresses do NOT mean several people paid: one wallet routinely spends many
              of its own inputs in a single transaction.
            - One of the RECIPIENT outputs is usually change returning to the payer, so the amount that
              actually reached a third party is smaller than valueNative, which is the sum of all
              outputs. fromAddress and toAddress are our best guess — the largest input and the largest
              non-change output — not a declared from/to pair.
            - success is always true here: transactions on this chain cannot revert. A null
              blockTimestamp means the transaction is still unconfirmed in the mempool and may never be
              mined. That is the only "not settled" state and it is NOT a failure.
            - nestedTransferCount and tokenTransferCount are always 0 on this chain and carry no
              information. The zeroValue, tokenOnly and fanOutInternal heuristics never fire here —
              judge fan-out from the parties list and distinctPartyCount instead.
            """;

    private static final String UNITS_TEMPLATE = """

            Amounts are integers in the smallest unit of %s: 1 %s equals 10^%d of them. balanceNative,
            valueNative, totalValueNative and every party amount use this unit. Convert to %s before
            describing any size in your explanation.
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

    private static final Map<ChainFamily, FamilyPromptRules> FAMILY_RULES = Map.of(
            ChainFamily.EVM, new FamilyPromptRules(ADDRESS_EVM_PROMPT, TRANSACTION_EVM_PROMPT),
            ChainFamily.BITCOIN, new FamilyPromptRules(ADDRESS_UTXO_PROMPT, TRANSACTION_UTXO_PROMPT));

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
        return domainRules(evidence)
                + units(evidence.chain())
                + VERDICT_FORMAT
                + LANGUAGE_INSTRUCTION_TEMPLATE.formatted(LANGUAGE_NAMES.get(language));
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

    private String domainRules(EvidenceBundle evidence) {
        FamilyPromptRules rules = familyRules(evidence.chain().family());

        return switch (evidence) {
            case AddressEvidence ignored -> ADDRESS_CORE_PROMPT + rules.addressRules();
            case TransactionEvidence ignored -> TRANSACTION_CORE_PROMPT + rules.transactionRules();
        };
    }

    private FamilyPromptRules familyRules(ChainFamily family) {
        FamilyPromptRules rules = FAMILY_RULES.get(family);
        if (rules == null) {
            throw new UnsupportedChainFamilyException(family);
        }
        return rules;
    }

    private String units(Chain chain) {
        return UNITS_TEMPLATE.formatted(
                chain.nativeSymbol(), chain.nativeSymbol(), chain.nativeDecimals(), chain.nativeSymbol());
    }

    private String asJson(EvidenceBundle evidence) {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(evidence);
    }

    private record FamilyPromptRules(String addressRules, String transactionRules) {
    }
}
