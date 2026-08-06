package com.riskscoring.gateway.model;

import com.riskscoring.common.model.ChainFamily;
import com.riskscoring.common.model.ScanTarget;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ChainTargetFormatTest {

    @Test
    void evmClassifiesFortyHexCharsAsAddress() {
        String address = "0x" + "a".repeat(40);

        assertThat(ChainTargetFormat.EVM.classify(address)).contains(ScanTarget.ADDRESS);
    }

    @Test
    void evmClassifiesSixtyFourHexCharsAsTransaction() {
        String hash = "0x" + "a".repeat(64);

        assertThat(ChainTargetFormat.EVM.classify(hash)).contains(ScanTarget.TRANSACTION);
    }

    @Test
    void evmNormalizesToLowercase() {
        String mixedCase = "0x" + "A".repeat(40);

        assertThat(ChainTargetFormat.EVM.normalize(mixedCase)).isEqualTo("0x" + "a".repeat(40));
    }

    @Test
    void bitcoinClassifiesLegacyAddressFormat() {
        assertThat(ChainTargetFormat.BITCOIN.classify("1BvBMSEYstWetqTFn5Au4m4GFg7xJaNVN2")).contains(ScanTarget.ADDRESS);
    }

    @Test
    void bitcoinClassifiesLowercaseBech32Address() {
        assertThat(ChainTargetFormat.BITCOIN.classify("bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq"))
                .contains(ScanTarget.ADDRESS);
    }

    @Test
    void bitcoinClassifiesUppercaseBech32Address() {
        assertThat(ChainTargetFormat.BITCOIN.classify("BC1QAR0SRRR7XFKVY5L643LYDNW9RE59GTZZWF5MDQ"))
                .contains(ScanTarget.ADDRESS);
    }

    @Test
    void bitcoinClassifiesSixtyFourHexAsTransaction() {
        assertThat(ChainTargetFormat.BITCOIN.classify("a".repeat(64))).contains(ScanTarget.TRANSACTION);
    }

    @Test
    void bitcoinNormalizationDoesNotChangeCase() {
        String value = "  1BvBMSEYstWetqTFn5Au4m4GFg7xJaNVN2  ";

        assertThat(ChainTargetFormat.BITCOIN.normalize(value)).isEqualTo("1BvBMSEYstWetqTFn5Au4m4GFg7xJaNVN2");
    }

    @Test
    void solanaClassifiesFortyFourBase58CharsAsAddress() {
        assertThat(ChainTargetFormat.SOLANA.classify("A".repeat(44))).contains(ScanTarget.ADDRESS);
    }

    @Test
    void solanaClassifiesEightySevenBase58CharsAsTransaction() {
        assertThat(ChainTargetFormat.SOLANA.classify("A".repeat(87))).contains(ScanTarget.TRANSACTION);
    }

    @Test
    void tronClassifiesTPrefixedAddress() {
        assertThat(ChainTargetFormat.TRON.classify("T" + "A".repeat(33))).contains(ScanTarget.ADDRESS);
    }

    @Test
    void tronClassifiesSixtyFourHexAsTransaction() {
        assertThat(ChainTargetFormat.TRON.classify("a".repeat(64))).contains(ScanTarget.TRANSACTION);
    }

    @Test
    void tonDelegatesNormalizationToTonTargets() {
        String raw = "0:" + "a".repeat(64);

        assertThat(ChainTargetFormat.TON.normalize(raw)).isEqualTo(TonTargets.normalize(raw));
    }

    @Test
    void suiClassifiesSixtyFourHexWithPrefixAsAddress() {
        String address = "0x" + "a".repeat(64);

        assertThat(ChainTargetFormat.SUI.classify(address)).contains(ScanTarget.ADDRESS);
    }

    @Test
    void suiClassifiesSolanaShapedBase58AsTransaction() {
        // SUI has no declared transaction format of its own; its "transaction" pattern mirrors
        // Solana's base58 address shape. This test locks in that intentional-looking quirk.
        assertThat(ChainTargetFormat.SUI.classify("A".repeat(44))).contains(ScanTarget.TRANSACTION);
    }

    @Test
    void classifyReturnsEmptyForNonMatchingInput() {
        assertThat(ChainTargetFormat.EVM.classify("not-a-valid-target")).isEqualTo(Optional.empty());
    }

    @Test
    void normalizeReturnsEmptyStringForNullInput() {
        assertThat(ChainTargetFormat.EVM.normalize(null)).isEmpty();
    }

    @Test
    void ofReturnsMatchingFormatForEveryChainFamily() {
        for (ChainFamily family : ChainFamily.values()) {
            assertThat(ChainTargetFormat.of(family).family()).isEqualTo(family);
        }
    }
}
