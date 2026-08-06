package com.riskscoring.riskai.service.impl;

import com.riskscoring.common.model.AddressEvidence;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.EvidenceBundle;
import com.riskscoring.common.model.Heuristics;
import com.riskscoring.common.model.Language;
import com.riskscoring.common.model.TransactionEvidence;
import com.riskscoring.common.model.TransactionHeuristics;
import com.riskscoring.riskai.exception.UnsupportedChainFamilyException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PromptBuilderImplTest {

    private static final Instant NOW = Instant.parse("2024-06-01T00:00:00Z");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PromptBuilderImpl builder = new PromptBuilderImpl(objectMapper);

    @Test
    void systemPromptForAddressEvidenceIncludesAddressCorePrompt() {
        String prompt = builder.systemPrompt(addressEvidence(Chain.ETHEREUM), Language.EN);

        assertThat(prompt).contains("You are a blockchain compliance analyst. You assess the risk of a crypto address");
    }

    @Test
    void systemPromptForTransactionEvidenceIncludesTransactionCorePrompt() {
        String prompt = builder.systemPrompt(transactionEvidence(Chain.ETHEREUM), Language.EN);

        assertThat(prompt).contains("You assess the risk of ONE SINGLE TRANSACTION");
    }

    @Test
    void systemPromptIncludesEvmAddressRulesForEvmChain() {
        String prompt = builder.systemPrompt(addressEvidence(Chain.ETHEREUM), Language.EN);

        assertThat(prompt).contains("tokenBalances are the wallet's token holdings at observedAt.");
    }

    @Test
    void systemPromptIncludesUtxoAddressRulesForBitcoinChain() {
        String prompt = builder.systemPrompt(addressEvidence(Chain.BITCOIN), Language.EN);

        assertThat(prompt).contains("Wallets on this chain routinely spread funds across many addresses they control");
    }

    @Test
    void systemPromptIncludesSolanaAddressRulesForSolanaChain() {
        String prompt = builder.systemPrompt(addressEvidence(Chain.SOLANA), Language.EN);

        assertThat(prompt).contains("Program, DEX-router and market-maker addresses routinely appear as counterparties on this");
    }

    @Test
    void systemPromptIncludesTronAddressRulesForTronChain() {
        String prompt = builder.systemPrompt(addressEvidence(Chain.TRON), Language.EN);

        assertThat(prompt).contains("Most economic activity on this chain is TRC20 stablecoin transfer, above all USDT.");
    }

    @Test
    void systemPromptIncludesTonAddressRulesForTonChain() {
        String prompt = builder.systemPrompt(addressEvidence(Chain.TON), Language.EN);

        assertThat(prompt).contains("Dust-spam events (unsolicited micro-transfers carrying advertising) are deliberately");
    }

    @Test
    void systemPromptIncludesEvmTransactionRulesForEvmChain() {
        String prompt = builder.systemPrompt(transactionEvidence(Chain.ETHEREUM), Language.EN);

        assertThat(prompt).contains("typical of routers, splitters and DEX aggregators");
    }

    @Test
    void systemPromptIncludesUtxoTransactionRulesForBitcoinChain() {
        String prompt = builder.systemPrompt(transactionEvidence(Chain.BITCOIN), Language.EN);

        assertThat(prompt).contains("carries role SENDER and every output address role RECIPIENT");
    }

    @Test
    void systemPromptThrowsUnsupportedChainFamilyExceptionForUnsupportedFamily() {
        assertThatThrownBy(() -> builder.systemPrompt(addressEvidence(Chain.SUI), Language.EN))
                .isInstanceOf(UnsupportedChainFamilyException.class)
                .hasMessageContaining("SUI");
    }

    @Test
    void systemPromptIncludesNativeUnitConversionForChainDecimals() {
        String prompt = builder.systemPrompt(addressEvidence(Chain.ETHEREUM), Language.EN);

        assertThat(prompt).contains("Amounts are integers in the smallest unit of ETH: 1 ETH equals 10^18 of them.");
    }

    @Test
    void systemPromptIncludesEnglishLanguageInstructionForEnglish() {
        String prompt = builder.systemPrompt(addressEvidence(Chain.ETHEREUM), Language.EN);

        assertThat(prompt).contains("Write the values of \"explanation\", \"decisiveSignals\" and \"manualChecks\" in English.");
    }

    @Test
    void systemPromptIncludesRussianLanguageInstructionForRussian() {
        String prompt = builder.systemPrompt(addressEvidence(Chain.ETHEREUM), Language.RU);

        assertThat(prompt).contains("Write the values of \"explanation\", \"decisiveSignals\" and \"manualChecks\" in Russian.");
    }

    @Test
    void userPromptForAddressEvidenceUsesAddressTemplateAndEmbedsEvidenceJson() {
        String prompt = builder.userPrompt(addressEvidence(Chain.ETHEREUM));

        assertThat(prompt).startsWith("Assess this address. Evidence bundle:");
        assertThat(prompt).contains("0xtarget");
    }

    @Test
    void userPromptForTransactionEvidenceUsesTransactionTemplateAndEmbedsEvidenceJson() {
        String prompt = builder.userPrompt(transactionEvidence(Chain.ETHEREUM));

        assertThat(prompt).startsWith("Assess this transaction. Evidence bundle:");
        assertThat(prompt).contains("0xhash");
    }

    @Test
    void retryPromptIncludesReasonPreviousResponseAndFreshEvidenceJson() {
        String prompt = builder.retryPrompt(addressEvidence(Chain.ETHEREUM), "{\"riskLevel\":null}", "riskLevel is missing");

        assertThat(prompt).contains("Your previous answer was rejected: riskLevel is missing");
        assertThat(prompt).contains("{\"riskLevel\":null}");
        assertThat(prompt).contains("0xtarget");
    }

    private static AddressEvidence addressEvidence(Chain chain) {
        return new AddressEvidence("0xtarget", chain, NOW, null, 0, 0, false, "0", List.of(), 0, List.of(), null,
                new Heuristics(null, null, false, 0, 0));
    }

    private static EvidenceBundle transactionEvidence(Chain chain) {
        return new TransactionEvidence("0xhash", chain, NOW, "0xfrom", "0xto", "0", true, NOW, 0, 0, List.of(),
                List.of(), List.of(), null,
                new TransactionHeuristics(false, true, false, false, false, false, 0));
    }
}
