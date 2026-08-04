package com.riskscoring.chainingest.mapper;

import com.riskscoring.chainingest.client.dto.MoralisErc20Transfer;
import com.riskscoring.chainingest.client.dto.MoralisInternalTransfer;
import com.riskscoring.chainingest.client.dto.MoralisTransaction;
import com.riskscoring.chainingest.config.ChainIngestProperties;
import com.riskscoring.common.model.TokenTransfer;
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
    private final ChainIngestProperties properties;

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
                tokenTransfers(tokens),
                Instant.now());
    }

    private List<TokenTransfer> tokenTransfers(List<MoralisErc20Transfer> tokens) {
        return tokens.stream()
                .limit(properties.maxTokenTransfers())
                .map(transfer -> new TokenTransfer(
                        transfer.symbol(),
                        values.address(transfer.contract()),
                        values.address(transfer.fromAddress()),
                        values.address(transfer.toAddress()),
                        transfer.valueFormatted()))
                .toList();
    }

    private List<TransactionParty> parties(MoralisTransaction transaction,
                                           List<MoralisInternalTransfer> internals,
                                           List<MoralisErc20Transfer> tokens) {
        BigInteger value = values.wei(transaction.value());

        Stream<Optional<TransactionParty>> direct = Stream.of(
                partyAggregator.party(values, transaction.fromAddress(), TransactionRole.SENDER, value),
                partyAggregator.party(values, transaction.toAddress(), TransactionRole.RECIPIENT, value));

        Stream<Optional<TransactionParty>> internal = internals.stream().flatMap(transfer -> {
            BigInteger amount = values.wei(transfer.value());
            return Stream.of(
                    partyAggregator.party(values, transfer.from(), TransactionRole.INTERNAL_SENDER, amount),
                    partyAggregator.party(values, transfer.to(), TransactionRole.INTERNAL_RECIPIENT, amount));
        });

        Stream<Optional<TransactionParty>> token = tokens.stream().flatMap(transfer -> Stream.of(
                partyAggregator.party(values, transfer.fromAddress(), TransactionRole.TOKEN_SENDER, BigInteger.ZERO),
                partyAggregator.party(values, transfer.toAddress(), TransactionRole.TOKEN_RECIPIENT, BigInteger.ZERO)));

        return partyAggregator.aggregate(
                Stream.of(direct, internal, token).flatMap(stream -> stream.flatMap(Optional::stream)));
    }
}
