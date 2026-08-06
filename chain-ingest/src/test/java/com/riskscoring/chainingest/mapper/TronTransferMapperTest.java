package com.riskscoring.chainingest.mapper;

import com.riskscoring.chainingest.client.dto.Transfer;
import com.riskscoring.chainingest.client.dto.trongrid.TronContract;
import com.riskscoring.chainingest.client.dto.trongrid.TronContractValue;
import com.riskscoring.chainingest.client.dto.trongrid.TronParameter;
import com.riskscoring.chainingest.client.dto.trongrid.TronRawData;
import com.riskscoring.chainingest.client.dto.trongrid.TronRet;
import com.riskscoring.chainingest.client.dto.trongrid.TronTransaction;
import com.riskscoring.chainingest.client.dto.trongrid.TronTrc20Transfer;
import com.riskscoring.common.model.TransferDirection;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TronTransferMapperTest {

    private static final String OWNER = "Towner";
    private static final long BLOCK_TIMESTAMP = 1700000000000L;
    private static final Instant AT = Instant.ofEpochMilli(BLOCK_TIMESTAMP);

    private final TronValues values = new TronValues(new TronAddressCodec());
    private final TronTransferMapper mapper = new TronTransferMapper(values, new TransferDirectionResolver());

    @Test
    void nativeTransferContractProducesTransfer() {
        TronTransaction transaction = transferContractTransaction("TransferContract", OWNER, "Trecipient", 100L);

        List<Transfer> transfers = mapper.fromNative(List.of(transaction), OWNER);

        assertThat(transfers).containsExactly(new Transfer("Trecipient", TransferDirection.OUT, BigInteger.valueOf(100L), AT));
    }

    @Test
    void triggerSmartContractTransactionsAreFilteredOutOfNativeTransfers() {
        TronTransaction transaction = transferContractTransaction("TriggerSmartContract", OWNER, "Trecipient", 100L);

        assertThat(mapper.fromNative(List.of(transaction), OWNER)).isEmpty();
    }

    @Test
    void failedTransactionsAreFilteredOutOfNativeTransfers() {
        TronContractValue value = new TronContractValue(OWNER, "Trecipient", 100L);
        TronContract contract = new TronContract("TransferContract", new TronParameter(value));
        TronTransaction failed = new TronTransaction("txid", BLOCK_TIMESTAMP,
                List.of(new TronRet("REVERT")), new TronRawData(List.of(contract)));

        assertThat(mapper.fromNative(List.of(failed), OWNER)).isEmpty();
    }

    @Test
    void fromTrc20AlwaysUsesZeroValueAndTimestampFromBlockTimestamp() {
        TronTrc20Transfer transfer = new TronTrc20Transfer("txid", BLOCK_TIMESTAMP, OWNER, "Trecipient", "500", null);

        List<Transfer> transfers = mapper.fromTrc20(List.of(transfer), OWNER);

        assertThat(transfers).containsExactly(new Transfer("Trecipient", TransferDirection.OUT, BigInteger.ZERO, AT));
    }

    private static TronTransaction transferContractTransaction(String contractType, String from, String to, long amount) {
        TronContractValue value = new TronContractValue(from, to, amount);
        TronContract contract = new TronContract(contractType, new TronParameter(value));
        return new TronTransaction("txid", BLOCK_TIMESTAMP, List.of(), new TronRawData(List.of(contract)));
    }
}
