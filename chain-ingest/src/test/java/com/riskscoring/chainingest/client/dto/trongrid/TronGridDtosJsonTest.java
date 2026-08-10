package com.riskscoring.chainingest.client.dto.trongrid;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class TronGridDtosJsonTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void transactionWithNullBlockTimestampDeserializes() throws Exception {
        TronTransaction transaction = mapper.readValue(
                "{\"txID\":\"abc\",\"block_timestamp\":null,\"ret\":[],\"raw_data\":{\"contract\":[]}}",
                TronTransaction.class);

        assertThat(transaction.blockTimestamp()).isNull();
    }

    @Test
    void trc20TransferWithNullBlockTimestampAndNullDecimalsDeserializes() throws Exception {
        TronTrc20Response response = mapper.readValue(
                "{\"data\":[{\"transaction_id\":\"t\",\"block_timestamp\":null,\"from\":\"a\",\"to\":\"b\","
                        + "\"value\":\"1\",\"token_info\":{\"symbol\":\"S\",\"address\":\"A\",\"decimals\":null}}]}",
                TronTrc20Response.class);

        assertThat(response.data()).hasSize(1);
        assertThat(response.data().getFirst().blockTimestamp()).isNull();
        assertThat(response.data().getFirst().tokenInfo().decimals()).isNull();
    }
}
