package by.innowise.orderservice.dto.orderitem;

import java.math.BigDecimal;

public record OrderItemResponseDto(
    Long id,
    Long itemId,
    String itemName,
    BigDecimal itemPrice,
    Integer quantity,
    BigDecimal totalPrice
) {
}
