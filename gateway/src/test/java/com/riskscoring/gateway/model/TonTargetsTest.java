package com.riskscoring.gateway.model;

import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.ScanTarget;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TonTargetsTest {

    private static final String BOUNCEABLE = "EQCD39VS5jcptHL8vMjEXrzGaRcCVYto7HUn4bpAOg8xqB2N";
    private static final String NON_BOUNCEABLE = "UQCD39VS5jcptHL8vMjEXrzGaRcCVYto7HUn4bpAOg8xqEBI";
    private static final String RAW = "0:83dfd552e63729b472fcbcc8c45ebcc6691702558b68ec7527e1ba403a0f31a8";

    private static final String HASH_HEX = "c1ea1b076620e049a8973180be79089c561aabfc292c732fa67cc25bedad7051";
    private static final String HASH_BASE64 = "weobB2Yg4EmolzGAvnkInFYaq_wpLHMvpnzCW-2tcFE=";

    private final ChainTargetFormat format = ChainTargetFormat.of(Chain.TON.family());

    @Test
    void collapsesBothUserFriendlyFormsToTheSameRawAddress() {
        assertEquals(RAW, TonTargets.normalize(BOUNCEABLE));
        assertEquals(RAW, TonTargets.normalize(NON_BOUNCEABLE));
    }

    @Test
    void keepsRawAddressLowercasedAndTrimmed() {
        assertEquals(RAW, TonTargets.normalize("  0:83DFD552E63729B472FCBCC8C45EBCC6691702558B68EC7527E1BA403A0F31A8  "));
    }

    @Test
    void keepsMasterchainWorkchain() {
        String masterchain = "-1" + RAW.substring(1);

        assertEquals(masterchain, TonTargets.normalize(masterchain));
    }

    @Test
    void acceptsStandardBase64Alphabet() {
        assertEquals(RAW, TonTargets.normalize(BOUNCEABLE.replace('-', '+').replace('_', '/')));
    }

    @Test
    void rejectsBrokenChecksum() {
        String corrupted = BOUNCEABLE.substring(0, BOUNCEABLE.length() - 2) + "AA";

        assertEquals(corrupted, TonTargets.normalize(corrupted));
    }

    @Test
    void normalizesTransactionHashToHex() {
        assertEquals(HASH_HEX, TonTargets.normalize(HASH_BASE64));
        assertEquals(HASH_HEX, TonTargets.normalize(HASH_HEX.toUpperCase()));
    }

    @Test
    void classifiesNormalizedTargets() {
        assertEquals(Optional.of(ScanTarget.ADDRESS), format.classify(NON_BOUNCEABLE));
        assertEquals(Optional.of(ScanTarget.TRANSACTION), format.classify(HASH_BASE64));
    }

    @Test
    void leavesForeignFormatsUnmatched() {
        assertEquals(Optional.empty(), format.classify("0x8ba1f109551bd432803012645ac136ddd64dba72"));
        assertEquals(Optional.empty(), format.classify("TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t"));
        assertEquals(Optional.empty(), format.classify("bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq"));
    }
}
