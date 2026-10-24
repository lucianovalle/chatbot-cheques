package com.estudo.cheques;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ChequesApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChequesApplication.class, args);
    }
}
