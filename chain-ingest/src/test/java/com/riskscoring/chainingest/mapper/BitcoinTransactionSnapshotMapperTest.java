package com.riskscoring.chainingest.mapper;

import com.riskscoring.chainingest.client.dto.mempool.MempoolStatus;
import com.riskscoring.chainingest.client.dto.mempool.MempoolTransaction;
import com.riskscoring.chainingest.client.dto.mempool.MempoolVin;
import com.riskscoring.chainingest.client.dto.mempool.MempoolVout;
import com.riskscoring.common.model.TransactionParty;
import com.riskscoring.common.model.TransactionRole;
import com.riskscoring.common.model.TransactionSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BitcoinTransactionSnapshotMapperTest {

    private static final MempoolStatus STATUS = new MempoolStatus(true, 800000L, 1700000000L);

    private final BitcoinValues values = new BitcoinValues();
    private final BitcoinTransactionSnapshotMapper mapper =
            new BitcoinTransactionSnapshotMapper(values, new TransactionPartyAggregator());

    @Test
    void mapsLargestInputAndOutputTotalAndParties() {
        MempoolTransaction transaction = new MempoolTransaction("tx1", 100,
                List.of(new MempoolVin(new MempoolVout("addrA", 100L), false),
                        new MempoolVin(new MempoolVout("addrB", 500L), false)),
                List.of(new MempoolVout("addrC", 300L)),
                STATUS);

        TransactionSnapshot snapshot = mapper.fromMempool(transaction);

        assertThat(snapshot.fromAddress()).isEqualTo("addrB");
        assertThat(snapshot.toAddress()).isEqualTo("addrC");
        assertThat(snapshot.valueNative()).isEqualTo("300");
        assertThat(snapshot.success()).isTrue();
        assertThat(snapshot.nestedTransferCount()).isZero();
        assertThat(snapshot.tokenTransferCount()).isZero();
        assertThat(snapshot.tokenTransfers()).isEmpty();
        assertThat(snapshot.parties()).containsExactlyInAnyOrder(
                new TransactionParty("addrA", TransactionRole.SENDER, "100"),
                new TransactionParty("addrB", TransactionRole.SENDER, "500"),
                new TransactionParty("addrC", TransactionRole.RECIPIENT, "300"));
    }

    @Test
    void largestOutputExcludesChangeEvenWhenItIsBiggerThanRealRecipient() {
        MempoolTransaction transaction = new MempoolTransaction("tx1", 100,
                List.of(new MempoolVin(new MempoolVout("addrA", 1000L), false)),
                List.of(new MempoolVout("addrA", 900L), new MempoolVout("addrB", 100L)),
                STATUS);

        TransactionSnapshot snapshot = mapper.fromMempool(transaction);

        assertThat(snapshot.toAddress()).isEqualTo("addrB");
    }

    @Test
    void largestOutputIsNullWhenAllOutputsAreChange() {
        MempoolTransaction transaction = new MempoolTransaction("tx1", 100,
                List.of(new MempoolVin(new MempoolVout("addrA", 1000L), false)),
                List.of(new MempoolVout("addrA", 500L)),
                STATUS);

        TransactionSnapshot snapshot = mapper.fromMempool(transaction);

        assertThat(snapshot.toAddress()).isNull();
    }

    @Test
    void largestInputIsNullWhenNoInputs() {
        MempoolTransaction transaction = new MempoolTransaction("tx1", 100,
                List.of(),
                List.of(new MempoolVout("addrB", 100L)),
                STATUS);

        TransactionSnapshot snapshot = mapper.fromMempool(transaction);

        assertThat(snapshot.fromAddress()).isNull();
    }
}
