package com.fyld.wxxcx.address;

import java.time.LocalDateTime;

public record UserAddress(
        long id,
        String province,
        String city,
        String community,
        String building,
        String unit,
        String roomNo,
        LocalDateTime createdAt) {

    /** 汇总门牌，例如 7-1-902，投票记录里用它标识投票人所在房屋 */
    public String line() {
        return building + "-" + unit + "-" + roomNo;
    }
}
