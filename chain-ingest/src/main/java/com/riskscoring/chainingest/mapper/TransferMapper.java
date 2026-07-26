package com.riskscoring.chainingest.mapper;

import com.riskscoring.chainingest.client.dto.EtherscanTokenTx;
import com.riskscoring.chainingest.client.dto.EtherscanTx;
import com.riskscoring.chainingest.client.dto.Transfer;
import com.riskscoring.chainingest.exception.EtherscanException;
import com.riskscoring.common.model.TransferDirection;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
public class TransferMapper {

    private static final String ERROR_FLAG = "1";

    public List<Transfer> fromTransactions(List<EtherscanTx> transactions, String owner) {
        return transactions.stream()
                .filter(transaction -> !ERROR_FLAG.equals(transaction.isError()))
                .map(transaction -> toTransfer(
                        owner,
                        transaction.from(),
                        recipientOf(transaction),
                        wei(transaction.value()),
                        transaction.timeStamp()))
                .flatMap(Optional::stream)
                .toList();
    }

    public List<Transfer> fromTokenTransfers(List<EtherscanTokenTx> transfers, String owner) {
        return transfers.stream()
                .map(transfer -> toTransfer(
                        owner,
                        transfer.from(),
                        transfer.to(),
                        BigInteger.ZERO,
                        transfer.timeStamp()))
                .flatMap(Optional::stream)
                .toList();
    }

    public Instant timestamp(String epochSeconds) {
        if (epochSeconds == null || epochSeconds.isBlank()) {
            return null;
        }

        try {
            return Instant.ofEpochSecond(Long.parseLong(epochSeconds.trim()));
        } catch (NumberFormatException e) {
            throw new EtherscanException("Unparsable timestamp from Etherscan: " + epochSeconds, e);
        }
    }

    private Optional<Transfer> toTransfer(String owner, String from, String to, BigInteger value, String timeStamp) {
        String sender = normalize(from);
        String recipient = normalize(to);

        if (owner.equals(sender) && !recipient.isEmpty() && !recipient.equals(owner)) {
            return Optional.of(new Transfer(recipient, TransferDirection.OUT, value, timestamp(timeStamp)));
        }

        if (owner.equals(recipient) && !sender.isEmpty() && !sender.equals(owner)) {
            return Optional.of(new Transfer(sender, TransferDirection.IN, value, timestamp(timeStamp)));
        }

        return Optional.empty();
    }

    private String recipientOf(EtherscanTx transaction) {
        return normalize(transaction.to()).isEmpty() ? transaction.contractAddress() : transaction.to();
    }

    private BigInteger wei(String value) {
        if (value == null || value.isBlank()) {
            return BigInteger.ZERO;
        }

        try {
            return new BigInteger(value.trim());
        } catch (NumberFormatException e) {
            throw new EtherscanException("Unparsable wei value from Etherscan: " + value, e);
        }
    }

    private String normalize(String address) {
        return address == null ? "" : address.trim().toLowerCase(Locale.ROOT);
    }
}
