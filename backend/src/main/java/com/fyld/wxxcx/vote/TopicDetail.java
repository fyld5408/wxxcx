package com.fyld.wxxcx.vote;

import java.time.LocalDateTime;
import java.util.List;

public record TopicDetail(
        long id,
        String title,
        long supportCount,
        long opposeCount,
        String myChoice,
        LocalDateTime createdAt,
        List<VoteItem> votes,
        List<BuildingSummary> buildingSummary) {
}
