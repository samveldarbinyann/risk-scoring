package com.riskscoring.chainingest.mapper;

import com.riskscoring.chainingest.client.dto.tonapi.TonAccountAddress;
import com.riskscoring.chainingest.client.dto.tonapi.TonAction;
import com.riskscoring.chainingest.client.dto.tonapi.TonEvent;
import com.riskscoring.chainingest.client.dto.tonapi.TonJettonTransferAction;
import com.riskscoring.chainingest.client.dto.tonapi.TonTransferAction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Component
public class TonValues implements ChainAddressValues {

    private static final String TON_TRANSFER = "TonTransfer";
    private static final String JETTON_TRANSFER = "JettonTransfer";
    private static final String SUCCEEDED = "ok";

    public String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public String address(String value) {
        return normalize(value);
    }

    public String party(TonAccountAddress account) {
        return account == null ? "" : address(account.address());
    }

    @Override
    public boolean isRoutable(String address) {
        return !address.isEmpty();
    }

    public Instant timestamp(Long epochSeconds) {
        return epochSeconds == null ? null : Instant.ofEpochSecond(epochSeconds);
    }

    public Stream<TonAction> transferActions(TonEvent event) {
        return actions(event)
                .filter(action -> TON_TRANSFER.equals(action.type()) || JETTON_TRANSFER.equals(action.type()));
    }

    public boolean succeeded(TonAction action) {
        return SUCCEEDED.equals(action.status());
    }

    public boolean succeeded(TonEvent event) {
        return actions(event).allMatch(this::succeeded);
    }

    private Stream<TonAction> actions(TonEvent event) {
        return Objects.requireNonNullElse(event.actions(), List.<TonAction>of()).stream();
    }

    public List<TonTransferAction> nativeTransfers(Stream<TonAction> actions) {
        return actions.map(TonAction::tonTransfer).filter(Objects::nonNull).toList();
    }

    public List<TonJettonTransferAction> jettonTransfers(Stream<TonAction> actions) {
        return actions.map(TonAction::jettonTransfer).filter(Objects::nonNull).toList();
    }

    public BigInteger amount(Long amount) {
        return BigInteger.valueOf(Objects.requireNonNullElse(amount, 0L));
    }

    public String scaled(String rawAmount, int decimals) {
        return scaledAmount(rawAmount, decimals).toPlainString();
    }

    public BigDecimal scaledAmount(String rawAmount, int decimals) {
        return decimal(rawAmount)
                .map(amount -> amount.movePointLeft(decimals))
                .orElse(BigDecimal.ZERO);
    }

    private Optional<BigDecimal> decimal(String rawAmount) {
        return Optional.ofNullable(rawAmount)
                .filter(amount -> !amount.isBlank())
                .flatMap(amount -> {
                    try {
                        return Optional.of(new BigDecimal(amount));
                    } catch (NumberFormatException e) {
                        return Optional.empty();
                    }
                });
    }
}
