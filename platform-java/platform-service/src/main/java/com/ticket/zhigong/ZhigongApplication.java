package com.ticket.zhigong;

import com.ticket.zhigong.config.AiServiceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AiServiceProperties.class)
public class ZhigongApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZhigongApplication.class, args);
    }
}
