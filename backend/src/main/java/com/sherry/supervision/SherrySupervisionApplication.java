package com.sherry.supervision;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.sherry.supervision.mapper")
@SpringBootApplication
public class SherrySupervisionApplication {

    public static void main(String[] args) {
        SpringApplication.run(SherrySupervisionApplication.class, args);
    }
}
