package com.riskscoring.chainingest.mapper;

import com.riskscoring.chainingest.exception.ChainDataException;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;

@Component
public class TronAddressCodec {

    private static final String ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    private static final BigInteger BASE = BigInteger.valueOf(ALPHABET.length());
    private static final String SHA_256 = "SHA-256";
    private static final String HEX_PREFIX = "0x";
    private static final String MAINNET_PREFIX = "41";
    private static final int BODY_LENGTH = 40;
    private static final int PREFIXED_LENGTH = 42;
    private static final int CHECKSUM_LENGTH = 4;

    public String toBase58(String hex) {
        String normalized = normalize(hex);
        if (normalized.isEmpty()) {
            return "";
        }

        byte[] address = HexFormat.of().parseHex(normalized);
        byte[] checksum = Arrays.copyOf(sha256(sha256(address)), CHECKSUM_LENGTH);

        byte[] payload = Arrays.copyOf(address, address.length + CHECKSUM_LENGTH);
        System.arraycopy(checksum, 0, payload, address.length, CHECKSUM_LENGTH);

        return encode(payload);
    }

    private String normalize(String hex) {
        if (hex == null) {
            return "";
        }

        String trimmed = hex.trim().toLowerCase(Locale.ROOT);
        if (trimmed.startsWith(HEX_PREFIX)) {
            trimmed = trimmed.substring(HEX_PREFIX.length());
        }

        if (trimmed.length() == BODY_LENGTH) {
            return MAINNET_PREFIX + trimmed;
        }

        return trimmed.length() == PREFIXED_LENGTH && trimmed.startsWith(MAINNET_PREFIX) ? trimmed : "";
    }

    private String encode(byte[] payload) {
        StringBuilder encoded = new StringBuilder();

        BigInteger value = new BigInteger(1, payload);
        while (value.signum() > 0) {
            BigInteger[] divisionAndRemainder = value.divideAndRemainder(BASE);
            encoded.append(ALPHABET.charAt(divisionAndRemainder[1].intValue()));
            value = divisionAndRemainder[0];
        }

        for (byte leading : payload) {
            if (leading != 0) {
                break;
            }
            encoded.append(ALPHABET.charAt(0));
        }

        return encoded.reverse().toString();
    }

    private byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance(SHA_256).digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new ChainDataException("SHA-256 is unavailable, cannot encode a TRON address", e);
        }
    }
}
