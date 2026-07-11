package com.huixiang;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HuixiangApplication {

    public static void main(String[] args) {
        SpringApplication.run(HuixiangApplication.class, args);
    }
}
