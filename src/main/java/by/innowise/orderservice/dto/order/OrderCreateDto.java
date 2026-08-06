package by.innowise.orderservice.dto.order;

import by.innowise.orderservice.dto.orderitem.OrderItemRequestDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record OrderCreateDto(

    @NotEmpty(message = "Order must contain at least one item")
    List<@Valid OrderItemRequestDto> items
) {

}
