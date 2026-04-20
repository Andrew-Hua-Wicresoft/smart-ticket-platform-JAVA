package com.ticket.zhigong.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ticket.zhigong.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

abstract class ControllerContractTestSupport {

    protected final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();

    protected MockMvc buildMockMvc(Object controller) {
        validator.afterPropertiesSet();
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    protected void authenticate(Long userId, String username, String... roles) {
        var authentication = new UsernamePasswordAuthenticationToken(
                username,
                "N/A",
                AuthorityUtils.createAuthorityList(roles)
        );
        authentication.setDetails(userId);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }
}
