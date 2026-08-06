package by.innowise.orderservice.controller;

import by.innowise.orderservice.dto.order.OrderCreateDto;
import by.innowise.orderservice.dto.order.OrderResponseDto;
import by.innowise.orderservice.dto.order.OrderStatusUpdateDto;
import by.innowise.orderservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
@Tag(
    name = "Orders",
    description = "Order management API"
)
public class OrderController {

  private final OrderService orderService;

  @PostMapping
  @Operation(
      summary = "Create a new order",
      responses = {
          @ApiResponse(
              responseCode = "201",
              description = "Order created"
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Request validation failed",
              content = @Content
          ),
          @ApiResponse(
              responseCode = "404",
              description = "One or more items were not found",
              content = @Content
          )
      }
  )
  public ResponseEntity<OrderResponseDto> create(
      @Valid @RequestBody OrderCreateDto request
  ) {
    OrderResponseDto response = orderService.create(request);

    URI location = ServletUriComponentsBuilder
        .fromCurrentRequest()
        .path("/{id}")
        .buildAndExpand(response.id())
        .toUri();

    return ResponseEntity
        .created(location)
        .body(response);
  }

  @GetMapping("/{id}")
  @Operation(
      summary = "Get an order by id",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Order found"
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Invalid order id",
              content = @Content
          ),
          @ApiResponse(
              responseCode = "404",
              description = "Order not found",
              content = @Content
          )
      }
  )
  public OrderResponseDto getById(
      @PathVariable
      @Positive(message = "Order id must be positive")
      Long id
  ) {
    return orderService.getById(id);
  }

  @GetMapping
  @Operation(
      summary = "Get all active orders",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Orders returned"
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Invalid request parameters",
              content = @Content
          )
      }
  )
  public Page<OrderResponseDto> getAll(
      @RequestParam(required = false)
      @Positive(message = "User id must be positive")
      Long userId,
      @ParameterObject Pageable pageable
  ) {
    if (userId == null) {
      return orderService.getAll(pageable);
    }

    return orderService.getAllByUserId(userId, pageable);
  }

  @PatchMapping("/{id}/status")
  @Operation(
      summary = "Update the status of an order",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Order status updated"
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Request validation failed",
              content = @Content
          ),
          @ApiResponse(
              responseCode = "404",
              description = "Order not found",
              content = @Content
          )
      }
  )
  public OrderResponseDto updateStatus(
      @PathVariable
      @Positive(message = "Order id must be positive")
      Long id,
      @Valid @RequestBody OrderStatusUpdateDto request
  ) {
    return orderService.updateStatus(id, request);
  }

  @DeleteMapping("/{id}")
  @Operation(
      summary = "Soft delete an order",
      responses = {
          @ApiResponse(
              responseCode = "204",
              description = "Order deleted",
              content = @Content
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Invalid order id",
              content = @Content
          ),
          @ApiResponse(
              responseCode = "404",
              description = "Order not found",
              content = @Content
          )
      }
  )
  public ResponseEntity<Void> delete(
      @PathVariable
      @Positive(message = "Order id must be positive")
      Long id
  ) {
    orderService.delete(id);

    return ResponseEntity.noContent().build();
  }
}
