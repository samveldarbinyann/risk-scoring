package com.riskscoring.chainingest.mapper;

import com.riskscoring.chainingest.client.dto.MoralisTransaction;
import com.riskscoring.chainingest.client.dto.Transfer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class TransferMapper {

    private final MoralisValues values;
    private final TransferDirectionResolver transferDirectionResolver;

    public List<Transfer> fromTransactions(List<MoralisTransaction> transactions, String owner) {
        return transactions.stream()
                .filter(values::succeeded)
                .flatMap(tx -> Stream.concat(
                        toTransfer(owner, tx.fromAddress(), tx.toAddress(), values.wei(tx.value()), tx.blockTimestamp()).stream(),
                        Stream.concat(internalTransfers(tx, owner), erc20Transfers(tx, owner))))
                .toList();
    }

    public Instant timestamp(String iso) {
        return values.timestamp(iso);
    }

    private Stream<Transfer> internalTransfers(MoralisTransaction tx, String owner) {
        return Optional.ofNullable(tx.internalTransactions()).orElseGet(List::of).stream()
                .map(internal -> toTransfer(owner, internal.from(), internal.to(), values.wei(internal.value()), tx.blockTimestamp()))
                .flatMap(Optional::stream);
    }

    private Stream<Transfer> erc20Transfers(MoralisTransaction tx, String owner) {
        return Optional.ofNullable(tx.erc20Transfers()).orElseGet(List::of).stream()
                .map(transfer -> toTransfer(owner, transfer.fromAddress(), transfer.toAddress(), BigInteger.ZERO, tx.blockTimestamp()))
                .flatMap(Optional::stream);
    }

    private Optional<Transfer> toTransfer(String owner, String from, String to, BigInteger value, String timeStamp) {
        return transferDirectionResolver.resolve(values, owner, from, to, value, values.timestamp(timeStamp));
    }
}
