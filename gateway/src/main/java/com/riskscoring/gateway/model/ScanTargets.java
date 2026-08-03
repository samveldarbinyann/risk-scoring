package com.riskscoring.gateway.model;

import com.riskscoring.common.model.Chain;
import com.riskscoring.gateway.exception.TargetChainMismatchException;
import com.riskscoring.gateway.exception.UnrecognizedTargetException;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public final class ScanTargets {

    public static final int MAX_LENGTH = 128;

    private static final Comparator<TargetMatch> CANDIDATE_ORDER =
            Comparator.<TargetMatch, Boolean>comparing(match -> !match.chain().scannable())
                    .thenComparing(match -> match.chain().ordinal());

    private ScanTargets() {
    }

    public static List<TargetMatch> classify(String raw) {
        List<TargetMatch> matches = Arrays.stream(ChainTargetFormat.values())
                .flatMap(format -> format.classify(raw).stream()
                        .flatMap(targetType -> Chain.of(format.family()).stream()
                                .map(chain -> new TargetMatch(chain, targetType, format.normalize(raw)))))
                .sorted(CANDIDATE_ORDER)
                .toList();

        if (matches.isEmpty()) {
            throw new UnrecognizedTargetException(raw);
        }

        return matches;
    }

    public static TargetMatch require(String raw, Chain chain) {
        return classify(raw).stream()
                .filter(match -> match.chain() == chain)
                .findFirst()
                .orElseThrow(() -> new TargetChainMismatchException(raw, chain));
    }
}
