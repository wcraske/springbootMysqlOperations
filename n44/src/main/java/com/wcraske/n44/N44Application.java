package com.wcraske.n44;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class N44Application {
    public static void main(String[] args) {
        SpringApplication.run(N44Application.class, args);
    }
}