package by.innowise.orderservice.event;

import java.time.Instant;

public record PaymentEvent(
        PaymentEventType type,
        String paymentId,
        Long orderId,
        Long userId,
        PaymentStatus status,
        Instant timestamp
) {
}