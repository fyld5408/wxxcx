package com.fyld.wxxcx.user;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public AppUser me(@RequestHeader(value = "X-WX-OPENID", required = false) String openid) {
        return userService.touch(requireOpenid(openid));
    }

    @PatchMapping("/me")
    public AppUser updateMe(@RequestHeader(value = "X-WX-OPENID", required = false) String openid,
                            @RequestBody Map<String, String> body) {
        return userService.updateProfile(requireOpenid(openid), body.get("nickname"), body.get("avatarUrl"));
    }

    private String requireOpenid(String openid) {
        if (openid == null || openid.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "缺少 X-WX-OPENID，请通过云托管 callContainer 调用");
        }
        return openid;
    }
}
