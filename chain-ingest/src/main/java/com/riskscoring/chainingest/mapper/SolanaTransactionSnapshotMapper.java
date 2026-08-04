package com.riskscoring.chainingest.mapper;

import com.riskscoring.chainingest.client.dto.helius.HeliusNativeTransfer;
import com.riskscoring.chainingest.client.dto.helius.HeliusTokenTransfer;
import com.riskscoring.chainingest.client.dto.helius.HeliusTransaction;
import com.riskscoring.chainingest.config.ChainIngestProperties;
import com.riskscoring.common.model.TokenTransfer;
import com.riskscoring.common.model.TransactionParty;
import com.riskscoring.common.model.TransactionRole;
import com.riskscoring.common.model.TransactionSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class SolanaTransactionSnapshotMapper {

    private static final int NO_NESTED_TRANSFERS = 0;

    private final SolanaValues values;
    private final TransactionPartyAggregator partyAggregator;
    private final ChainIngestProperties properties;

    public TransactionSnapshot fromHelius(HeliusTransaction transaction) {
        List<HeliusNativeTransfer> nativeTransfers = values.nativeTransfers(transaction);
        List<HeliusTokenTransfer> tokenTransfers = values.tokenTransfers(transaction);
        String feePayer = values.address(transaction.feePayer());

        BigInteger total = nativeTransfers.stream()
                .map(transfer -> BigInteger.valueOf(transfer.amount()))
                .reduce(BigInteger.ZERO, BigInteger::add);

        return new TransactionSnapshot(
                values.normalize(transaction.signature()),
                values.isRoutable(feePayer) ? feePayer : null,
                largestRecipient(nativeTransfers, tokenTransfers, feePayer),
                total.toString(),
                values.succeeded(transaction),
                values.timestamp(transaction.timestamp()),
                parties(nativeTransfers, tokenTransfers),
                NO_NESTED_TRANSFERS,
                tokenTransfers.size(),
                tokenTransfers(tokenTransfers),
                Instant.now());
    }

    private List<TokenTransfer> tokenTransfers(List<HeliusTokenTransfer> transfers) {
        return transfers.stream()
                .limit(properties.maxTokenTransfers())
                .map(transfer -> new TokenTransfer(
                        null,
                        values.address(transfer.mint()),
                        values.address(transfer.fromUserAccount()),
                        values.address(transfer.toUserAccount()),
                        tokenAmount(transfer).toPlainString()))
                .toList();
    }

    private String largestRecipient(List<HeliusNativeTransfer> nativeTransfers,
                                    List<HeliusTokenTransfer> tokenTransfers,
                                    String feePayer) {
        return nativeTransfers.stream()
                .filter(transfer -> isOther(transfer.toUserAccount(), feePayer))
                .max(Comparator.comparingLong(HeliusNativeTransfer::amount))
                .map(transfer -> values.address(transfer.toUserAccount()))
                .or(() -> tokenTransfers.stream()
                        .filter(transfer -> isOther(transfer.toUserAccount(), feePayer))
                        .max(Comparator.comparing(this::tokenAmount))
                        .map(transfer -> values.address(transfer.toUserAccount())))
                .orElse(null);
    }

    private BigDecimal tokenAmount(HeliusTokenTransfer transfer) {
        return values.amount(transfer.tokenAmount());
    }

    private boolean isOther(String address, String feePayer) {
        String normalized = values.address(address);
        return values.isRoutable(normalized) && !normalized.equals(feePayer);
    }

    private List<TransactionParty> parties(List<HeliusNativeTransfer> nativeTransfers,
                                           List<HeliusTokenTransfer> tokenTransfers) {
        Stream<Optional<TransactionParty>> nativeParties = nativeTransfers.stream().flatMap(transfer -> {
            BigInteger amount = BigInteger.valueOf(transfer.amount());
            return Stream.of(
                    partyAggregator.party(values, transfer.fromUserAccount(), TransactionRole.SENDER, amount),
                    partyAggregator.party(values, transfer.toUserAccount(), TransactionRole.RECIPIENT, amount));
        });

        Stream<Optional<TransactionParty>> tokenParties = tokenTransfers.stream().flatMap(transfer -> Stream.of(
                partyAggregator.party(values, transfer.fromUserAccount(), TransactionRole.TOKEN_SENDER, BigInteger.ZERO),
                partyAggregator.party(values, transfer.toUserAccount(), TransactionRole.TOKEN_RECIPIENT, BigInteger.ZERO)));

        return partyAggregator.aggregate(Stream.concat(nativeParties, tokenParties).flatMap(Optional::stream));
    }
}
