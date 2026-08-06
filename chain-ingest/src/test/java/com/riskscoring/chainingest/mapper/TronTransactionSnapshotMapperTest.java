package com.riskscoring.chainingest.mapper;

import com.riskscoring.chainingest.client.dto.trongrid.TronContract;
import com.riskscoring.chainingest.client.dto.trongrid.TronContractValue;
import com.riskscoring.chainingest.client.dto.trongrid.TronParameter;
import com.riskscoring.chainingest.client.dto.trongrid.TronRawData;
import com.riskscoring.chainingest.client.dto.trongrid.TronRet;
import com.riskscoring.chainingest.client.dto.trongrid.TronTokenInfo;
import com.riskscoring.chainingest.client.dto.trongrid.TronTransaction;
import com.riskscoring.chainingest.client.dto.trongrid.TronTransactionInfo;
import com.riskscoring.chainingest.client.dto.trongrid.TronTrc20Transfer;
import com.riskscoring.chainingest.config.ChainIngestProperties;
import com.riskscoring.common.model.TokenTransfer;
import com.riskscoring.common.model.TransactionSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class TronTransactionSnapshotMapperTest {

    private static final String OWNER = "Towner";
    private static final long BLOCK_TIMESTAMP = 1700000000000L;

    @Mock
    private ChainIngestProperties properties;

    private final TronValues values = new TronValues(new TronAddressCodec());

    private TronTransactionSnapshotMapper mapper() {
        lenient().when(properties.maxTokenTransfers()).thenReturn(10);
        return new TronTransactionSnapshotMapper(values, new TransactionPartyAggregator(), properties);
    }

    @Test
    void senderAndNativeAmountDerivedFromContractValue() {
        TronTransaction transaction = transaction(OWNER, "Trecipient", 100L, List.of());
        TronTransactionInfo info = new TronTransactionInfo(BLOCK_TIMESTAMP);

        TransactionSnapshot snapshot = mapper().fromTronGrid(transaction, info, List.of());

        assertThat(snapshot.fromAddress()).isEqualTo(OWNER);
        assertThat(snapshot.valueNative()).isEqualTo("100");
    }

    @Test
    void senderAndAmountFallBackToEmptyAndZeroWhenNoContractPresent() {
        TronTransaction transaction = new TronTransaction("txid", BLOCK_TIMESTAMP, List.of(), new TronRawData(List.of()));
        TronTransactionInfo info = new TronTransactionInfo(BLOCK_TIMESTAMP);

        TransactionSnapshot snapshot = mapper().fromTronGrid(transaction, info, List.of());

        assertThat(snapshot.fromAddress()).isNull();
        assertThat(snapshot.valueNative()).isEqualTo("0");
    }

    @Test
    void recipientPrefersNativeContractToAddressWhenRoutable() {
        TronTransaction transaction = transaction(OWNER, "Trecipient", 100L, List.of());
        TronTransactionInfo info = new TronTransactionInfo(BLOCK_TIMESTAMP);

        assertThat(mapper().fromTronGrid(transaction, info, List.of()).toAddress()).isEqualTo("Trecipient");
    }

    @Test
    void recipientFallsBackToLargestTokenRecipientWhenNoNativeRecipient() {
        TronTransaction transaction = new TronTransaction("txid", BLOCK_TIMESTAMP, List.of(), new TronRawData(List.of()));
        TronTransactionInfo info = new TronTransactionInfo(BLOCK_TIMESTAMP);
        List<TronTrc20Transfer> tokenTransfers = List.of(
                new TronTrc20Transfer("txid", BLOCK_TIMESTAMP, OWNER, "Tsmall", "10", null),
                new TronTrc20Transfer("txid", BLOCK_TIMESTAMP, OWNER, "Tbig", "1000", null));

        assertThat(mapper().fromTronGrid(transaction, info, tokenTransfers).toAddress()).isEqualTo("Tbig");
    }

    @Test
    void tokenTransfersExcludeRowsWithNullTokenInfoAndTruncateByProperty() {
        lenient().when(properties.maxTokenTransfers()).thenReturn(1);
        TronTransactionSnapshotMapper mapper = new TronTransactionSnapshotMapper(values, new TransactionPartyAggregator(), properties);
        TronTransaction transaction = new TronTransaction("txid", BLOCK_TIMESTAMP, List.of(), new TronRawData(List.of()));
        TronTransactionInfo info = new TronTransactionInfo(BLOCK_TIMESTAMP);
        List<TronTrc20Transfer> tokenTransfers = List.of(
                new TronTrc20Transfer("txid", BLOCK_TIMESTAMP, OWNER, "Tr1", "1000000", new TronTokenInfo("USDT", "Ttoken", 6)),
                new TronTrc20Transfer("txid", BLOCK_TIMESTAMP, OWNER, "Tr2", "500", null));

        TransactionSnapshot snapshot = mapper.fromTronGrid(transaction, info, tokenTransfers);

        assertThat(snapshot.tokenTransferCount()).isEqualTo(2);
        assertThat(snapshot.tokenTransfers()).containsExactly(
                new TokenTransfer("USDT", "Ttoken", OWNER, "Tr1", "1.000000"));
    }

    @Test
    void blockTimestampComesFromTransactionInfo() {
        TronTransaction transaction = transaction(OWNER, "Trecipient", 100L, List.of());
        TronTransactionInfo info = new TronTransactionInfo(BLOCK_TIMESTAMP);

        assertThat(mapper().fromTronGrid(transaction, info, List.of()).blockTimestamp())
                .isEqualTo(Instant.ofEpochMilli(BLOCK_TIMESTAMP));
    }

    private static TronTransaction transaction(String from, String to, long amount, List<TronRet> ret) {
        TronContractValue value = new TronContractValue(from, to, amount);
        TronContract contract = new TronContract("TransferContract", new TronParameter(value));
        return new TronTransaction("txid", BLOCK_TIMESTAMP, ret, new TronRawData(List.of(contract)));
    }
}
