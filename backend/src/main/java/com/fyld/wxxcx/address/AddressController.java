package com.fyld.wxxcx.address;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/address")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    /** 未登记返回 204，前端据此决定是否进注册页 */
    @GetMapping
    public ResponseEntity<UserAddress> get(@RequestHeader(value = "X-WX-OPENID", required = false) String openid) {
        UserAddress address = addressService.findOne(requireOpenid(openid));
        return address == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(address);
    }

    @PostMapping
    public UserAddress create(@RequestHeader(value = "X-WX-OPENID", required = false) String openid,
                              @RequestBody AddressInput input) {
        String currentUser = requireOpenid(openid);
        try {
            return addressService.create(currentUser, normalize(input));
        } catch (DuplicateKeyException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "已经登记过了");
        }
    }

    private AddressInput normalize(AddressInput input) {
        return new AddressInput(
                required(input.province(), "省", 32),
                required(input.city(), "市", 32),
                required(input.community(), "小区名称", 64),
                required(input.building(), "栋", 32),
                required(input.unit(), "单元", 32),
                required(input.roomNo(), "房号", 32));
    }

    private String required(String value, String label, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " 不能为空");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " 不能超过 " + maxLength + " 个字符");
        }
        return trimmed;
    }

    private String requireOpenid(String openid) {
        if (openid == null || openid.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "缺少 X-WX-OPENID，请通过云托管 callContainer 调用");
        }
        return openid;
    }
}
