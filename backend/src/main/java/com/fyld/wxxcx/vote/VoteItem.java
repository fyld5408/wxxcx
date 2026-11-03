package com.fyld.wxxcx.vote;

import java.time.LocalDateTime;

public record VoteItem(
        String address,
        String choice,
        LocalDateTime votedAt) {
}
