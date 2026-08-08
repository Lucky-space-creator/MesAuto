package com.lucky.mescore.common.shiro;

import com.lucky.mescore.common.util.JwtUtil;
import com.lucky.mescore.modules.system.entity.Permission;
import com.lucky.mescore.modules.system.entity.User;
import com.lucky.mescore.modules.system.service.UserService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class MesRealm extends AuthorizingRealm {

    private final JwtUtil jwtUtil;
    private final UserService userService;

    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof JwtToken;
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        String token = (String) principals.getPrimaryPrincipal();
        Long userId = jwtUtil.getUserId(token);
        if (userId == null) {
            return new SimpleAuthorizationInfo();
        }
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
        Set<String> roleCodes = userService.getUserRoles(userId);
        Set<String> permCodes = userService.getUserPermissions(userId);
        info.setRoles(roleCodes);
        info.setStringPermissions(permCodes);
        return info;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authToken) throws AuthenticationException {
        String token = (String) authToken.getCredentials();
        Long userId = jwtUtil.getUserId(token);
        if (userId == null) {
            throw new ExpiredCredentialsException("Token已过期");
        }
        User user = userService.getById(userId);
        if (user == null || user.getStatus() == 0) {
            throw new DisabledAccountException("用户已被禁用");
        }
        return new SimpleAuthenticationInfo(token, token, getName());
    }
}
