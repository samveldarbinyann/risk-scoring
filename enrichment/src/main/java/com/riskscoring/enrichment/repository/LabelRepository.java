package com.riskscoring.enrichment.repository;

import com.riskscoring.common.model.Chain;
import com.riskscoring.enrichment.entity.Label;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface LabelRepository extends JpaRepository<Label, UUID> {

    List<Label> findByChainAndAddressIn(Chain chain, Collection<String> addresses);
}