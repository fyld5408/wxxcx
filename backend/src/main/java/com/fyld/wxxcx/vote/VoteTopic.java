package com.fyld.wxxcx.vote;

import java.time.LocalDateTime;

public record VoteTopic(
        long id,
        String title,
        long supportCount,
        long opposeCount,
        String myChoice,
        LocalDateTime createdAt) {
}
