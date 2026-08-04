package com.riskscoring.chainingest.mapper;

import com.riskscoring.chainingest.client.dto.tonapi.TonEvent;
import com.riskscoring.chainingest.client.dto.tonapi.TonJettonTransferAction;
import com.riskscoring.chainingest.client.dto.tonapi.TonTransferAction;
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
public class TonTransactionSnapshotMapper {

    private static final int NO_NESTED_TRANSFERS = 0;

    private final TonValues values;
    private final TransactionPartyAggregator partyAggregator;
    private final ChainIngestProperties properties;

    public TransactionSnapshot fromTonApi(TonEvent event, String hash) {
        List<TonTransferAction> nativeTransfers = values.nativeTransfers(values.transferActions(event));
        List<TonJettonTransferAction> jettonTransfers = values.jettonTransfers(values.transferActions(event));

        String sender = sender(nativeTransfers, jettonTransfers);
        BigInteger total = nativeTransfers.stream()
                .map(transfer -> values.amount(transfer.amount()))
                .reduce(BigInteger.ZERO, BigInteger::add);

        return new TransactionSnapshot(
                values.normalize(hash),
                values.isRoutable(sender) ? sender : null,
                largestRecipient(nativeTransfers, jettonTransfers, sender),
                total.toString(),
                values.succeeded(event),
                values.timestamp(event.timestamp()),
                parties(nativeTransfers, jettonTransfers),
                NO_NESTED_TRANSFERS,
                jettonTransfers.size(),
                tokenTransfers(jettonTransfers),
                Instant.now());
    }

    private String sender(List<TonTransferAction> nativeTransfers, List<TonJettonTransferAction> jettonTransfers) {
        return nativeTransfers.stream()
                .map(transfer -> values.party(transfer.sender()))
                .findFirst()
                .or(() -> jettonTransfers.stream().map(transfer -> values.party(transfer.sender())).findFirst())
                .orElse("");
    }

    private String largestRecipient(List<TonTransferAction> nativeTransfers,
                                    List<TonJettonTransferAction> jettonTransfers,
                                    String sender) {
        return nativeTransfers.stream()
                .filter(transfer -> isOther(values.party(transfer.recipient()), sender))
                .max(Comparator.comparing(transfer -> values.amount(transfer.amount())))
                .map(transfer -> values.party(transfer.recipient()))
                .or(() -> jettonTransfers.stream()
                        .filter(transfer -> isOther(values.party(transfer.recipient()), sender))
                        .max(Comparator.comparing(this::jettonAmount))
                        .map(transfer -> values.party(transfer.recipient())))
                .orElse(null);
    }

    private boolean isOther(String address, String sender) {
        return values.isRoutable(address) && !address.equals(sender);
    }

    private BigDecimal jettonAmount(TonJettonTransferAction transfer) {
        return values.scaledAmount(transfer.amount(), decimals(transfer));
    }

    private int decimals(TonJettonTransferAction transfer) {
        return transfer.jetton() == null ? 0 : transfer.jetton().decimals();
    }

    private List<TransactionParty> parties(List<TonTransferAction> nativeTransfers,
                                           List<TonJettonTransferAction> jettonTransfers) {
        Stream<Optional<TransactionParty>> nativeParties = nativeTransfers.stream().flatMap(transfer -> {
            BigInteger amount = values.amount(transfer.amount());
            return Stream.of(
                    partyAggregator.party(values, values.party(transfer.sender()), TransactionRole.SENDER, amount),
                    partyAggregator.party(values, values.party(transfer.recipient()), TransactionRole.RECIPIENT, amount));
        });

        Stream<Optional<TransactionParty>> jettonParties = jettonTransfers.stream().flatMap(transfer -> Stream.of(
                partyAggregator.party(values, values.party(transfer.sender()),
                        TransactionRole.TOKEN_SENDER, BigInteger.ZERO),
                partyAggregator.party(values, values.party(transfer.recipient()),
                        TransactionRole.TOKEN_RECIPIENT, BigInteger.ZERO)));

        return partyAggregator.aggregate(Stream.concat(nativeParties, jettonParties).flatMap(Optional::stream));
    }

    private List<TokenTransfer> tokenTransfers(List<TonJettonTransferAction> transfers) {
        return transfers.stream()
                .filter(transfer -> transfer.jetton() != null)
                .limit(properties.maxTokenTransfers())
                .map(transfer -> new TokenTransfer(
                        transfer.jetton().symbol(),
                        values.address(transfer.jetton().address()),
                        values.party(transfer.sender()),
                        values.party(transfer.recipient()),
                        values.scaled(transfer.amount(), transfer.jetton().decimals())))
                .toList();
    }
}
