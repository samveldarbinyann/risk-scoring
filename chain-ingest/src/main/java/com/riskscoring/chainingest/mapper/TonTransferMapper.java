package com.riskscoring.chainingest.mapper;

import com.riskscoring.chainingest.client.dto.Transfer;
import com.riskscoring.chainingest.client.dto.tonapi.TonAccountAddress;
import com.riskscoring.chainingest.client.dto.tonapi.TonAction;
import com.riskscoring.chainingest.client.dto.tonapi.TonEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class TonTransferMapper {

    private final TonValues values;
    private final TransferDirectionResolver transferDirectionResolver;

    public List<Transfer> fromEvents(List<TonEvent> events, String owner) {
        return events.stream()
                .filter(event -> !event.scam())
                .flatMap(event -> transfers(event, owner))
                .toList();
    }

    private Stream<Transfer> transfers(TonEvent event, String owner) {
        Instant at = values.timestamp(event.timestamp());

        return values.transferActions(event)
                .filter(values::succeeded)
                .map(action -> toTransfer(action, owner, at))
                .flatMap(Optional::stream);
    }

    private Optional<Transfer> toTransfer(TonAction action, String owner, Instant at) {
        return Optional.ofNullable(action.tonTransfer())
                .flatMap(transfer -> resolve(owner, transfer.sender(), transfer.recipient(),
                        values.amount(transfer.amount()), at))
                .or(() -> Optional.ofNullable(action.jettonTransfer())
                        .flatMap(transfer -> resolve(owner, transfer.sender(), transfer.recipient(),
                                BigInteger.ZERO, at)));
    }

    private Optional<Transfer> resolve(String owner, TonAccountAddress sender, TonAccountAddress recipient,
                                       BigInteger valueNative, Instant at) {
        return transferDirectionResolver.resolve(
                values, owner, values.party(sender), values.party(recipient), valueNative, at);
    }
}
