package com.ticket.zhigong.service;

import com.ticket.zhigong.dto.LoginRequest;
import com.ticket.zhigong.dto.LoginResponse;
import com.ticket.zhigong.entity.User;
import com.ticket.zhigong.enums.AuditAction;
import com.ticket.zhigong.exception.BusinessException;
import com.ticket.zhigong.repository.UserRepository;
import com.ticket.zhigong.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuditService auditService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.auditService = auditService;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    log.warn("Login failed: user not found [username={}]", request.getUsername());
                    return new BusinessException("AUTH_FAILED", "用户名或密码错误", HttpStatus.UNAUTHORIZED);
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed: wrong password [username={}]", request.getUsername());
            throw new BusinessException("AUTH_FAILED", "用户名或密码错误", HttpStatus.UNAUTHORIZED);
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name(), user.getId());
        auditService.log(user.getId(), AuditAction.LOGIN_SUCCESS, "USER", user.getId(),
                "用户 " + user.getUsername() + " 登录成功");
        log.info("Login success [username={}, role={}]", user.getUsername(), user.getRole());
        return new LoginResponse(token, jwtUtil.getExpirationMs() / 1000, user.getId(), user.getName(), user.getRole());
    }
}
