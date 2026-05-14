package com.stonestorage.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.stonestorage")
public class StoneStorageApplication {

    public static void main(String[] args) {
        SpringApplication.run(StoneStorageApplication.class, args);
    }
}
