package com.riskscoring.gateway.model;

import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public final class TonTargets {

    private static final Pattern RAW_ADDRESS = Pattern.compile("^(-1|0):[a-fA-F0-9]{64}$");
    private static final Pattern HEX_HASH = Pattern.compile("^[a-fA-F0-9]{64}$");
    private static final Pattern BASE64 = Pattern.compile("^[A-Za-z0-9_\\-+/]+={0,2}$");

    private static final int FRIENDLY_ADDRESS_LENGTH = 36;
    private static final int HASH_LENGTH = 32;
    private static final int WORKCHAIN_OFFSET = 1;
    private static final int HASH_OFFSET = 2;
    private static final int CHECKSUM_OFFSET = 34;

    private static final int CRC_POLYNOMIAL = 0x1021;
    private static final int CRC_HIGH_BIT = 0x8000;
    private static final int CRC_MASK = 0xFFFF;
    private static final int BYTE_MASK = 0xFF;
    private static final int BYTE_BITS = 8;

    private TonTargets() {
    }

    public static String normalize(String value) {
        String trimmed = value == null ? "" : value.trim();

        if (RAW_ADDRESS.matcher(trimmed).matches() || HEX_HASH.matcher(trimmed).matches()) {
            return trimmed.toLowerCase(Locale.ROOT);
        }

        return decode(trimmed).flatMap(TonTargets::canonical).orElse(trimmed);
    }

    private static Optional<byte[]> decode(String value) {
        if (!BASE64.matcher(value).matches()) {
            return Optional.empty();
        }

        try {
            return Optional.of(Base64.getUrlDecoder().decode(value.replace('+', '-').replace('/', '_')));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static Optional<String> canonical(byte[] decoded) {
        return switch (decoded.length) {
            case FRIENDLY_ADDRESS_LENGTH -> rawAddress(decoded);
            case HASH_LENGTH -> Optional.of(HexFormat.of().formatHex(decoded));
            default -> Optional.empty();
        };
    }

    private static Optional<String> rawAddress(byte[] decoded) {
        if (crc16(decoded, CHECKSUM_OFFSET) != checksum(decoded)) {
            return Optional.empty();
        }

        return Optional.of("%d:%s".formatted(
                decoded[WORKCHAIN_OFFSET], HexFormat.of().formatHex(decoded, HASH_OFFSET, CHECKSUM_OFFSET)));
    }

    private static int checksum(byte[] decoded) {
        return ((decoded[CHECKSUM_OFFSET] & BYTE_MASK) << BYTE_BITS) | (decoded[CHECKSUM_OFFSET + 1] & BYTE_MASK);
    }

    private static int crc16(byte[] data, int length) {
        int crc = 0;

        for (int index = 0; index < length; index++) {
            crc ^= (data[index] & BYTE_MASK) << BYTE_BITS;

            for (int bit = 0; bit < BYTE_BITS; bit++) {
                crc = (crc & CRC_HIGH_BIT) != 0
                        ? ((crc << 1) ^ CRC_POLYNOMIAL) & CRC_MASK
                        : (crc << 1) & CRC_MASK;
            }
        }

        return crc;
    }
}
