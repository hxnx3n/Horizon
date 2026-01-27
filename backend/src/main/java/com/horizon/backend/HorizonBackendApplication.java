package com.horizon.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HorizonBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(HorizonBackendApplication.class, args);
    }

}
