package com.riskscoring.gateway.model;

import java.util.UUID;

public final class ScanOwnership {

    private ScanOwnership() {
    }

    public static boolean isAccessible(UUID ownerId, UUID requesterId) {
        return ownerId == null || ownerId.equals(requesterId);
    }
}
