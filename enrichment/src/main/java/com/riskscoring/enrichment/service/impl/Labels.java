package com.riskscoring.enrichment.service.impl;

import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.FlaggedExposure;
import com.riskscoring.common.model.LabelCategory;
import com.riskscoring.common.model.TransferDirection;
import com.riskscoring.enrichment.entity.Label;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class Labels {

    public static final int PERCENT = 100;

    private static final Set<LabelCategory> FLAGGED_CATEGORIES =
            EnumSet.of(LabelCategory.SANCTION, LabelCategory.MIXER);

    public Optional<Label> flagged(Map<String, Label> byAddress, String address) {
        return Optional.ofNullable(byAddress.get(address))
                .filter(label -> FLAGGED_CATEGORIES.contains(label.getCategory()));
    }

    public boolean hasCategory(Map<String, Label> byAddress, String address, LabelCategory category) {
        return Optional.ofNullable(byAddress.get(address))
                .filter(label -> label.getCategory() == category)
                .isPresent();
    }

    public FlaggedExposure toExposure(Label label, String address, TransferDirection direction,
                                      int hops, String valueNative) {
        return new FlaggedExposure(
                address,
                label.getCategory(),
                label.getName(),
                label.getSource(),
                direction,
                hops,
                valueNative);
    }

    public boolean isRoundAmount(BigInteger value, Chain chain) {
        return value.signum() > 0 && value.mod(chain.nativeUnit()).signum() == 0;
    }

    public int percent(BigInteger part, BigInteger total) {
        return total.signum() == 0
                ? 0
                : part.multiply(BigInteger.valueOf(PERCENT)).divide(total).intValue();
    }
}
