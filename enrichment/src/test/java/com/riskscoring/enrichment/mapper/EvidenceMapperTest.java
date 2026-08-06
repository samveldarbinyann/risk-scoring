package com.riskscoring.enrichment.mapper;

import com.riskscoring.common.event.ChainFetched;
import com.riskscoring.common.model.AddressEvidence;
import com.riskscoring.common.model.AddressFacts;
import com.riskscoring.common.model.AddressSnapshot;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.EvidenceBundle;
import com.riskscoring.common.model.Heuristics;
import com.riskscoring.common.model.Language;
import com.riskscoring.common.model.ScanTarget;
import com.riskscoring.common.model.TransactionEvidence;
import com.riskscoring.common.model.TransactionFacts;
import com.riskscoring.common.model.TransactionHeuristics;
import com.riskscoring.common.model.TransactionSnapshot;
import com.riskscoring.enrichment.entity.EvidenceRecord;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EvidenceMapperTest {

    private static final Instant NOW = Instant.parse("2024-06-01T00:00:00Z");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EvidenceMapper mapper = new EvidenceMapper(objectMapper);

    @Test
    void toRecordTakesTargetAndChainFromEvidenceNotEvent() {
        ChainFetched event = addressEvent("0xevent-target", Chain.ETHEREUM);
        AddressEvidence evidence = addressEvidence("0xevidence-target", Chain.BITCOIN);

        EvidenceRecord record = mapper.toRecord(event, evidence, NOW);

        assertThat(record.getTarget()).isEqualTo("0xevidence-target");
        assertThat(record.getChain()).isEqualTo(Chain.BITCOIN);
    }

    @Test
    void toRecordMapsScanIdTargetTypeAndCreatedAtFromEventAndArgument() {
        ChainFetched event = addressEvent("0xtarget", Chain.ETHEREUM);
        AddressEvidence evidence = addressEvidence("0xtarget", Chain.ETHEREUM);

        EvidenceRecord record = mapper.toRecord(event, evidence, NOW);

        assertThat(record.getId()).isNotNull();
        assertThat(record.getScanId()).isEqualTo(event.scanId());
        assertThat(record.getTargetType()).isEqualTo(ScanTarget.ADDRESS);
        assertThat(record.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void toRecordSerializesAddressEvidenceAsRoundTrippablePayload() {
        ChainFetched event = addressEvent("0xtarget", Chain.ETHEREUM);
        AddressEvidence evidence = addressEvidence("0xtarget", Chain.ETHEREUM);

        EvidenceRecord record = mapper.toRecord(event, evidence, NOW);

        EvidenceBundle roundTripped = objectMapper.readValue(record.getPayload(), EvidenceBundle.class);
        assertThat(roundTripped).isEqualTo(evidence);
    }

    @Test
    void toRecordSerializesTransactionEvidenceAsRoundTrippablePayload() {
        ChainFetched event = transactionEvent("0xhash", Chain.ETHEREUM);
        TransactionEvidence evidence = transactionEvidence("0xhash", Chain.ETHEREUM);

        EvidenceRecord record = mapper.toRecord(event, evidence, NOW);

        assertThat(record.getTargetType()).isEqualTo(ScanTarget.TRANSACTION);
        EvidenceBundle roundTripped = objectMapper.readValue(record.getPayload(), EvidenceBundle.class);
        assertThat(roundTripped).isEqualTo(evidence);
    }

    @Test
    void toRecordGeneratesDistinctRandomIdsAcrossCalls() {
        ChainFetched event = addressEvent("0xtarget", Chain.ETHEREUM);
        AddressEvidence evidence = addressEvidence("0xtarget", Chain.ETHEREUM);

        EvidenceRecord first = mapper.toRecord(event, evidence, NOW);
        EvidenceRecord second = mapper.toRecord(event, evidence, NOW);

        assertThat(first.getId()).isNotEqualTo(second.getId());
    }

    private static ChainFetched addressEvent(String target, Chain chain) {
        AddressFacts facts = new AddressFacts(
                new AddressSnapshot(1, 1, "0", List.of(), null, NOW, false, NOW), List.of());
        return new ChainFetched(UUID.randomUUID(), ScanTarget.ADDRESS, target, chain, facts, Language.EN, NOW);
    }

    private static ChainFetched transactionEvent(String target, Chain chain) {
        TransactionFacts facts = new TransactionFacts(
                new TransactionSnapshot(target, "0xfrom", "0xto", "0", true, NOW, List.of(), 0, 0, List.of(), NOW));
        return new ChainFetched(UUID.randomUUID(), ScanTarget.TRANSACTION, target, chain, facts, Language.EN, NOW);
    }

    private static AddressEvidence addressEvidence(String target, Chain chain) {
        return new AddressEvidence(target, chain, NOW, null, 0, 0, false, "0", List.of(), 0, List.of(), null,
                new Heuristics(null, null, false, 0, 0));
    }

    private static TransactionEvidence transactionEvidence(String target, Chain chain) {
        return new TransactionEvidence(target, chain, NOW, "0xfrom", "0xto", "0", true, NOW, 0, 0, List.of(),
                List.of(), List.of(), null,
                new TransactionHeuristics(false, true, false, false, false, false, 0));
    }
}
