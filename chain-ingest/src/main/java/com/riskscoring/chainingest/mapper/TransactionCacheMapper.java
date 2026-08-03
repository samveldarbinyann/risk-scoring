package com.riskscoring.chainingest.mapper;

import com.riskscoring.chainingest.entity.TransactionCache;
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

    private final ObjectMapper objectMapper;

    public TransactionSnapshot toSnapshot(TransactionCache cache) {
        return new TransactionSnapshot(
                cache.getTxHash(),
                cache.getFromAddress(),
                cache.getToAddress(),
                cache.getValueWei().toString(),
                cache.isSuccess(),
                cache.getBlockTimestamp(),
                objectMapper.readValue(cache.getParties(), PARTY_LIST),
                cache.getInternalTransferCount(),
                cache.getErc20TransferCount(),
                cache.getFetchedAt());
    }

    public TransactionCache toEntity(int chainId, TransactionSnapshot snapshot) {
        return TransactionCache.builder()
                .id(UUID.randomUUID())
                .chainId(chainId)
                .txHash(snapshot.hash())
                .fromAddress(snapshot.fromAddress())
                .toAddress(snapshot.toAddress())
                .valueWei(new BigInteger(snapshot.valueWei()))
                .success(snapshot.success())
                .blockTimestamp(snapshot.blockTimestamp())
                .parties(objectMapper.writeValueAsString(snapshot.parties()))
                .internalTransferCount(snapshot.internalTransferCount())
                .erc20TransferCount(snapshot.erc20TransferCount())
                .fetchedAt(snapshot.observedAt())
                .build();
    }
}
