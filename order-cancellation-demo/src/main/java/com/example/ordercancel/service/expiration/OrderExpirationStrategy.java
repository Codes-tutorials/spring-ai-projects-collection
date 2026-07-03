package com.example.ordercancel.service.expiration;

import com.example.ordercancel.model.CancellationApproach;
import com.example.ordercancel.model.Order;

public interface OrderExpirationStrategy {
    
    /**
     * Determines if this strategy supports the given cancellation approach.
     */
    boolean supports(CancellationApproach approach);

    /**
     * Schedules a task to cancel the order if it remains unpaid after the delay.
     * 
     * @param order the order to monitor
     * @param delaySeconds delay in seconds
     */
    void scheduleExpiration(Order order, int delaySeconds);
}
