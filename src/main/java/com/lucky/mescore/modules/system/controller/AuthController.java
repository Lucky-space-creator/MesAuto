package com.lucky.mescore.modules.system.controller;

import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lucky.mescore.common.result.R;
import com.lucky.mescore.common.util.JwtUtil;
import com.lucky.mescore.common.util.RedisUtil;
import com.lucky.mescore.modules.system.dto.LoginDTO;
import com.lucky.mescore.modules.system.dto.LoginVO;
import com.lucky.mescore.modules.system.entity.User;
import com.lucky.mescore.modules.system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.SecurityUtils;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;

    @PostMapping("/login")
    public R<LoginVO> login(@RequestBody LoginDTO dto) {
        User user = userService.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        if (user == null || user.getStatus() == 0) {
            return R.fail(401, "用户名或密码错误");
        }
        String encrypted = SecureUtil.sha256(user.getId() + user.getSalt() + dto.getPassword());
        if (!encrypted.equals(user.getPassword())) {
            return R.fail(401, "用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getTenantId(), null);
        redisUtil.set("token:jwt:" + token, "1", 7200, TimeUnit.SECONDS);

        user.setLastLoginTime(java.time.LocalDateTime.now());
        userService.updateById(user);

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setPermissions(userService.getUserPermissions(user.getId()));
        return R.ok(vo);
    }

    @PostMapping("/logout")
    public R<Void> logout() {
        String token = (String) SecurityUtils.getSubject().getPrincipal();
        if (token != null) {
            redisUtil.delete("token:jwt:" + token);
        }
        SecurityUtils.getSubject().logout();
        return R.ok();
    }
}
