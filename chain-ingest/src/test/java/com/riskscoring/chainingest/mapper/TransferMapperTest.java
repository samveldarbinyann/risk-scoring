package com.riskscoring.chainingest.mapper;

import com.riskscoring.chainingest.client.dto.MoralisErc20Transfer;
import com.riskscoring.chainingest.client.dto.MoralisInternalTransfer;
import com.riskscoring.chainingest.client.dto.MoralisTransaction;
import com.riskscoring.chainingest.client.dto.Transfer;
import com.riskscoring.common.model.TransferDirection;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TransferMapperTest {

    private static final String OWNER = "0xowner";
    private static final String ISO = "2024-01-01T00:00:00Z";
    private static final Instant AT = Instant.parse(ISO);

    private final MoralisValues values = new MoralisValues();
    private final TransferMapper mapper = new TransferMapper(values, new TransferDirectionResolver());

    @Test
    void failedTransactionsAreFilteredOutEntirely() {
        MoralisTransaction failed = new MoralisTransaction("hash", OWNER, "0xrecipient", "100", ISO,
                "0",
                List.of(new MoralisInternalTransfer(OWNER, "0xinternal", "50")),
                List.of(new MoralisErc20Transfer(OWNER, "0xtoken", "0xcontract", "USDC", "1")));

        assertThat(mapper.fromTransactions(List.of(failed), OWNER)).isEmpty();
    }

    @Test
    void directTransferIsIncludedWhenDirectionResolves() {
        MoralisTransaction transaction = new MoralisTransaction("hash", OWNER, "0xrecipient", "100", ISO,
                "1", null, null);

        List<Transfer> transfers = mapper.fromTransactions(List.of(transaction), OWNER);

        assertThat(transfers).containsExactly(
                new Transfer("0xrecipient", TransferDirection.OUT, BigInteger.valueOf(100L), AT));
    }

    @Test
    void nullInternalTransactionsListProducesNoInternalTransfersWithoutError() {
        MoralisTransaction transaction = new MoralisTransaction("hash", OWNER, "0xrecipient", "100", ISO,
                "1", null, null);

        assertThat(mapper.fromTransactions(List.of(transaction), OWNER)).hasSize(1);
    }

    @Test
    void erc20TransfersAlwaysUseZeroValueButStillResolveDirection() {
        MoralisTransaction transaction = new MoralisTransaction("hash", "0xother", "0xother2", "0", ISO,
                "1", null,
                List.of(new MoralisErc20Transfer(OWNER, "0xrecipient", "0xcontract", "USDC", "1")));

        List<Transfer> transfers = mapper.fromTransactions(List.of(transaction), OWNER);

        assertThat(transfers).containsExactly(
                new Transfer("0xrecipient", TransferDirection.OUT, BigInteger.ZERO, AT));
    }

    @Test
    void timestampDelegatesToValues() {
        assertThat(mapper.timestamp(ISO)).isEqualTo(AT);
    }
}
