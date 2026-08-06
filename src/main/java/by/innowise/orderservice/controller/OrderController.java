package by.innowise.orderservice.controller;

import by.innowise.orderservice.dto.order.OrderCreateDto;
import by.innowise.orderservice.dto.order.OrderResponseDto;
import by.innowise.orderservice.dto.order.OrderStatusUpdateDto;
import by.innowise.orderservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    description = "Operations for managing orders"
)
public class OrderController {

  private final OrderService orderService;

  @PostMapping
  @Operation(summary = "Create a new order")
  @ApiResponses({
      @ApiResponse(
          responseCode = "201",
          description = "Order successfully created"
      ),
      @ApiResponse(
          responseCode = "400",
          description = "Request validation failed"
      ),
      @ApiResponse(
          responseCode = "404",
          description = "One or more items were not found"
      )
  })
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
  @Operation(summary = "Get an order by id")
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "Order successfully returned"
      ),
      @ApiResponse(
          responseCode = "400",
          description = "Order id is invalid"
      ),
      @ApiResponse(
          responseCode = "404",
          description = "Order was not found"
      )
  })
  public ResponseEntity<OrderResponseDto> getById(
      @PathVariable
      @Positive(message = "Order id must be positive")
      Long id
  ) {
    OrderResponseDto response = orderService.getById(id);

    return ResponseEntity.ok(response);
  }

  @GetMapping
  @Operation(summary = "Get all active orders")
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "Orders successfully returned"
      ),
      @ApiResponse(
          responseCode = "400",
          description = "Request parameters are invalid"
      )
  })
  public ResponseEntity<Page<OrderResponseDto>> getAll(
      @RequestParam(required = false)
      @Positive(message = "User id must be positive")
      Long userId,

      @ParameterObject
      Pageable pageable
  ) {
    Page<OrderResponseDto> response;

    if (userId == null) {
      response = orderService.getAll(pageable);
    } else {
      response = orderService.getAllByUserId(
          userId,
          pageable
      );
    }

    return ResponseEntity.ok(response);
  }

  @PatchMapping("/{id}/status")
  @Operation(summary = "Update an order status")
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "Order status successfully updated"
      ),
      @ApiResponse(
          responseCode = "400",
          description = "Request validation failed"
      ),
      @ApiResponse(
          responseCode = "404",
          description = "Order was not found"
      )
  })
  public ResponseEntity<OrderResponseDto> updateStatus(
      @PathVariable
      @Positive(message = "Order id must be positive")
      Long id,

      @Valid @RequestBody OrderStatusUpdateDto request
  ) {
    OrderResponseDto response = orderService.updateStatus(
        id,
        request
    );

    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Soft delete an order")
  @ApiResponses({
      @ApiResponse(
          responseCode = "204",
          description = "Order successfully deleted"
      ),
      @ApiResponse(
          responseCode = "400",
          description = "Order id is invalid"
      ),
      @ApiResponse(
          responseCode = "404",
          description = "Order was not found"
      )
  })
  public ResponseEntity<Void> delete(
      @PathVariable
      @Positive(message = "Order id must be positive")
      Long id
  ) {
    orderService.delete(id);

    return ResponseEntity.noContent().build();
  }
}
