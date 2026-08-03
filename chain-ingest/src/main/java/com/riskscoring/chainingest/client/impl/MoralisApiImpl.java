package com.riskscoring.chainingest.client.impl;

import com.riskscoring.chainingest.client.HttpCallTemplate;
import com.riskscoring.chainingest.client.MoralisApi;
import com.riskscoring.chainingest.client.dto.MoralisActiveChain;
import com.riskscoring.chainingest.client.dto.MoralisBalance;
import com.riskscoring.chainingest.client.dto.MoralisHistoryEnvelope;
import com.riskscoring.chainingest.client.dto.MoralisTokenBalance;
import com.riskscoring.chainingest.client.dto.MoralisTokenBalancesEnvelope;
import com.riskscoring.chainingest.client.dto.MoralisTransaction;
import com.riskscoring.chainingest.client.dto.MoralisWalletChainsEnvelope;
import com.riskscoring.chainingest.config.ChainIngestProperties;
import com.riskscoring.chainingest.exception.ChainDataException;
import com.riskscoring.chainingest.exception.ChainDataNotFoundException;
import com.riskscoring.common.model.Chain;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class MoralisApiImpl implements MoralisApi {

    private static final String PARAM_CHAIN = "chain";
    private static final String PARAM_CHAINS = "chains";
    private static final String PARAM_ORDER = "order";
    private static final String PARAM_LIMIT = "limit";
    private static final String PARAM_INCLUDE_INTERNAL = "include_internal_transactions";
    private static final String PARAM_INCLUDE = "include";
    private static final String INCLUDE_INTERNAL_TRANSACTIONS = "internal_transactions";
    private static final String PARAM_EXCLUDE_SPAM = "exclude_spam";
    private static final String PARAM_EXCLUDE_NATIVE = "exclude_native";
    private static final String ORDER_DESC = "DESC";

    private static final String PATH_BALANCE = "/%s/balance";
    private static final String PATH_HISTORY = "/wallets/%s/history";
    private static final String PATH_CHAINS = "/wallets/%s/chains";
    private static final String PATH_TOKENS = "/wallets/%s/tokens";
    private static final String PATH_TRANSACTION = "/transaction/%s";

    private final HttpCallTemplate moralisCallTemplate;
    private final ChainIngestProperties properties;

    @Override
    public String balanceNative(String address, Chain chain) {
        String path = PATH_BALANCE.formatted(address);
        MoralisBalance balance = moralisCallTemplate.get(path, builder -> builder
                .queryParam(PARAM_CHAIN, chainHex(chain)), MoralisBalance.class);

        return moralisCallTemplate.require(balance.balance(), path);
    }

    @Override
    public MoralisTransaction transaction(String hash, Chain chain) {
        String path = PATH_TRANSACTION.formatted(hash);
        return moralisCallTemplate.get(path, builder -> builder
                .queryParam(PARAM_CHAIN, chainHex(chain))
                .queryParam(PARAM_INCLUDE, INCLUDE_INTERNAL_TRANSACTIONS), MoralisTransaction.class);
    }

    @Override
    public MoralisHistoryEnvelope walletHistory(String address, Chain chain) {
        String path = PATH_HISTORY.formatted(address);
        MoralisHistoryEnvelope envelope = moralisCallTemplate.get(path, builder -> builder
                .queryParam(PARAM_CHAIN, chainHex(chain))
                .queryParam(PARAM_ORDER, ORDER_DESC)
                .queryParam(PARAM_LIMIT, properties.moralis().pageSize())
                .queryParam(PARAM_INCLUDE_INTERNAL, true), MoralisHistoryEnvelope.class);

        moralisCallTemplate.require(envelope.result(), path);
        return envelope;
    }

    @Override
    public Optional<MoralisActiveChain> walletActivity(String address, Chain chain) {
        String path = PATH_CHAINS.formatted(address);

        try {
            List<MoralisActiveChain> activeChains = moralisCallTemplate.get(path, builder -> builder
                            .queryParam(PARAM_CHAINS, chainHex(chain)), MoralisWalletChainsEnvelope.class)
                    .activeChains();

            return activeChains == null ? Optional.empty() : activeChains.stream().findFirst();
        } catch (ChainDataNotFoundException e) {
            log.debug("No wallet-activity data for address={} chain={}: {}", address, chain, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<MoralisTokenBalance> tokenBalances(String address, Chain chain) {
        String path = PATH_TOKENS.formatted(address);
        MoralisTokenBalancesEnvelope envelope = moralisCallTemplate.get(path, builder -> builder
                .queryParam(PARAM_CHAIN, chainHex(chain))
                .queryParam(PARAM_EXCLUDE_SPAM, true)
                .queryParam(PARAM_EXCLUDE_NATIVE, true), MoralisTokenBalancesEnvelope.class);

        return moralisCallTemplate.require(envelope.result(), path);
    }

    private String chainHex(Chain chain) {
        return "0x" + Integer.toHexString(chain.evmChainId().orElseThrow(
                () -> new ChainDataException("%s has no EVM chain id, Moralis cannot be queried for it"
                        .formatted(chain.displayName()))));
    }
}
