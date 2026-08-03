package com.riskscoring.chainingest.mapper;

import com.riskscoring.common.model.TransactionParty;
import com.riskscoring.common.model.TransactionRole;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Component
public class TransactionPartyAggregator {

    public List<TransactionParty> aggregate(Stream<TransactionParty> parties) {
        Map<PartyKey, BigInteger> totals = new LinkedHashMap<>();
        parties.forEach(party -> totals.merge(
                new PartyKey(party.address(), party.role()),
                new BigInteger(party.valueNative()),
                BigInteger::add));

        return totals.entrySet().stream()
                .map(entry -> new TransactionParty(
                        entry.getKey().address(), entry.getKey().role(), entry.getValue().toString()))
                .sorted(Comparator.comparing(TransactionParty::role).thenComparing(TransactionParty::address))
                .toList();
    }

    private record PartyKey(String address, TransactionRole role) {
    }
}
