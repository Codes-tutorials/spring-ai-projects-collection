package com.example.ordercancel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class OrderCancellationApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderCancellationApplication.class, args);
    }
}
