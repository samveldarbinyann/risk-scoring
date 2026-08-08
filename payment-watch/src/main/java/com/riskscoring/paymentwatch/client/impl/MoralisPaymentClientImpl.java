package com.riskscoring.paymentwatch.client.impl;

import com.riskscoring.paymentwatch.client.HttpCallTemplate;
import com.riskscoring.paymentwatch.client.MoralisPaymentClient;
import com.riskscoring.paymentwatch.client.dto.MoralisTokenTransfer;
import com.riskscoring.paymentwatch.client.dto.MoralisTokenTransfersEnvelope;
import com.riskscoring.paymentwatch.config.PaymentWatchProperties;
import com.riskscoring.paymentwatch.exception.PaymentWatchException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MoralisPaymentClientImpl implements MoralisPaymentClient {

    private static final String PATH_ERC20_TRANSFERS = "/%s/erc20/transfers";
    private static final String PARAM_CHAIN = "chain";
    private static final String PARAM_CONTRACT_ADDRESSES = "contract_addresses";
    private static final String PARAM_FROM_DATE = "from_date";
    private static final String PARAM_ORDER = "order";
    private static final String ORDER_DESC = "DESC";

    private final HttpCallTemplate moralisCallTemplate;
    private final PaymentWatchProperties properties;

    @Override
    public List<MoralisTokenTransfer> incomingUsdtTransfers(Instant since) {
        String path = PATH_ERC20_TRANSFERS.formatted(properties.targetAddress());

        MoralisTokenTransfersEnvelope envelope = moralisCallTemplate.get(path, builder -> builder
                .queryParam(PARAM_CHAIN, chainHex())
                .queryParam(PARAM_CONTRACT_ADDRESSES, properties.usdtContractAddress())
                .queryParam(PARAM_FROM_DATE, since.toString())
                .queryParam(PARAM_ORDER, ORDER_DESC), MoralisTokenTransfersEnvelope.class);

        return Optional.ofNullable(moralisCallTemplate.require(envelope, path).result()).orElseGet(List::of);
    }

    private String chainHex() {
        return "0x" + Integer.toHexString(properties.chain().evmChainId()
                .orElseThrow(() -> new PaymentWatchException(
                        properties.chain().displayName() + " has no EVM chain id, Moralis cannot be queried for it")));
    }
}
