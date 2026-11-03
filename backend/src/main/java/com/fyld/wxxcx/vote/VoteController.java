package com.fyld.wxxcx.vote;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class VoteController {

    private final VoteService voteService;

    public VoteController(VoteService voteService) {
        this.voteService = voteService;
    }

    @PostMapping("/votes")
    public VoteResult vote(@RequestHeader(value = "X-WX-OPENID", required = false) String openid,
                           @RequestBody VoteInput input) {
        return voteService.vote(requireOpenid(openid), title(input.title()), choice(input.choice()));
    }

    /** 对已有主题投票：别人发起的主题，其他人从这里投 */
    @PostMapping("/topics/{id}/votes")
    public VoteResult voteOnTopic(@RequestHeader(value = "X-WX-OPENID", required = false) String openid,
                                  @PathVariable long id,
                                  @RequestBody Map<String, String> body) {
        return voteService.voteOnTopic(requireOpenid(openid), id, choice(body.get("choice")));
    }

    @GetMapping("/topics")
    public List<VoteTopic> topics(@RequestHeader(value = "X-WX-OPENID", required = false) String openid) {
        return voteService.listTopics(requireOpenid(openid));
    }

    @GetMapping("/topics/{id}")
    public TopicDetail topic(@RequestHeader(value = "X-WX-OPENID", required = false) String openid,
                             @PathVariable long id) {
        return voteService.getTopic(id, requireOpenid(openid));
    }

    private String title(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "投票主题不能为空");
        }
        String trimmed = value.trim();
        if (trimmed.length() > 64) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "投票主题不能超过 64 个字符");
        }
        return trimmed;
    }

    private String choice(String value) {
        if (!VoteService.SUPPORT.equals(value) && !VoteService.OPPOSE.equals(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择支持或不支持");
        }
        return value;
    }

    private String requireOpenid(String openid) {
        if (openid == null || openid.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "缺少 X-WX-OPENID，请通过云托管 callContainer 调用");
        }
        return openid;
    }
}
