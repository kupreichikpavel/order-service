package by.innowise.orderservice.dto.order;

import by.innowise.orderservice.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record OrderStatusUpdateDto(

        @NotNull(message = "Status must not be null")
        OrderStatus status

) {

}
