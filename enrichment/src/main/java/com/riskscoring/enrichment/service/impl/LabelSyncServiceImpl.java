package com.riskscoring.enrichment.service.impl;

import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.LabelCategory;
import com.riskscoring.enrichment.client.OfacSdnClient;
import com.riskscoring.enrichment.client.dto.OfacDigitalCurrencyAddress;
import com.riskscoring.enrichment.entity.Label;
import com.riskscoring.enrichment.mapper.OfacChainMapping;
import com.riskscoring.enrichment.repository.LabelRepository;
import com.riskscoring.enrichment.service.LabelSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LabelSyncServiceImpl implements LabelSyncService {

    private static final String SOURCE = "OFAC SDN";

    private final OfacSdnClient ofacSdnClient;
    private final OfacChainMapping chainMapping;
    private final LabelRepository labelRepository;

    @Override
    @Scheduled(cron = "${enrichment.ofac.sync-cron}")
    public void syncOfacSanctions() {
        List<OfacDigitalCurrencyAddress> fetched;
        try {
            fetched = ofacSdnClient.fetchDigitalCurrencyAddresses();
        } catch (RuntimeException e) {
            log.error("OFAC SDN sync skipped: failed to fetch or parse sdn_advanced.xml", e);
            return;
        }

        Map<String, LabelTarget> desired = toTargets(fetched);
        Map<String, Label> existing = new LinkedHashMap<>();
        labelRepository.findByCategoryAndSource(LabelCategory.SANCTION, SOURCE)
                .forEach(label -> existing.put(key(label.getChain(), label.getAddress()), label));

        Instant now = Instant.now();
        List<Label> toSave = new ArrayList<>();
        int added = 0;
        int updated = 0;

        for (Map.Entry<String, LabelTarget> entry : desired.entrySet()) {
            LabelTarget target = entry.getValue();
            Label existingLabel = existing.get(entry.getKey());

            if (existingLabel == null) {
                toSave.add(Label.builder()
                        .id(UUID.randomUUID())
                        .chain(target.chain())
                        .address(target.address())
                        .category(LabelCategory.SANCTION)
                        .name(target.name())
                        .source(SOURCE)
                        .updatedAt(now)
                        .build());
                added++;
            } else if (!existingLabel.getName().equals(target.name())) {
                existingLabel.setName(target.name());
                existingLabel.setUpdatedAt(now);
                toSave.add(existingLabel);
                updated++;
            }
        }

        List<Label> toDelete = existing.entrySet().stream()
                .filter(entry -> !desired.containsKey(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();

        labelRepository.saveAll(toSave);
        labelRepository.deleteAll(toDelete);

        log.info("OFAC SDN sync complete: fetched={} added={} updated={} removed={} unchanged={}",
                fetched.size(), added, updated, toDelete.size(), desired.size() - added - updated);
    }

    private Map<String, LabelTarget> toTargets(List<OfacDigitalCurrencyAddress> fetched) {
        Map<String, LabelTarget> byKey = new LinkedHashMap<>();
        for (OfacDigitalCurrencyAddress address : fetched) {
            for (Chain chain : chainMapping.chainsFor(address.ticker())) {
                byKey.put(key(chain, address.address()), new LabelTarget(chain, address.address(), address.entityName()));
            }
        }
        return byKey;
    }

    private static String key(Chain chain, String address) {
        return chain.name() + '|' + address;
    }

    private record LabelTarget(Chain chain, String address, String name) {
    }
}
