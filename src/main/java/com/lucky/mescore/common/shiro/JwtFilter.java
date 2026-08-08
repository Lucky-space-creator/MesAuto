package com.lucky.mescore.common.shiro;

import com.lucky.mescore.common.util.JwtUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter implements Filter {

    private final JwtUtil jwtUtil;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String bearer = httpRequest.getHeader("Authorization");
        String token = (bearer != null && bearer.startsWith("Bearer ")) ? bearer.substring(7) : null;

        if (token != null && !jwtUtil.isTokenExpired(token)) {
            try {
                Subject subject = SecurityUtils.getSubject();
                if (!subject.isAuthenticated()) {
                    subject.login(new JwtToken(token));
                }
            } catch (Exception e) {
                log.debug("JWT登录失败: {}", e.getMessage());
            }
        }
        chain.doFilter(request, response);
    }
}
