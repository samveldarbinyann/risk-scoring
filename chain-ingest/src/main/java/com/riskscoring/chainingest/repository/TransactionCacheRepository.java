package com.riskscoring.chainingest.repository;

import com.riskscoring.chainingest.entity.TransactionCache;
import com.riskscoring.common.model.Chain;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TransactionCacheRepository extends JpaRepository<TransactionCache, UUID> {

    Optional<TransactionCache> findByChainAndTxHash(Chain chain, String txHash);
}
