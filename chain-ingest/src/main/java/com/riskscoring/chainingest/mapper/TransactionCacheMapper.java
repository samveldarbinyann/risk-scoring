package com.riskscoring.chainingest.mapper;

import com.riskscoring.chainingest.entity.TransactionCache;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.TokenTransfer;
import com.riskscoring.common.model.TransactionParty;
import com.riskscoring.common.model.TransactionSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.math.BigInteger;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TransactionCacheMapper {

    private static final TypeReference<List<TransactionParty>> PARTY_LIST = new TypeReference<>() {
    };

    private static final TypeReference<List<TokenTransfer>> TOKEN_TRANSFER_LIST = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public TransactionSnapshot toSnapshot(TransactionCache cache) {
        return new TransactionSnapshot(
                cache.getTxHash(),
                cache.getFromAddress(),
                cache.getToAddress(),
                cache.getValueNative().toString(),
                cache.isSuccess(),
                cache.getBlockTimestamp(),
                objectMapper.readValue(cache.getParties(), PARTY_LIST),
                cache.getNestedTransferCount(),
                cache.getTokenTransferCount(),
                objectMapper.readValue(cache.getTokenTransfers(), TOKEN_TRANSFER_LIST),
                cache.getFetchedAt());
    }

    public TransactionCache toEntity(Chain chain, TransactionSnapshot snapshot) {
        return TransactionCache.builder()
                .id(UUID.randomUUID())
                .chain(chain)
                .txHash(snapshot.hash())
                .fromAddress(snapshot.fromAddress())
                .toAddress(snapshot.toAddress())
                .valueNative(new BigInteger(snapshot.valueNative()))
                .success(snapshot.success())
                .blockTimestamp(snapshot.blockTimestamp())
                .parties(objectMapper.writeValueAsString(snapshot.parties()))
                .nestedTransferCount(snapshot.nestedTransferCount())
                .tokenTransferCount(snapshot.tokenTransferCount())
                .tokenTransfers(objectMapper.writeValueAsString(snapshot.tokenTransfers()))
                .fetchedAt(snapshot.observedAt())
                .build();
    }
}
