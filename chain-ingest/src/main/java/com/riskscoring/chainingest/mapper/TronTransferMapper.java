package com.riskscoring.chainingest.mapper;

import com.riskscoring.chainingest.client.dto.Transfer;
import com.riskscoring.chainingest.client.dto.trongrid.TronTransaction;
import com.riskscoring.chainingest.client.dto.trongrid.TronTrc20Transfer;
import com.riskscoring.common.model.TransferDirection;
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
                        BigInteger.valueOf(value.amount()), values.timestamp(transaction.blockTimestamp())));
    }

    private Optional<Transfer> toTransfer(String owner, String from, String to, BigInteger value, Instant at) {
        String sender = values.address(from);
        String recipient = values.address(to);

        if (owner.equals(sender) && values.isRoutable(recipient) && !recipient.equals(owner)) {
            return Optional.of(new Transfer(recipient, TransferDirection.OUT, value, at));
        }

        if (owner.equals(recipient) && values.isRoutable(sender) && !sender.equals(owner)) {
            return Optional.of(new Transfer(sender, TransferDirection.IN, value, at));
        }

        return Optional.empty();
    }
}
