package com.riskscoring.chainingest.mapper;

import com.riskscoring.chainingest.client.dto.Transfer;
import com.riskscoring.common.model.TransferDirection;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Optional;

@Component
public class TransferDirectionResolver {

    public Optional<Transfer> resolve(ChainAddressValues values, String owner,
                                      String from, String to, BigInteger valueNative, Instant at) {
        String sender = values.address(from);
        String recipient = values.address(to);

        if (owner.equals(sender) && values.isRoutable(recipient) && !recipient.equals(owner)) {
            return Optional.of(new Transfer(recipient, TransferDirection.OUT, valueNative, at));
        }

        if (owner.equals(recipient) && values.isRoutable(sender) && !sender.equals(owner)) {
            return Optional.of(new Transfer(sender, TransferDirection.IN, valueNative, at));
        }

        return Optional.empty();
    }
}
