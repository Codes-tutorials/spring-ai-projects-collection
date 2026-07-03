package com.example.ordercancel.model;

public enum CancellationApproach {
    DB_POLLING,
    DELAY_QUEUE,
    TIME_WHEEL,
    REDIS_ZSET,
    REDIS_EXPIRED,
    RABBITMQ_DLX
}
