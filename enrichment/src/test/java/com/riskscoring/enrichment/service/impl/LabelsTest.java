package com.riskscoring.enrichment.service.impl;

import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.FlaggedExposure;
import com.riskscoring.common.model.LabelCategory;
import com.riskscoring.common.model.TransferDirection;
import com.riskscoring.enrichment.entity.Label;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LabelsTest {

    private static final String ADDRESS = "0xabc";

    private final Labels labels = new Labels();

    @Test
    void flaggedReturnsLabelWhenCategoryIsSanction() {
        Label label = label(LabelCategory.SANCTION);
        Map<String, Label> byAddress = Map.of(ADDRESS, label);

        Optional<Label> result = labels.flagged(byAddress, ADDRESS);

        assertThat(result).contains(label);
    }

    @Test
    void flaggedReturnsLabelWhenCategoryIsMixer() {
        Label label = label(LabelCategory.MIXER);
        Map<String, Label> byAddress = Map.of(ADDRESS, label);

        Optional<Label> result = labels.flagged(byAddress, ADDRESS);

        assertThat(result).contains(label);
    }

    @Test
    void flaggedReturnsEmptyWhenCategoryIsExchange() {
        Map<String, Label> byAddress = Map.of(ADDRESS, label(LabelCategory.EXCHANGE));

        Optional<Label> result = labels.flagged(byAddress, ADDRESS);

        assertThat(result).isEmpty();
    }

    @Test
    void flaggedReturnsEmptyWhenAddressNotPresent() {
        Optional<Label> result = labels.flagged(Map.of(), ADDRESS);

        assertThat(result).isEmpty();
    }

    @Test
    void hasCategoryReturnsTrueWhenCategoryMatches() {
        Map<String, Label> byAddress = Map.of(ADDRESS, label(LabelCategory.MIXER));

        assertThat(labels.hasCategory(byAddress, ADDRESS, LabelCategory.MIXER)).isTrue();
    }

    @Test
    void hasCategoryReturnsFalseWhenCategoryDiffers() {
        Map<String, Label> byAddress = Map.of(ADDRESS, label(LabelCategory.MIXER));

        assertThat(labels.hasCategory(byAddress, ADDRESS, LabelCategory.SANCTION)).isFalse();
    }

    @Test
    void hasCategoryReturnsFalseWhenAddressNotPresent() {
        assertThat(labels.hasCategory(Map.of(), ADDRESS, LabelCategory.MIXER)).isFalse();
    }

    @Test
    void toExposureMapsAllFieldsFromLabel() {
        Label label = label(LabelCategory.SANCTION);

        FlaggedExposure exposure = labels.toExposure(label, ADDRESS, TransferDirection.OUT, 2, "500");

        assertThat(exposure).isEqualTo(new FlaggedExposure(
                ADDRESS, LabelCategory.SANCTION, label.getName(), label.getSource(), TransferDirection.OUT, 2, "500"));
    }

    @Test
    void isRoundAmountReturnsTrueForWholeBitcoinUnit() {
        BigInteger oneBitcoin = Chain.BITCOIN.nativeUnit();

        assertThat(labels.isRoundAmount(oneBitcoin, Chain.BITCOIN)).isTrue();
    }

    @Test
    void isRoundAmountReturnsTrueForWholeEthereumUnit() {
        BigInteger oneEther = Chain.ETHEREUM.nativeUnit().multiply(BigInteger.valueOf(3));

        assertThat(labels.isRoundAmount(oneEther, Chain.ETHEREUM)).isTrue();
    }

    @Test
    void isRoundAmountReturnsFalseForZero() {
        assertThat(labels.isRoundAmount(BigInteger.ZERO, Chain.BITCOIN)).isFalse();
    }

    @Test
    void isRoundAmountReturnsFalseWhenNotMultipleOfNativeUnit() {
        BigInteger notRound = Chain.SOLANA.nativeUnit().add(BigInteger.ONE);

        assertThat(labels.isRoundAmount(notRound, Chain.SOLANA)).isFalse();
    }

    @Test
    void isRoundAmountReturnsFalseForNegativeValue() {
        BigInteger negative = Chain.BITCOIN.nativeUnit().negate();

        assertThat(labels.isRoundAmount(negative, Chain.BITCOIN)).isFalse();
    }

    @Test
    void percentComputesShareOfTotal() {
        assertThat(labels.percent(BigInteger.valueOf(25), BigInteger.valueOf(100))).isEqualTo(25);
    }

    @Test
    void percentReturnsZeroWhenTotalIsZero() {
        assertThat(labels.percent(BigInteger.valueOf(25), BigInteger.ZERO)).isEqualTo(0);
    }

    @Test
    void percentTruncatesInsteadOfRounding() {
        // 499/1000 = 49.9% must truncate to 49, not round to 50.
        assertThat(labels.percent(BigInteger.valueOf(499), BigInteger.valueOf(1000))).isEqualTo(49);
    }

    private static Label label(LabelCategory category) {
        return Label.builder()
                .id(UUID.randomUUID())
                .chain(Chain.ETHEREUM)
                .address(ADDRESS)
                .category(category)
                .name("Test Label")
                .source("Test Source")
                .updatedAt(Instant.now())
                .build();
    }
}
