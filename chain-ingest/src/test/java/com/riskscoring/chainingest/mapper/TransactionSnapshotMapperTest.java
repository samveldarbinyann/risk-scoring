package com.riskscoring.chainingest.mapper;

import com.riskscoring.chainingest.client.dto.MoralisErc20Transfer;
import com.riskscoring.chainingest.client.dto.MoralisInternalTransfer;
import com.riskscoring.chainingest.client.dto.MoralisTransaction;
import com.riskscoring.chainingest.config.ChainIngestProperties;
import com.riskscoring.common.model.TokenTransfer;
import com.riskscoring.common.model.TransactionParty;
import com.riskscoring.common.model.TransactionRole;
import com.riskscoring.common.model.TransactionSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class TransactionSnapshotMapperTest {

    private static final String ISO = "2024-01-01T00:00:00Z";
    private static final String ZERO_ADDRESS = "0x0000000000000000000000000000000000000000";

    @Mock
    private ChainIngestProperties properties;

    private final MoralisValues values = new MoralisValues();

    @Test
    void mapsAddressesLowercasedAndValueFromWei() {
        lenient().when(properties.maxTokenTransfers()).thenReturn(10);
        TransactionSnapshotMapper mapper =
                new TransactionSnapshotMapper(values, new TransactionPartyAggregator(), properties);
        MoralisTransaction transaction = new MoralisTransaction("0xHASH", "0xFROM", "0xTO", "1000", ISO,
                "1", null, null);

        TransactionSnapshot snapshot = mapper.fromMoralis(transaction);

        assertThat(snapshot.hash()).isEqualTo("0xhash");
        assertThat(snapshot.fromAddress()).isEqualTo("0xfrom");
        assertThat(snapshot.toAddress()).isEqualTo("0xto");
        assertThat(snapshot.valueNative()).isEqualTo("1000");
        assertThat(snapshot.success()).isTrue();
    }

    @Test
    void successReflectsValuesSucceeded() {
        lenient().when(properties.maxTokenTransfers()).thenReturn(10);
        TransactionSnapshotMapper mapper =
                new TransactionSnapshotMapper(values, new TransactionPartyAggregator(), properties);
        MoralisTransaction failed = new MoralisTransaction("0xhash", "0xfrom", "0xto", "0", ISO, "0", null, null);

        assertThat(mapper.fromMoralis(failed).success()).isFalse();
    }

    @Test
    void tokenTransferCountReflectsFullListSizeEvenWhenTruncated() {
        lenient().when(properties.maxTokenTransfers()).thenReturn(1);
        TransactionSnapshotMapper mapper =
                new TransactionSnapshotMapper(values, new TransactionPartyAggregator(), properties);
        List<MoralisErc20Transfer> tokens = List.of(
                new MoralisErc20Transfer("0xa", "0xb", "0xc1", "USDC", "1"),
                new MoralisErc20Transfer("0xd", "0xe", "0xc2", "USDT", "2"));
        MoralisTransaction transaction = new MoralisTransaction("0xhash", "0xfrom", "0xto", "0", ISO,
                "1", null, tokens);

        TransactionSnapshot snapshot = mapper.fromMoralis(transaction);

        assertThat(snapshot.tokenTransferCount()).isEqualTo(2);
        assertThat(snapshot.tokenTransfers()).hasSize(1);
    }

    @Test
    void nestedTransferCountReflectsFullInternalListSize() {
        lenient().when(properties.maxTokenTransfers()).thenReturn(10);
        TransactionSnapshotMapper mapper =
                new TransactionSnapshotMapper(values, new TransactionPartyAggregator(), properties);
        List<MoralisInternalTransfer> internals = List.of(
                new MoralisInternalTransfer("0xfrom", "0xinternal1", "10"),
                new MoralisInternalTransfer("0xfrom", "0xinternal2", "20"));
        MoralisTransaction transaction = new MoralisTransaction("0xhash", "0xfrom", "0xto", "0", ISO,
                "1", internals, null);

        assertThat(mapper.fromMoralis(transaction).nestedTransferCount()).isEqualTo(2);
    }

    @Test
    void tokenTransfersMapSymbolContractAndAddresses() {
        lenient().when(properties.maxTokenTransfers()).thenReturn(10);
        TransactionSnapshotMapper mapper =
                new TransactionSnapshotMapper(values, new TransactionPartyAggregator(), properties);
        List<MoralisErc20Transfer> tokens = List.of(
                new MoralisErc20Transfer("0xA", "0xB", "0xCONTRACT", "USDC", "5.0"));
        MoralisTransaction transaction = new MoralisTransaction("0xhash", "0xfrom", "0xto", "0", ISO,
                "1", null, tokens);

        assertThat(mapper.fromMoralis(transaction).tokenTransfers())
                .containsExactly(new TokenTransfer("USDC", "0xcontract", "0xa", "0xb", "5.0"));
    }

    @Test
    void partiesAggregatesDirectInternalAndTokenRolesExcludingNonRoutableAddresses() {
        lenient().when(properties.maxTokenTransfers()).thenReturn(10);
        TransactionSnapshotMapper mapper =
                new TransactionSnapshotMapper(values, new TransactionPartyAggregator(), properties);
        List<MoralisInternalTransfer> internals = List.of(
                new MoralisInternalTransfer("0xfrom", "0xinternal", "10"));
        List<MoralisErc20Transfer> tokens = List.of(
                new MoralisErc20Transfer("0xfrom", ZERO_ADDRESS, "0xcontract", "USDC", "1"));
        MoralisTransaction transaction = new MoralisTransaction("0xhash", "0xfrom", "0xto", "100", ISO,
                "1", internals, tokens);

        List<TransactionParty> parties = mapper.fromMoralis(transaction).parties();

        assertThat(parties).contains(
                new TransactionParty("0xfrom", TransactionRole.SENDER, "100"),
                new TransactionParty("0xto", TransactionRole.RECIPIENT, "100"),
                new TransactionParty("0xfrom", TransactionRole.INTERNAL_SENDER, "10"),
                new TransactionParty("0xinternal", TransactionRole.INTERNAL_RECIPIENT, "10"),
                new TransactionParty("0xfrom", TransactionRole.TOKEN_SENDER, "0"));
        assertThat(parties).extracting(TransactionParty::role)
                .doesNotContain(TransactionRole.TOKEN_RECIPIENT);
    }
}
