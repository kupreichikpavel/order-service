package by.innowise.orderservice.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrderStatusUpdateDto(

    @NotBlank(message = "Status must not be blank")
    @Size(
        max = 50,
        message = "Status must not exceed 50 characters"
    )
    String status
) {
}
