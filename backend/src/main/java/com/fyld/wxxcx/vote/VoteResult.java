package com.fyld.wxxcx.vote;

public record VoteResult(
        long topicId,
        String title,
        String choice,
        String address) {
}
