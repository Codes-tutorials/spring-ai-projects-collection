package com.example.ordercancel.config;

import io.netty.util.HashedWheelTimer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;

@Configuration
public class NettyConfig {

    private HashedWheelTimer timer;

    @Bean
    public HashedWheelTimer hashedWheelTimer() {
        // Create a hashed wheel timer with 100ms ticks and 512 size wheel
        this.timer = new HashedWheelTimer(100, TimeUnit.MILLISECONDS, 512);
        return this.timer;
    }

    @PreDestroy
    public void onDestroy() {
        if (this.timer != null) {
            this.timer.stop();
        }
    }
}
