package com.riskscoring.chainingest.mapper;

import com.riskscoring.chainingest.client.dto.helius.HeliusNativeTransfer;
import com.riskscoring.chainingest.client.dto.helius.HeliusTokenTransfer;
import com.riskscoring.chainingest.client.dto.helius.HeliusTransaction;
import com.riskscoring.chainingest.config.ChainIngestProperties;
import com.riskscoring.common.model.TransactionSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class SolanaTransactionSnapshotMapperTest {

    private static final long TIMESTAMP = 1700000000L;
    private static final String FEE_PAYER = "fee-payer";

    @Mock
    private ChainIngestProperties properties;

    private final SolanaValues values = new SolanaValues();

    @Test
    void fromAddressIsFeePayerWhenRoutable() {
        lenient().when(properties.maxTokenTransfers()).thenReturn(10);
        SolanaTransactionSnapshotMapper mapper =
                new SolanaTransactionSnapshotMapper(values, new TransactionPartyAggregator(), properties);
        HeliusTransaction transaction = new HeliusTransaction("sig", TIMESTAMP, FEE_PAYER, null,
                List.of(new HeliusNativeTransfer(FEE_PAYER, "recipient", 100L)), null);

        assertThat(mapper.fromHelius(transaction).fromAddress()).isEqualTo(FEE_PAYER);
    }

    @Test
    void fromAddressIsNullWhenFeePayerNotRoutable() {
        lenient().when(properties.maxTokenTransfers()).thenReturn(10);
        SolanaTransactionSnapshotMapper mapper =
                new SolanaTransactionSnapshotMapper(values, new TransactionPartyAggregator(), properties);
        HeliusTransaction transaction = new HeliusTransaction("sig", TIMESTAMP, "", null, null, null);

        assertThat(mapper.fromHelius(transaction).fromAddress()).isNull();
    }

    @Test
    void largestRecipientPrefersNativeOverTokenAndExcludesFeePayer() {
        lenient().when(properties.maxTokenTransfers()).thenReturn(10);
        SolanaTransactionSnapshotMapper mapper =
                new SolanaTransactionSnapshotMapper(values, new TransactionPartyAggregator(), properties);
        HeliusTransaction transaction = new HeliusTransaction("sig", TIMESTAMP, FEE_PAYER, null,
                List.of(new HeliusNativeTransfer(FEE_PAYER, FEE_PAYER, 500L),
                        new HeliusNativeTransfer(FEE_PAYER, "native-recipient", 200L)),
                List.of(new HeliusTokenTransfer(FEE_PAYER, "token-recipient", "mint", BigDecimal.valueOf(999))));

        assertThat(mapper.fromHelius(transaction).toAddress()).isEqualTo("native-recipient");
    }

    @Test
    void largestRecipientFallsBackToTokenTransferWhenNoQualifyingNative() {
        lenient().when(properties.maxTokenTransfers()).thenReturn(10);
        SolanaTransactionSnapshotMapper mapper =
                new SolanaTransactionSnapshotMapper(values, new TransactionPartyAggregator(), properties);
        HeliusTransaction transaction = new HeliusTransaction("sig", TIMESTAMP, FEE_PAYER, null, null,
                List.of(new HeliusTokenTransfer(FEE_PAYER, "token-recipient", "mint", BigDecimal.TEN)));

        assertThat(mapper.fromHelius(transaction).toAddress()).isEqualTo("token-recipient");
    }

    @Test
    void tokenTransferCountReflectsFullListSizeEvenWhenTruncated() {
        lenient().when(properties.maxTokenTransfers()).thenReturn(1);
        SolanaTransactionSnapshotMapper mapper =
                new SolanaTransactionSnapshotMapper(values, new TransactionPartyAggregator(), properties);
        HeliusTransaction transaction = new HeliusTransaction("sig", TIMESTAMP, FEE_PAYER, null, null,
                List.of(new HeliusTokenTransfer(FEE_PAYER, "r1", "mint1", BigDecimal.ONE),
                        new HeliusTokenTransfer(FEE_PAYER, "r2", "mint2", BigDecimal.ONE)));

        TransactionSnapshot snapshot = mapper.fromHelius(transaction);

        assertThat(snapshot.tokenTransferCount()).isEqualTo(2);
        assertThat(snapshot.tokenTransfers()).hasSize(1);
    }

    @Test
    void nestedTransferCountIsAlwaysZero() {
        lenient().when(properties.maxTokenTransfers()).thenReturn(10);
        SolanaTransactionSnapshotMapper mapper =
                new SolanaTransactionSnapshotMapper(values, new TransactionPartyAggregator(), properties);
        HeliusTransaction transaction = new HeliusTransaction("sig", TIMESTAMP, FEE_PAYER, null, null, null);

        assertThat(mapper.fromHelius(transaction).nestedTransferCount()).isZero();
    }

    @Test
    void tokenTransferSymbolIsAlwaysNull() {
        lenient().when(properties.maxTokenTransfers()).thenReturn(10);
        SolanaTransactionSnapshotMapper mapper =
                new SolanaTransactionSnapshotMapper(values, new TransactionPartyAggregator(), properties);
        HeliusTransaction transaction = new HeliusTransaction("sig", TIMESTAMP, FEE_PAYER, null, null,
                List.of(new HeliusTokenTransfer(FEE_PAYER, "recipient", "mint", BigDecimal.ONE)));

        assertThat(mapper.fromHelius(transaction).tokenTransfers().getFirst().symbol()).isNull();
    }
}
