package com.vergiliusproject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.domingosuarez.boot.autoconfigure.pug4j;com.vergiliusproject")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}