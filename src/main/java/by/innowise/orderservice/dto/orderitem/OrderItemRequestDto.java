package by.innowise.orderservice.dto.orderitem;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderItemRequestDto(

    @NotNull(message = "Item id must not be null")
    @Positive(message = "Item id must be positive")
    Long itemId,

    @NotNull(message = "Quantity must not be null")
    @Positive(message = "Quantity must be positive")
    Integer quantity
) {
}
