package com.riskscoring.chainingest.mapper;

import com.riskscoring.chainingest.client.dto.Transfer;
import com.riskscoring.chainingest.client.dto.tonapi.TonAccountAddress;
import com.riskscoring.chainingest.client.dto.tonapi.TonAction;
import com.riskscoring.chainingest.client.dto.tonapi.TonEvent;
import com.riskscoring.chainingest.client.dto.tonapi.TonJettonTransferAction;
import com.riskscoring.chainingest.client.dto.tonapi.TonTransferAction;
import com.riskscoring.common.model.TransferDirection;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TonTransferMapperTest {

    private static final String OWNER = "owner";
    private static final long TIMESTAMP = 1700000000L;
    private static final Instant AT = Instant.ofEpochSecond(TIMESTAMP);

    private final TonValues values = new TonValues();
    private final TonTransferMapper mapper = new TonTransferMapper(values, new TransferDirectionResolver());

    @Test
    void scamEventsAreFilteredOutEntirely() {
        TonAction action = new TonAction("TonTransfer", "ok",
                new TonTransferAction(new TonAccountAddress(OWNER), new TonAccountAddress("recipient"), 100L), null);
        TonEvent scamEvent = new TonEvent("evt", TIMESTAMP, true, List.of(action));

        assertThat(mapper.fromEvents(List.of(scamEvent), OWNER)).isEmpty();
    }

    @Test
    void onlySucceededTransferActionsProduceTransfers() {
        TonAction failed = new TonAction("TonTransfer", "failed",
                new TonTransferAction(new TonAccountAddress(OWNER), new TonAccountAddress("recipient"), 100L), null);
        TonEvent event = new TonEvent("evt", TIMESTAMP, false, List.of(failed));

        assertThat(mapper.fromEvents(List.of(event), OWNER)).isEmpty();
    }

    @Test
    void tonTransferActionResolvesToTransfer() {
        TonAction action = new TonAction("TonTransfer", "ok",
                new TonTransferAction(new TonAccountAddress(OWNER), new TonAccountAddress("recipient"), 100L), null);
        TonEvent event = new TonEvent("evt", TIMESTAMP, false, List.of(action));

        List<Transfer> transfers = mapper.fromEvents(List.of(event), OWNER);

        assertThat(transfers).containsExactly(
                new Transfer("recipient", TransferDirection.OUT, BigInteger.valueOf(100L), AT));
    }

    @Test
    void fallsBackToJettonTransferWhenNoNativeTransferPresent() {
        TonAction action = new TonAction("JettonTransfer", "ok", null,
                new TonJettonTransferAction(new TonAccountAddress(OWNER), new TonAccountAddress("recipient"),
                        "500", null));
        TonEvent event = new TonEvent("evt", TIMESTAMP, false, List.of(action));

        List<Transfer> transfers = mapper.fromEvents(List.of(event), OWNER);

        assertThat(transfers).containsExactly(
                new Transfer("recipient", TransferDirection.OUT, BigInteger.ZERO, AT));
    }
}
