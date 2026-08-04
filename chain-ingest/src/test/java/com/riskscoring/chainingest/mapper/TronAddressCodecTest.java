package com.riskscoring.chainingest.mapper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TronAddressCodecTest {

    private final TronAddressCodec codec = new TronAddressCodec();

    @Test
    void encodesMainnetPrefixedHex() {
        assertEquals("TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t",
                codec.toBase58("41a614f803b6fd780986a42c78ec9c7f77e6ded13c"));
    }

    @Test
    void addsMainnetPrefixToBareBody() {
        assertEquals("TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t",
                codec.toBase58("a614f803b6fd780986a42c78ec9c7f77e6ded13c"));
    }

    @Test
    void acceptsHexPrefixAndUpperCase() {
        assertEquals("TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t",
                codec.toBase58("0xA614F803B6FD780986A42C78EC9C7F77E6DED13C"));
    }

    @Test
    void encodesZeroBody() {
        assertEquals("T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb",
                codec.toBase58("410000000000000000000000000000000000000000"));
    }

    @Test
    void rejectsUnusableInput() {
        assertEquals("", codec.toBase58(null));
        assertEquals("", codec.toBase58(""));
        assertEquals("", codec.toBase58("a614f8"));
        assertEquals("", codec.toBase58("42a614f803b6fd780986a42c78ec9c7f77e6ded13c"));
    }
}
