package com.riskscoring.chainingest.mapper;

import com.riskscoring.chainingest.client.dto.Transfer;
import com.riskscoring.chainingest.client.dto.trongrid.TronTransaction;
import com.riskscoring.chainingest.client.dto.trongrid.TronTrc20Transfer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TronTransferMapper {

    private final TronValues values;
    private final TransferDirectionResolver transferDirectionResolver;

    public List<Transfer> fromNative(List<TronTransaction> transactions, String owner) {
        return transactions.stream()
                .filter(values::succeeded)
                .map(transaction -> nativeTransfer(transaction, owner))
                .flatMap(Optional::stream)
                .toList();
    }

    public List<Transfer> fromTrc20(List<TronTrc20Transfer> transfers, String owner) {
        return transfers.stream()
                .map(transfer -> toTransfer(owner, transfer.from(), transfer.to(), BigInteger.ZERO,
                        values.timestamp(transfer.blockTimestamp())))
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<Transfer> nativeTransfer(TronTransaction transaction, String owner) {
        return values.contract(transaction)
                .filter(contract -> TronValues.TRANSFER_CONTRACT.equals(contract.type()))
                .flatMap(values::value)
                .flatMap(value -> toTransfer(owner, value.ownerAddress(), value.toAddress(),
                        values.amount(value), values.timestamp(transaction.blockTimestamp())));
    }

    private Optional<Transfer> toTransfer(String owner, String from, String to, BigInteger value, Instant at) {
        return transferDirectionResolver.resolve(values, owner, from, to, value, at);
    }
}
