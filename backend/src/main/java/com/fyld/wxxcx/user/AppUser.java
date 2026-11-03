package com.fyld.wxxcx.user;

import java.time.LocalDateTime;

public record AppUser(
        long id,
        String openid,
        String nickname,
        String avatarUrl,
        LocalDateTime createdAt,
        LocalDateTime lastSeenAt) {
}
