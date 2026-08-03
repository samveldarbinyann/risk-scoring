package com.riskscoring.gateway.model;

import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.ScanTarget;
import com.riskscoring.gateway.exception.TargetChainMismatchException;
import com.riskscoring.gateway.exception.UnrecognizedTargetException;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class ScanTargets {

    public static final int MAX_LENGTH = 128;

    private static final Comparator<TargetMatch> CANDIDATE_ORDER =
            Comparator.comparing((TargetMatch match) -> match.chain().support())
                    .thenComparing(TargetMatch::chain);

    private ScanTargets() {
    }

    public static List<TargetMatch> classify(String raw) {
        List<TargetMatch> matches = Arrays.stream(ChainTargetFormat.values())
                .flatMap(format -> format.classify(raw).stream()
                        .flatMap(targetType -> matches(format, raw, targetType)))
                .sorted(CANDIDATE_ORDER)
                .toList();

        if (matches.isEmpty()) {
            throw new UnrecognizedTargetException(raw);
        }

        return matches;
    }

    public static TargetMatch require(String raw, Chain chain, ScanTarget expected) {
        ChainTargetFormat format = ChainTargetFormat.of(chain.family());

        return format.classify(raw)
                .filter(targetType -> targetType == expected)
                .map(targetType -> new TargetMatch(chain, targetType, format.normalize(raw)))
                .orElseThrow(() -> new TargetChainMismatchException(raw, chain));
    }

    private static Stream<TargetMatch> matches(ChainTargetFormat format, String raw, ScanTarget targetType) {
        String normalized = format.normalize(raw);
        return Chain.of(format.family()).stream()
                .map(chain -> new TargetMatch(chain, targetType, normalized));
    }
}
