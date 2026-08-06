package by.innowise.orderservice.dto.order;

import by.innowise.orderservice.dto.orderitem.OrderItemRequestDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record OrderCreateDto(

    @NotNull(message = "User id must not be null")
    @Positive(message = "User id must be positive")
    Long userId,

    @NotEmpty(message = "Order must contain at least one item")
    List<@Valid OrderItemRequestDto> items
) {
}
