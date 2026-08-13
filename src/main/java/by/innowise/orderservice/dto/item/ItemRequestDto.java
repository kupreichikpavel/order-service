package by.innowise.orderservice.dto.item;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ItemRequestDto(

    @NotBlank(message = "Name must not be blank")
    @Size(
        max = 255,
        message = "Name must not exceed 255 characters"
    )
    String name,

    @NotNull(message = "Price must not be null")
    @PositiveOrZero(message = "Price must not be negative")
    BigDecimal price

) {

}
