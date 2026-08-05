package com.riskscoring.chainingest.exception;

import java.util.List;

public interface UserFacingChainFailure {

    String progressMessageKey();

    List<Object> progressMessageArgs();
}
