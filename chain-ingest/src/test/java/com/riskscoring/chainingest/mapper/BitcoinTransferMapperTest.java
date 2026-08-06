package com.riskscoring.chainingest.mapper;

import com.riskscoring.chainingest.client.dto.Transfer;
import com.riskscoring.chainingest.client.dto.mempool.MempoolStatus;
import com.riskscoring.chainingest.client.dto.mempool.MempoolTransaction;
import com.riskscoring.chainingest.client.dto.mempool.MempoolVin;
import com.riskscoring.chainingest.client.dto.mempool.MempoolVout;
import com.riskscoring.common.model.TransferDirection;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BitcoinTransferMapperTest {

    private static final MempoolStatus STATUS = new MempoolStatus(true, 800000L, 1700000000L);
    private static final Instant AT = Instant.ofEpochSecond(1700000000L);

    private final BitcoinValues values = new BitcoinValues();
    private final BitcoinTransferMapper mapper = new BitcoinTransferMapper(values);

    @Test
    void addressAsSenderProducesOutTransfersToOtherOutputs() {
        MempoolTransaction transaction = new MempoolTransaction("tx1", 100,
                List.of(new MempoolVin(new MempoolVout("addrX", 1000L), false)),
                List.of(new MempoolVout("addrY", 400L), new MempoolVout("addrX", 550L)),
                STATUS);

        List<Transfer> transfers = mapper.fromTransactions(List.of(transaction), "addrX");

        assertThat(transfers).containsExactly(new Transfer("addrY", TransferDirection.OUT, BigInteger.valueOf(400L), AT));
    }

    @Test
    void addressAsRecipientOnlyProducesInTransfersFromInputs() {
        MempoolTransaction transaction = new MempoolTransaction("tx1", 100,
                List.of(new MempoolVin(new MempoolVout("addrA", 1000L), false)),
                List.of(new MempoolVout("addrZ", 300L)),
                STATUS);

        List<Transfer> transfers = mapper.fromTransactions(List.of(transaction), "addrZ");

        assertThat(transfers).containsExactly(new Transfer("addrA", TransferDirection.IN, BigInteger.valueOf(1000L), AT));
    }

    @Test
    void emptyTransactionListProducesEmptyTransfers() {
        assertThat(mapper.fromTransactions(List.of(), "addrX")).isEmpty();
    }

    @Test
    void nonRoutableOutputAddressIsExcluded() {
        MempoolTransaction transaction = new MempoolTransaction("tx1", 100,
                List.of(new MempoolVin(new MempoolVout("addrX", 1000L), false)),
                List.of(new MempoolVout(null, 400L)),
                STATUS);

        assertThat(mapper.fromTransactions(List.of(transaction), "addrX")).isEmpty();
    }
}
