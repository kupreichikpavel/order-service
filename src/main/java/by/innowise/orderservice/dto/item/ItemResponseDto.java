package by.innowise.orderservice.dto.item;

import java.math.BigDecimal;
import java.time.Instant;

public record ItemResponseDto(
    Long id,
    String name,
    BigDecimal price,
    Instant createdAt,
    Instant updatedAt
) {

}
