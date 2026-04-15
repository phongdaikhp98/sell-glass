package com.sellglass;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class SellGlassApplication {

    public static void main(String[] args) {
        SpringApplication.run(SellGlassApplication.class, args);
    }
}
