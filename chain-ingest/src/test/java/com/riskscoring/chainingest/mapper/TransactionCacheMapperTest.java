package com.riskscoring.chainingest.mapper;

import com.riskscoring.chainingest.entity.TransactionCache;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.TokenTransfer;
import com.riskscoring.common.model.TransactionParty;
import com.riskscoring.common.model.TransactionRole;
import com.riskscoring.common.model.TransactionSnapshot;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionCacheMapperTest {

    private static final Instant NOW = Instant.parse("2024-01-01T00:00:00Z");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TransactionCacheMapper mapper = new TransactionCacheMapper(objectMapper);

    @Test
    void toSnapshotDeserializesPartiesAndTokenTransfersFromJson() {
        List<TransactionParty> parties = List.of(new TransactionParty("0xa", TransactionRole.SENDER, "100"));
        List<TokenTransfer> tokenTransfers = List.of(new TokenTransfer("USDC", "0xc", "0xa", "0xb", "10"));
        TransactionCache cache = TransactionCache.builder()
                .id(UUID.randomUUID())
                .chain(Chain.ETHEREUM)
                .txHash("0xhash")
                .fromAddress("0xa")
                .toAddress("0xb")
                .valueNative(BigInteger.valueOf(100))
                .success(true)
                .blockTimestamp(NOW)
                .parties(objectMapper.writeValueAsString(parties))
                .nestedTransferCount(0)
                .tokenTransferCount(1)
                .tokenTransfers(objectMapper.writeValueAsString(tokenTransfers))
                .fetchedAt(NOW)
                .build();

        TransactionSnapshot snapshot = mapper.toSnapshot(cache);

        assertThat(snapshot.hash()).isEqualTo("0xhash");
        assertThat(snapshot.valueNative()).isEqualTo("100");
        assertThat(snapshot.parties()).isEqualTo(parties);
        assertThat(snapshot.tokenTransfers()).isEqualTo(tokenTransfers);
    }

    @Test
    void toEntityGeneratesFreshIdAndSerializesPartiesAndTokenTransfers() {
        List<TransactionParty> parties = List.of(new TransactionParty("0xa", TransactionRole.SENDER, "100"));
        List<TokenTransfer> tokenTransfers = List.of(new TokenTransfer("USDC", "0xc", "0xa", "0xb", "10"));
        TransactionSnapshot snapshot = new TransactionSnapshot(
                "0xhash", "0xa", "0xb", "100", true, NOW, parties, 0, 1, tokenTransfers, NOW);

        TransactionCache entity = mapper.toEntity(Chain.ETHEREUM, snapshot);

        assertThat(entity.getId()).isNotNull();
        assertThat(entity.getChain()).isEqualTo(Chain.ETHEREUM);
        assertThat(entity.getTxHash()).isEqualTo("0xhash");
        assertThat(entity.getValueNative()).isEqualTo(BigInteger.valueOf(100));
        assertThat(objectMapper.readValue(entity.getParties(), TransactionParty[].class))
                .containsExactly(parties.toArray(new TransactionParty[0]));
        assertThat(objectMapper.readValue(entity.getTokenTransfers(), TokenTransfer[].class))
                .containsExactly(tokenTransfers.toArray(new TokenTransfer[0]));
    }
}
