package com.ticket.zhigong;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ZhigongApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZhigongApplication.class, args);
    }
}
