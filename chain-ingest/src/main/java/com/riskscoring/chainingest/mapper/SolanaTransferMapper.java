package com.riskscoring.chainingest.mapper;

import com.riskscoring.chainingest.client.dto.Transfer;
import com.riskscoring.chainingest.client.dto.helius.HeliusTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class SolanaTransferMapper {

    private final SolanaValues values;
    private final TransferDirectionResolver transferDirectionResolver;

    public List<Transfer> fromTransactions(List<HeliusTransaction> transactions, String owner) {
        return transactions.stream()
                .filter(values::succeeded)
                .flatMap(transaction -> counterparties(transaction, owner))
                .toList();
    }

    private Stream<Transfer> counterparties(HeliusTransaction transaction, String owner) {
        Instant at = values.timestamp(transaction.timestamp());

        Stream<Optional<Transfer>> nativeTransfers = values.nativeTransfers(transaction).stream()
                .map(transfer -> toTransfer(owner, transfer.fromUserAccount(), transfer.toUserAccount(),
                        BigInteger.valueOf(transfer.amount()), at));

        Stream<Optional<Transfer>> tokenTransfers = values.tokenTransfers(transaction).stream()
                .map(transfer -> toTransfer(owner, transfer.fromUserAccount(), transfer.toUserAccount(),
                        BigInteger.ZERO, at));

        return Stream.concat(nativeTransfers, tokenTransfers).flatMap(Optional::stream);
    }

    private Optional<Transfer> toTransfer(String owner, String from, String to, BigInteger value, Instant at) {
        return transferDirectionResolver.resolve(values, owner, from, to, value, at);
    }
}
