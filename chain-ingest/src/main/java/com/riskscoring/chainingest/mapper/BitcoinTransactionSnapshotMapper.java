package com.riskscoring.chainingest.mapper;

import com.riskscoring.chainingest.client.dto.mempool.MempoolTransaction;
import com.riskscoring.chainingest.client.dto.mempool.MempoolVin;
import com.riskscoring.chainingest.client.dto.mempool.MempoolVout;
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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class BitcoinTransactionSnapshotMapper {

    private static final int NO_NESTED_TRANSFERS = 0;
    private static final int NO_TOKEN_TRANSFERS = 0;
    private static final boolean NEVER_REVERTS = true;

    private final BitcoinValues values;
    private final TransactionPartyAggregator partyAggregator;

    public TransactionSnapshot fromMempool(MempoolTransaction transaction) {
        List<MempoolVin> inputs = values.inputs(transaction);
        List<MempoolVout> outputs = values.outputs(transaction);

        BigInteger total = outputs.stream()
                .map(output -> BigInteger.valueOf(output.value()))
                .reduce(BigInteger.ZERO, BigInteger::add);

        return new TransactionSnapshot(
                transaction.txid(),
                largestInput(inputs),
                largestOutput(inputs, outputs),
                total.toString(),
                NEVER_REVERTS,
                values.timestamp(transaction.status()),
                parties(inputs, outputs),
                NO_NESTED_TRANSFERS,
                NO_TOKEN_TRANSFERS,
                List.of(),
                Instant.now());
    }

    private String largestInput(List<MempoolVin> inputs) {
        return inputs.stream()
                .max(Comparator.comparingLong(values::inputValue))
                .map(values::inputAddress)
                .filter(values::isRoutable)
                .orElse(null);
    }

    private String largestOutput(List<MempoolVin> inputs, List<MempoolVout> outputs) {
        Set<String> change = inputs.stream()
                .map(values::inputAddress)
                .filter(values::isRoutable)
                .collect(Collectors.toSet());

        return outputs.stream()
                .filter(output -> !change.contains(values.address(output.address())))
                .max(Comparator.comparingLong(MempoolVout::value))
                .map(output -> values.address(output.address()))
                .filter(values::isRoutable)
                .orElse(null);
    }

    private List<TransactionParty> parties(List<MempoolVin> inputs, List<MempoolVout> outputs) {
        Stream<Optional<TransactionParty>> senders = inputs.stream()
                .map(input -> partyAggregator.party(values, values.inputAddress(input),
                        TransactionRole.SENDER, BigInteger.valueOf(values.inputValue(input))));

        Stream<Optional<TransactionParty>> recipients = outputs.stream()
                .map(output -> partyAggregator.party(values, output.address(),
                        TransactionRole.RECIPIENT, BigInteger.valueOf(output.value())));

        return partyAggregator.aggregate(Stream.concat(senders, recipients).flatMap(Optional::stream));
    }
}
