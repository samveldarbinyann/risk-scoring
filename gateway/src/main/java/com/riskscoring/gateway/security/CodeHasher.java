package com.riskscoring.gateway.security;

import com.riskscoring.gateway.config.GatewayProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class CodeHasher {

    private static final String ALGORITHM = "HmacSHA256";

    private final byte[] pepper;

    public CodeHasher(GatewayProperties gatewayProperties) {
        this.pepper = gatewayProperties.verification().codePepper().getBytes(StandardCharsets.UTF_8);
    }

    public String hash(String code) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(pepper, ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(code.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("%s not available".formatted(ALGORITHM), exception);
        }
    }

    public boolean matches(String code, String expectedHash) {
        return MessageDigest.isEqual(
                hash(code).getBytes(StandardCharsets.UTF_8),
                expectedHash.getBytes(StandardCharsets.UTF_8));
    }
}