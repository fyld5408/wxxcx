package com.fyld.wxxcx.address;

public record AddressInput(
        String province,
        String city,
        String community,
        String building,
        String unit,
        String roomNo) {
}
