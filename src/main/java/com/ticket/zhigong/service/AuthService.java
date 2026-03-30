package com.ticket.zhigong.service;

import com.ticket.zhigong.dto.LoginRequest;
import com.ticket.zhigong.dto.LoginResponse;
import com.ticket.zhigong.entity.User;
import com.ticket.zhigong.exception.BusinessException;
import com.ticket.zhigong.repository.UserRepository;
import com.ticket.zhigong.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException("AUTH_FAILED", "用户名或密码错误", HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("AUTH_FAILED", "用户名或密码错误", HttpStatus.UNAUTHORIZED);
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name(), user.getId());
        return new LoginResponse(token, jwtUtil.getExpirationMs() / 1000, user.getId(), user.getName(), user.getRole());
    }
}
