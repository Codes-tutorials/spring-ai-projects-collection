package com.example.ordercancel.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    public static final String DELAY_EXCHANGE = "order.delay.exchange";
    public static final String DELAY_QUEUE = "order.delay.queue";
    public static final String DELAY_ROUTING_KEY = "order.delay.key";

    public static final String DLX_EXCHANGE = "order.dlx.exchange";
    public static final String DLX_QUEUE = "order.dlx.queue";
    public static final String DLX_ROUTING_KEY = "order.dlx.key";

    // 1. DLX Exchange
    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(DLX_EXCHANGE);
    }

    // 2. DLX Queue (The actual queue where expired messages end up)
    @Bean
    public Queue dlxQueue() {
        return new Queue(DLX_QUEUE, true);
    }

    // 3. Bind DLX Queue to DLX Exchange
    @Bean
    public Binding dlxBinding() {
        return BindingBuilder.bind(dlxQueue()).to(dlxExchange()).with(DLX_ROUTING_KEY);
    }

    // 4. Delay Exchange
    @Bean
    public DirectExchange delayExchange() {
        return new DirectExchange(DELAY_EXCHANGE);
    }

    // 5. Delay Queue (Messages wait here until they expire)
    @Bean
    public Queue delayQueue() {
        Map<String, Object> arguments = new HashMap<>();
        // Set DLX to route expired messages
        arguments.put("x-dead-letter-exchange", DLX_EXCHANGE);
        arguments.put("x-dead-letter-routing-key", DLX_ROUTING_KEY);
        
        return new Queue(DELAY_QUEUE, true, false, false, arguments);
    }

    // 6. Bind Delay Queue to Delay Exchange
    @Bean
    public Binding delayBinding() {
        return BindingBuilder.bind(delayQueue()).to(delayExchange()).with(DELAY_ROUTING_KEY);
    }
}
