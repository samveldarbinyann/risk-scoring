package com.riskscoring.chainingest.mapper;

import com.riskscoring.chainingest.client.dto.MoralisErc20Transfer;
import com.riskscoring.chainingest.client.dto.MoralisInternalTransfer;
import com.riskscoring.chainingest.client.dto.MoralisTransaction;
import com.riskscoring.common.model.TransactionParty;
import com.riskscoring.common.model.TransactionRole;
import com.riskscoring.common.model.TransactionSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class TransactionSnapshotMapper {

    private final MoralisValues values;
    private final TransactionPartyAggregator partyAggregator;

    public TransactionSnapshot fromMoralis(MoralisTransaction transaction) {
        List<MoralisInternalTransfer> internals =
                Optional.ofNullable(transaction.internalTransactions()).orElseGet(List::of);
        List<MoralisErc20Transfer> tokens =
                Optional.ofNullable(transaction.erc20Transfers()).orElseGet(List::of);

        return new TransactionSnapshot(
                values.address(transaction.hash()),
                values.address(transaction.fromAddress()),
                values.address(transaction.toAddress()),
                values.wei(transaction.value()).toString(),
                values.succeeded(transaction),
                values.timestamp(transaction.blockTimestamp()),
                parties(transaction, internals, tokens),
                internals.size(),
                tokens.size(),
                Instant.now());
    }

    private List<TransactionParty> parties(MoralisTransaction transaction,
                                           List<MoralisInternalTransfer> internals,
                                           List<MoralisErc20Transfer> tokens) {
        BigInteger value = values.wei(transaction.value());

        Stream<Optional<TransactionParty>> direct = Stream.of(
                party(transaction.fromAddress(), TransactionRole.SENDER, value),
                party(transaction.toAddress(), TransactionRole.RECIPIENT, value));

        Stream<Optional<TransactionParty>> internal = internals.stream().flatMap(transfer -> Stream.of(
                party(transfer.from(), TransactionRole.INTERNAL_SENDER, values.wei(transfer.value())),
                party(transfer.to(), TransactionRole.INTERNAL_RECIPIENT, values.wei(transfer.value()))));

        Stream<Optional<TransactionParty>> token = tokens.stream().flatMap(transfer -> Stream.of(
                party(transfer.fromAddress(), TransactionRole.TOKEN_SENDER, BigInteger.ZERO),
                party(transfer.toAddress(), TransactionRole.TOKEN_RECIPIENT, BigInteger.ZERO)));

        return partyAggregator.aggregate(
                Stream.of(direct, internal, token).flatMap(stream -> stream.flatMap(Optional::stream)));
    }

    private Optional<TransactionParty> party(String address, TransactionRole role, BigInteger value) {
        String normalized = values.address(address);
        return values.isRoutable(normalized)
                ? Optional.of(new TransactionParty(normalized, role, value.toString()))
                : Optional.empty();
    }
}
