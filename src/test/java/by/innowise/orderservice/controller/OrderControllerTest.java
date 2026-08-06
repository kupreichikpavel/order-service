package by.innowise.orderservice.controller;


import by.innowise.orderservice.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import by.innowise.orderservice.dto.order.OrderCreateDto;
import by.innowise.orderservice.dto.order.OrderResponseDto;
import by.innowise.orderservice.dto.order.OrderStatusUpdateDto;
import by.innowise.orderservice.exception.ResourceNotFoundException;
import by.innowise.orderservice.service.OrderService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderController.class)
@Import(GlobalExceptionHandler.class)
class OrderControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private OrderService orderService;

  @MockitoBean(name = "jpaMappingContext")
  private JpaMetamodelMappingContext jpaMappingContext;

  @Test
  void shouldCreateOrder() throws Exception {
    OrderResponseDto response = createResponse(
        1L,
        "CREATED"
    );

    when(orderService.create(any(OrderCreateDto.class)))
        .thenReturn(response);

    mockMvc.perform(post("/api/v1/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "userId": 7,
                  "items": [
                    {
                      "itemId": 1,
                      "quantity": 2
                    }
                  ]
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(header().string(
            "Location",
            "http://localhost/api/v1/orders/1"
        ))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.userId").value(7))
        .andExpect(jsonPath("$.status").value("CREATED"))
        .andExpect(jsonPath("$.totalPrice").value(100.00))
        .andExpect(jsonPath("$.items").isArray());

    verify(orderService).create(any(OrderCreateDto.class));
  }

  @Test
  void shouldRejectInvalidCreateRequest() throws Exception {
    mockMvc.perform(post("/api/v1/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "userId": 0,
                  "items": []
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title")
            .value("Validation failed"))
        .andExpect(jsonPath("$.detail")
            .value("Request validation failed"))
        .andExpect(jsonPath("$.errors.userId")
            .value("User id must be positive"))
        .andExpect(jsonPath("$.errors.items")
            .value("Order must contain at least one item"))
        .andExpect(jsonPath("$.path")
            .value("/api/v1/orders"));

    verifyNoInteractions(orderService);
  }

  @Test
  void shouldRejectInvalidNestedOrderItem() throws Exception {
    mockMvc.perform(post("/api/v1/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "userId": 7,
                  "items": [
                    {
                      "itemId": 0,
                      "quantity": 0
                    }
                  ]
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title")
            .value("Validation failed"))
        .andExpect(jsonPath("$.detail")
            .value("Request validation failed"));

    verifyNoInteractions(orderService);
  }

  @Test
  void shouldReturnOrderById() throws Exception {
    OrderResponseDto response = createResponse(
        1L,
        "CREATED"
    );

    when(orderService.getById(1L))
        .thenReturn(response);

    mockMvc.perform(get("/api/v1/orders/{id}", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.userId").value(7))
        .andExpect(jsonPath("$.status").value("CREATED"))
        .andExpect(jsonPath("$.totalPrice").value(100.00));

    verify(orderService).getById(1L);
  }

  @Test
  void shouldReturnNotFoundProblem() throws Exception {
    when(orderService.getById(99L))
        .thenThrow(new ResourceNotFoundException(
            "Order with id 99 was not found"
        ));

    mockMvc.perform(get("/api/v1/orders/{id}", 99L))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title")
            .value("Resource not found"))
        .andExpect(jsonPath("$.detail")
            .value("Order with id 99 was not found"))
        .andExpect(jsonPath("$.path")
            .value("/api/v1/orders/99"));
  }

  @Test
  void shouldUpdateOrderStatus() throws Exception {
    OrderResponseDto response = createResponse(
        1L,
        "PROCESSING"
    );

    when(orderService.updateStatus(
        eq(1L),
        any(OrderStatusUpdateDto.class)
    )).thenReturn(response);

    mockMvc.perform(patch("/api/v1/orders/{id}/status", 1L)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "status": "PROCESSING"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.status").value("PROCESSING"));

    verify(orderService).updateStatus(
        eq(1L),
        any(OrderStatusUpdateDto.class)
    );
  }

  @Test
  void shouldRejectBlankStatus() throws Exception {
    mockMvc.perform(patch("/api/v1/orders/{id}/status", 1L)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "status": " "
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title")
            .value("Validation failed"))
        .andExpect(jsonPath("$.detail")
            .value("Request validation failed"))
        .andExpect(jsonPath("$.errors.status")
            .value("Status must not be blank"));

    verifyNoInteractions(orderService);
  }

  @Test
  void shouldSoftDeleteOrder() throws Exception {
    mockMvc.perform(delete("/api/v1/orders/{id}", 1L))
        .andExpect(status().isNoContent());

    verify(orderService).delete(1L);
  }

  @Test
  void shouldRejectNonPositiveOrderId() throws Exception {
    mockMvc.perform(get("/api/v1/orders/{id}", 0L))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title")
            .value("Validation failed"))
        .andExpect(jsonPath("$.detail")
            .value("Request parameter validation failed"));

    verifyNoInteractions(orderService);
  }

  private OrderResponseDto createResponse(
      Long id,
      String status
  ) {
    return new OrderResponseDto(
        id,
        7L,
        status,
        new BigDecimal("100.00"),
        List.of(),
        null,
        null
    );
  }
}
