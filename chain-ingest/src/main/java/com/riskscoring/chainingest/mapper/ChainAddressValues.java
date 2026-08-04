package com.riskscoring.chainingest.mapper;

public interface ChainAddressValues {

    String address(String value);

    boolean isRoutable(String address);
}
