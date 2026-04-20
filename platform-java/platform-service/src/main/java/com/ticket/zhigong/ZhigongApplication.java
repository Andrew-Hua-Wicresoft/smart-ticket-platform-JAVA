package com.ticket.zhigong;

import com.ticket.zhigong.config.LlmProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties(LlmProperties.class)
public class ZhigongApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZhigongApplication.class, args);
    }
}
