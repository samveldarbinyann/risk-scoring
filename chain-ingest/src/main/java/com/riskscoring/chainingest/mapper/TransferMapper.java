package com.riskscoring.chainingest.mapper;

import com.riskscoring.chainingest.client.dto.MoralisTransaction;
import com.riskscoring.chainingest.client.dto.Transfer;
import com.riskscoring.chainingest.exception.MoralisException;
import com.riskscoring.common.model.TransferDirection;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

@Component
public class TransferMapper {

    private static final String FAILED_STATUS = "0";
    private static final String ZERO_ADDRESS = "0x0000000000000000000000000000000000000000";

    public List<Transfer> fromTransactions(List<MoralisTransaction> transactions, String owner) {
        return transactions.stream()
                .filter(tx -> !FAILED_STATUS.equals(tx.receiptStatus()))
                .flatMap(tx -> Stream.concat(
                        toTransfer(owner, tx.fromAddress(), tx.toAddress(), wei(tx.value()), tx.blockTimestamp()).stream(),
                        Stream.concat(internalTransfers(tx, owner), erc20Transfers(tx, owner))))
                .toList();
    }

    public Instant timestamp(String iso) {
        if (iso == null || iso.isBlank()) {
            return null;
        }

        try {
            return Instant.parse(iso.trim());
        } catch (DateTimeParseException e) {
            throw new MoralisException("Unparsable timestamp from Moralis: " + iso, e);
        }
    }

    private Stream<Transfer> internalTransfers(MoralisTransaction tx, String owner) {
        return Optional.ofNullable(tx.internalTransactions()).orElseGet(List::of).stream()
                .map(internal -> toTransfer(owner, internal.from(), internal.to(), wei(internal.value()), tx.blockTimestamp()))
                .flatMap(Optional::stream);
    }

    private Stream<Transfer> erc20Transfers(MoralisTransaction tx, String owner) {
        return Optional.ofNullable(tx.erc20Transfers()).orElseGet(List::of).stream()
                .map(transfer -> toTransfer(owner, transfer.fromAddress(), transfer.toAddress(), BigInteger.ZERO, tx.blockTimestamp()))
                .flatMap(Optional::stream);
    }

    private Optional<Transfer> toTransfer(String owner, String from, String to, BigInteger value, String timeStamp) {
        String sender = normalize(from);
        String recipient = normalize(to);

        if (owner.equals(sender) && !recipient.isEmpty() && !recipient.equals(owner) && !recipient.equals(ZERO_ADDRESS)) {
            return Optional.of(new Transfer(recipient, TransferDirection.OUT, value, timestamp(timeStamp)));
        }

        if (owner.equals(recipient) && !sender.isEmpty() && !sender.equals(owner) && !sender.equals(ZERO_ADDRESS)) {
            return Optional.of(new Transfer(sender, TransferDirection.IN, value, timestamp(timeStamp)));
        }

        return Optional.empty();
    }

    private BigInteger wei(String value) {
        if (value == null || value.isBlank()) {
            return BigInteger.ZERO;
        }

        try {
            return new BigInteger(value.trim());
        } catch (NumberFormatException e) {
            throw new MoralisException("Unparsable wei value from Moralis: " + value, e);
        }
    }

    private String normalize(String address) {
        return address == null ? "" : address.trim().toLowerCase(Locale.ROOT);
    }
}
