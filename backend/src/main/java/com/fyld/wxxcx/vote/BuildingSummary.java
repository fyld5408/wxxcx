package com.fyld.wxxcx.vote;

/** 楼栋投票汇总：一栋一单元一行 */
public record BuildingSummary(
        String building,
        String unit,
        long supportCount,
        long opposeCount) {
}
