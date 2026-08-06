package by.innowise.orderservice.dto.order;

import by.innowise.orderservice.dto.orderitem.OrderItemResponseDto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponseDto(
    Long id,
    Long userId,
    String status,
    BigDecimal totalPrice,
    List<OrderItemResponseDto> items,
    Instant createdAt,
    Instant updatedAt
) {
}
