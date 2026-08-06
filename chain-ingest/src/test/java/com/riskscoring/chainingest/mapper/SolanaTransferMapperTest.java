package com.riskscoring.chainingest.mapper;

import com.riskscoring.chainingest.client.dto.Transfer;
import com.riskscoring.chainingest.client.dto.helius.HeliusNativeTransfer;
import com.riskscoring.chainingest.client.dto.helius.HeliusTokenTransfer;
import com.riskscoring.chainingest.client.dto.helius.HeliusTransaction;
import com.riskscoring.chainingest.client.dto.helius.HeliusTransactionError;
import com.riskscoring.common.model.TransferDirection;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SolanaTransferMapperTest {

    private static final String OWNER = "owner";
    private static final long TIMESTAMP = 1700000000L;
    private static final Instant AT = Instant.ofEpochSecond(TIMESTAMP);

    private final SolanaValues values = new SolanaValues();
    private final SolanaTransferMapper mapper = new SolanaTransferMapper(values, new TransferDirectionResolver());

    @Test
    void filtersOutTransactionsWithTransactionError() {
        HeliusTransaction failed = new HeliusTransaction("sig", TIMESTAMP, OWNER,
                new HeliusTransactionError("boom"),
                List.of(new HeliusNativeTransfer(OWNER, "recipient", 100L)), null);

        assertThat(mapper.fromTransactions(List.of(failed), OWNER)).isEmpty();
    }

    @Test
    void nativeTransfersContributeTransferEntries() {
        HeliusTransaction transaction = new HeliusTransaction("sig", TIMESTAMP, OWNER, null,
                List.of(new HeliusNativeTransfer(OWNER, "recipient", 100L)), null);

        List<Transfer> transfers = mapper.fromTransactions(List.of(transaction), OWNER);

        assertThat(transfers).containsExactly(
                new Transfer("recipient", TransferDirection.OUT, BigInteger.valueOf(100L), AT));
    }

    @Test
    void tokenTransfersContributeTransferEntriesWithZeroValue() {
        HeliusTransaction transaction = new HeliusTransaction("sig", TIMESTAMP, OWNER, null, null,
                List.of(new HeliusTokenTransfer(OWNER, "recipient", "mint", BigDecimal.TEN)));

        List<Transfer> transfers = mapper.fromTransactions(List.of(transaction), OWNER);

        assertThat(transfers).containsExactly(
                new Transfer("recipient", TransferDirection.OUT, BigInteger.ZERO, AT));
    }

    @Test
    void nullTransferListsAreTreatedAsEmptyWithoutError() {
        HeliusTransaction transaction = new HeliusTransaction("sig", TIMESTAMP, OWNER, null, null, null);

        assertThat(mapper.fromTransactions(List.of(transaction), OWNER)).isEmpty();
    }
}
