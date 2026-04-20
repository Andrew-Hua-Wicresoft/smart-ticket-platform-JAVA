package com.ticket.zhigong.controller;

import com.ticket.zhigong.dto.LoginRequest;
import com.ticket.zhigong.dto.LoginResponse;
import com.ticket.zhigong.enums.UserRole;
import com.ticket.zhigong.service.AuthService;
import com.ticket.zhigong.service.RateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerContractTest extends ControllerContractTestSupport {

    @Mock
    private AuthService authService;

    @Mock
    private RateLimiterService rateLimiterService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(authController);
    }

    @Test
    void loginReturnsJwtPayloadAndUsesForwardedIp() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("customer1");
        request.setPassword("demo123");

        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new LoginResponse("jwt-token", 86400L, 1001L, "客户一", UserRole.CUSTOMER));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", "198.51.100.10")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.expiresIn").value(86400))
                .andExpect(jsonPath("$.userId").value(1001))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));

        verify(rateLimiterService).checkLoginRateLimit("198.51.100.10");
    }

    @Test
    void loginRejectsBlankUsername() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"","password":"demo123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("用户名不能为空"));
    }
}
