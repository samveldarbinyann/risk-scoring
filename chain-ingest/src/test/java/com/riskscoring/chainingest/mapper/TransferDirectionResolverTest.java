package com.riskscoring.chainingest.mapper;

import com.riskscoring.chainingest.client.dto.Transfer;
import com.riskscoring.common.model.TransferDirection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class TransferDirectionResolverTest {

    private static final String OWNER = "owner";
    private static final BigInteger VALUE = BigInteger.TEN;
    private static final Instant AT = Instant.parse("2024-01-01T00:00:00Z");

    @Mock
    private ChainAddressValues values;

    private final TransferDirectionResolver resolver = new TransferDirectionResolver();

    @Test
    void ownerAsSenderWithRoutableRecipientResolvesToOut() {
        stub("raw-from", OWNER, "raw-to", "counterparty");

        Optional<Transfer> transfer = resolver.resolve(values, OWNER, "raw-from", "raw-to", VALUE, AT);

        assertThat(transfer).contains(new Transfer("counterparty", TransferDirection.OUT, VALUE, AT));
    }

    @Test
    void ownerAsRecipientWithRoutableSenderResolvesToIn() {
        stub("raw-from", "counterparty", "raw-to", OWNER);

        Optional<Transfer> transfer = resolver.resolve(values, OWNER, "raw-from", "raw-to", VALUE, AT);

        assertThat(transfer).contains(new Transfer("counterparty", TransferDirection.IN, VALUE, AT));
    }

    @Test
    void selfTransferResolvesToEmpty() {
        stub("raw-from", OWNER, "raw-to", OWNER);

        Optional<Transfer> transfer = resolver.resolve(values, OWNER, "raw-from", "raw-to", VALUE, AT);

        assertThat(transfer).isEmpty();
    }

    @Test
    void ownerAsSenderWithNonRoutableRecipientResolvesToEmpty() {
        lenient().when(values.address("raw-from")).thenReturn(OWNER);
        lenient().when(values.address("raw-to")).thenReturn("non-routable");
        lenient().when(values.isRoutable("non-routable")).thenReturn(false);

        Optional<Transfer> transfer = resolver.resolve(values, OWNER, "raw-from", "raw-to", VALUE, AT);

        assertThat(transfer).isEmpty();
    }

    @Test
    void transferBetweenTwoOtherPartiesResolvesToEmpty() {
        lenient().when(values.address("raw-from")).thenReturn("party-a");
        lenient().when(values.address("raw-to")).thenReturn("party-b");

        Optional<Transfer> transfer = resolver.resolve(values, OWNER, "raw-from", "raw-to", VALUE, AT);

        assertThat(transfer).isEmpty();
    }

    private void stub(String rawFrom, String sender, String rawTo, String recipient) {
        lenient().when(values.address(rawFrom)).thenReturn(sender);
        lenient().when(values.address(rawTo)).thenReturn(recipient);
        lenient().when(values.isRoutable(sender)).thenReturn(true);
        lenient().when(values.isRoutable(recipient)).thenReturn(true);
    }
}
