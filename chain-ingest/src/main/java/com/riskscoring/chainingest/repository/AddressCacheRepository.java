package com.riskscoring.chainingest.repository;

import com.riskscoring.chainingest.entity.AddressCache;
import com.riskscoring.common.model.Chain;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AddressCacheRepository extends JpaRepository<AddressCache, UUID> {

    Optional<AddressCache> findByChainAndAddress(Chain chain, String address);
}