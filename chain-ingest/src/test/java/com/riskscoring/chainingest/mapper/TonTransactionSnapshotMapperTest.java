package com.riskscoring.chainingest.mapper;

import com.riskscoring.chainingest.client.dto.tonapi.TonAccountAddress;
import com.riskscoring.chainingest.client.dto.tonapi.TonAction;
import com.riskscoring.chainingest.client.dto.tonapi.TonEvent;
import com.riskscoring.chainingest.client.dto.tonapi.TonJettonPreview;
import com.riskscoring.chainingest.client.dto.tonapi.TonJettonTransferAction;
import com.riskscoring.chainingest.client.dto.tonapi.TonTransferAction;
import com.riskscoring.chainingest.config.ChainIngestProperties;
import com.riskscoring.common.model.TokenTransfer;
import com.riskscoring.common.model.TransactionSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class TonTransactionSnapshotMapperTest {

    private static final long TIMESTAMP = 1700000000L;
    private static final String SENDER = "sender";
    private static final String HASH = "evt-hash";

    @Mock
    private ChainIngestProperties properties;

    private final TonValues values = new TonValues();

    private TonTransactionSnapshotMapper mapper() {
        lenient().when(properties.maxTokenTransfers()).thenReturn(10);
        return new TonTransactionSnapshotMapper(values, new TransactionPartyAggregator(), properties);
    }

    @Test
    void senderIsFirstNativeTransferSenderWhenPresent() {
        TonAction action = tonTransferAction(SENDER, "recipient", 100L);
        TonEvent event = new TonEvent("evt", TIMESTAMP, false, List.of(action));

        assertThat(mapper().fromTonApi(event, HASH).fromAddress()).isEqualTo(SENDER);
    }

    @Test
    void senderFallsBackToJettonTransferSenderWhenNoNativeTransfers() {
        TonAction action = jettonTransferAction(SENDER, "recipient", "100", null);
        TonEvent event = new TonEvent("evt", TIMESTAMP, false, List.of(action));

        assertThat(mapper().fromTonApi(event, HASH).fromAddress()).isEqualTo(SENDER);
    }

    @Test
    void senderIsNullWhenNoTransfersAtAll() {
        TonEvent event = new TonEvent("evt", TIMESTAMP, false, List.of());

        assertThat(mapper().fromTonApi(event, HASH).fromAddress()).isNull();
    }

    @Test
    void largestRecipientPrefersNativeAndExcludesSender() {
        TonAction selfTransfer = tonTransferAction(SENDER, SENDER, 900L);
        TonAction realTransfer = tonTransferAction(SENDER, "native-recipient", 100L);
        TonAction jettonTransfer = jettonTransferAction(SENDER, "jetton-recipient", "999",
                new TonJettonPreview("jetton-addr", "USDT", 6));
        TonEvent event = new TonEvent("evt", TIMESTAMP, false, List.of(selfTransfer, realTransfer, jettonTransfer));

        assertThat(mapper().fromTonApi(event, HASH).toAddress()).isEqualTo("native-recipient");
    }

    @Test
    void largestRecipientFallsBackToJettonWhenNoQualifyingNativeTransfer() {
        TonAction jettonTransfer = jettonTransferAction(SENDER, "jetton-recipient", "100",
                new TonJettonPreview("jetton-addr", "USDT", 6));
        TonEvent event = new TonEvent("evt", TIMESTAMP, false, List.of(jettonTransfer));

        assertThat(mapper().fromTonApi(event, HASH).toAddress()).isEqualTo("jetton-recipient");
    }

    @Test
    void largestRecipientHandlesJettonWithNullMetadataAsZeroDecimals() {
        TonAction jettonTransfer = jettonTransferAction(SENDER, "jetton-recipient", "100", null);
        TonEvent event = new TonEvent("evt", TIMESTAMP, false, List.of(jettonTransfer));

        assertThat(mapper().fromTonApi(event, HASH).toAddress()).isEqualTo("jetton-recipient");
    }

    @Test
    void tokenTransfersOnlyIncludeRowsWithJettonMetadataAndScaleByDecimals() {
        TonAction withMetadata = jettonTransferAction(SENDER, "r1", "1000000",
                new TonJettonPreview("jetton-addr", "USDT", 6));
        TonAction withoutMetadata = jettonTransferAction(SENDER, "r2", "500", null);
        TonEvent event = new TonEvent("evt", TIMESTAMP, false, List.of(withMetadata, withoutMetadata));

        TransactionSnapshot snapshot = mapper().fromTonApi(event, HASH);

        assertThat(snapshot.tokenTransferCount()).isEqualTo(2);
        assertThat(snapshot.tokenTransfers()).containsExactly(
                new TokenTransfer("USDT", "jetton-addr", SENDER, "r1", "1.000000"));
    }

    private static TonAction tonTransferAction(String sender, String recipient, long amount) {
        return new TonAction("TonTransfer", "ok",
                new TonTransferAction(new TonAccountAddress(sender), new TonAccountAddress(recipient), amount), null);
    }

    private static TonAction jettonTransferAction(String sender, String recipient, String amount,
                                                  TonJettonPreview jetton) {
        return new TonAction("JettonTransfer", "ok", null,
                new TonJettonTransferAction(new TonAccountAddress(sender), new TonAccountAddress(recipient),
                        amount, jetton));
    }
}
