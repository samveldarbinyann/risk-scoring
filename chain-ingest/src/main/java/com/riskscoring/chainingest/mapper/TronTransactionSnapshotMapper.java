package com.riskscoring.chainingest.mapper;

import com.riskscoring.chainingest.client.dto.trongrid.TronContractValue;
import com.riskscoring.chainingest.client.dto.trongrid.TronTransaction;
import com.riskscoring.chainingest.client.dto.trongrid.TronTransactionInfo;
import com.riskscoring.chainingest.client.dto.trongrid.TronTrc20Transfer;
import com.riskscoring.chainingest.config.ChainIngestProperties;
import com.riskscoring.common.model.TokenTransfer;
import com.riskscoring.common.model.TransactionParty;
import com.riskscoring.common.model.TransactionRole;
import com.riskscoring.common.model.TransactionSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class TronTransactionSnapshotMapper {

    private static final int NO_NESTED_TRANSFERS = 0;

    private final TronValues values;
    private final TransactionPartyAggregator partyAggregator;
    private final ChainIngestProperties properties;

    public TransactionSnapshot fromTronGrid(TronTransaction transaction,
                                            TronTransactionInfo info,
                                            List<TronTrc20Transfer> tokenTransfers) {
        Optional<TronContractValue> value = values.contract(transaction).flatMap(values::value);

        String sender = value.map(TronContractValue::ownerAddress).map(values::address).orElse("");
        BigInteger nativeAmount = BigInteger.valueOf(value.map(TronContractValue::amount).orElse(0L));

        return new TransactionSnapshot(
                values.normalize(transaction.txID()),
                values.isRoutable(sender) ? sender : null,
                recipient(value, tokenTransfers, sender),
                nativeAmount.toString(),
                values.succeeded(transaction),
                values.timestamp(info.blockTimeStamp()),
                parties(value, nativeAmount, tokenTransfers),
                NO_NESTED_TRANSFERS,
                tokenTransfers.size(),
                tokenTransfers(tokenTransfers),
                Instant.now());
    }

    private String recipient(Optional<TronContractValue> value,
                             List<TronTrc20Transfer> tokenTransfers,
                             String sender) {
        return value.map(TronContractValue::toAddress)
                .map(values::address)
                .filter(values::isRoutable)
                .or(() -> largestTokenRecipient(tokenTransfers, sender))
                .orElse(null);
    }

    private Optional<String> largestTokenRecipient(List<TronTrc20Transfer> tokenTransfers, String sender) {
        return tokenTransfers.stream()
                .filter(transfer -> {
                    String to = values.address(transfer.to());
                    return values.isRoutable(to) && !to.equals(sender);
                })
                .max(Comparator.comparing(transfer -> new BigInteger(transfer.value())))
                .map(transfer -> values.address(transfer.to()));
    }

    private List<TransactionParty> parties(Optional<TronContractValue> value,
                                           BigInteger nativeAmount,
                                           List<TronTrc20Transfer> tokenTransfers) {
        Stream<Optional<TransactionParty>> nativeParties = value.stream().flatMap(contract -> Stream.of(
                party(contract.ownerAddress(), TransactionRole.SENDER, nativeAmount),
                party(contract.toAddress(), TransactionRole.RECIPIENT, nativeAmount)));

        Stream<Optional<TransactionParty>> tokenParties = tokenTransfers.stream().flatMap(transfer -> Stream.of(
                party(transfer.from(), TransactionRole.TOKEN_SENDER, BigInteger.ZERO),
                party(transfer.to(), TransactionRole.TOKEN_RECIPIENT, BigInteger.ZERO)));

        return partyAggregator.aggregate(Stream.concat(nativeParties, tokenParties).flatMap(Optional::stream));
    }

    private Optional<TransactionParty> party(String address, TransactionRole role, BigInteger amount) {
        String normalized = values.address(address);
        return values.isRoutable(normalized)
                ? Optional.of(new TransactionParty(normalized, role, amount.toString()))
                : Optional.empty();
    }

    private List<TokenTransfer> tokenTransfers(List<TronTrc20Transfer> transfers) {
        return transfers.stream()
                .limit(properties.maxTokenTransfers())
                .map(transfer -> new TokenTransfer(
                        transfer.tokenInfo().symbol(),
                        values.address(transfer.tokenInfo().address()),
                        values.address(transfer.from()),
                        values.address(transfer.to()),
                        values.scaled(transfer.value(), transfer.tokenInfo().decimals())))
                .toList();
    }
}
