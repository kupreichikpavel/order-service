package by.innowise.orderservice.controller;


import by.innowise.orderservice.config.SecurityConfig;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import by.innowise.orderservice.dto.order.OrderCreateDto;
import by.innowise.orderservice.dto.order.OrderResponseDto;
import by.innowise.orderservice.dto.order.OrderStatusUpdateDto;
import by.innowise.orderservice.entity.OrderStatus;
import by.innowise.orderservice.exception.ResourceNotFoundException;
import by.innowise.orderservice.service.OrderService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(OrderController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class OrderControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private OrderService orderService;

  @MockitoBean
  private JwtDecoder jwtDecoder;

  @MockitoBean(name = "jpaMappingContext")
  private JpaMetamodelMappingContext jpaMappingContext;

  @Test
  void shouldCreateOrder() throws Exception {
    OrderResponseDto response = createResponse(1L, OrderStatus.CREATED);

    when(orderService.create(eq(7L), any(OrderCreateDto.class))).thenReturn(response);

    perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content("""
        {
          "items": [
            {
              "itemId": 1,
              "quantity": 2
            }
          ]
        }
        """)).andExpect(status().isCreated())
        .andExpect(header().string("Location", "http://localhost/api/v1/orders/1"))
        .andExpect(jsonPath("$.id").value(1)).andExpect(jsonPath("$.userId").value(7))
        .andExpect(jsonPath("$.status").value("CREATED"))
        .andExpect(jsonPath("$.totalPrice").value(100.00)).andExpect(jsonPath("$.items").isArray());

    verify(orderService).create(eq(7L), any(OrderCreateDto.class));
  }

  @Test
  void shouldRejectInvalidCreateRequest() throws Exception {
    perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content("""
        {
          "items": []
        }
        """)).andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Validation failed"))
        .andExpect(jsonPath("$.detail").value("Request validation failed"))
        .andExpect(jsonPath("$.errors.items").value("Order must contain at least one item"))
        .andExpect(jsonPath("$.path").value("/api/v1/orders"));

    verifyNoInteractions(orderService);
  }

  @Test
  void shouldRejectInvalidNestedOrderItem() throws Exception {
    perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content("""
        {
          "items": [
            {
              "itemId": 0,
              "quantity": 0
            }
          ]
        }
        """)).andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Validation failed"))
        .andExpect(jsonPath("$.detail").value("Request validation failed"));

    verifyNoInteractions(orderService);
  }

  @Test
  void shouldReturnOrderById() throws Exception {
    OrderResponseDto response = createResponse(1L, OrderStatus.CREATED);

    when(orderService.getById(1L, 7L)).thenReturn(response);

    perform(get("/api/v1/orders/{id}", 1L)).andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1)).andExpect(jsonPath("$.userId").value(7))
        .andExpect(jsonPath("$.status").value("CREATED"))
        .andExpect(jsonPath("$.totalPrice").value(100.00));

    verify(orderService).getById(1L, 7L);
  }

  @Test
  void shouldReturnNotFoundProblem() throws Exception {
    when(orderService.getById(99L, 7L)).thenThrow(
        new ResourceNotFoundException("Order with id 99 was not found"));

    perform(get("/api/v1/orders/{id}", 99L)).andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Resource not found"))
        .andExpect(jsonPath("$.detail").value("Order with id 99 was not found"))
        .andExpect(jsonPath("$.path").value("/api/v1/orders/99"));
  }

  @Test
  void shouldReturnCurrentUserOrders() throws Exception {
    OrderResponseDto order = createResponse(1L, OrderStatus.CREATED);

    Page<OrderResponseDto> response =
        new PageImpl<>(List.of(order));

    when(orderService.getAllByUserId(
        eq(7L),
        any(Pageable.class)
    )).thenReturn(response);

    perform(get("/api/v1/orders"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[0].id").value(1))
        .andExpect(jsonPath("$.content[0].userId").value(7))
        .andExpect(
            jsonPath("$.content[0].status")
                .value("CREATED")
        );

    verify(orderService).getAllByUserId(
        eq(7L),
        any(Pageable.class)
    );
  }

  @Test
  void shouldUpdateOrderStatus() throws Exception {
    OrderResponseDto response = createResponse(1L, OrderStatus.PROCESSING);

    when(orderService.updateStatus(
        eq(1L),
        eq(7L),
        any(OrderStatusUpdateDto.class))).thenReturn(response);

    perform(
        patch("/api/v1/orders/{id}/status", 1L).contentType(MediaType.APPLICATION_JSON).content("""
            {
              "status": "PROCESSING"
            }
            """)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.status").value("PROCESSING"));

    verify(orderService).updateStatus(
        eq(1L),
        eq(7L),
        any(OrderStatusUpdateDto.class));
  }

  @Test
  void shouldRejectNullStatus() throws Exception {
    perform(
        patch("/api/v1/orders/{id}/status", 1L)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "status": null
                }
                """)
    )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Validation failed"))
        .andExpect(jsonPath("$.detail").value("Request validation failed"))
        .andExpect(
            jsonPath("$.errors.status")
                .value("Status must not be null")
        );

    verifyNoInteractions(orderService);
  }

  @Test
  void shouldRejectUnknownStatus() throws Exception {
    perform(
        patch("/api/v1/orders/{id}/status", 1L)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "status": "UNKNOWN"
                }
                """)
    )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Malformed request"))
        .andExpect(
            jsonPath("$.detail")
                .value("Request body is malformed")
        );

    verifyNoInteractions(orderService);
  }

  @Test
  void shouldSoftDeleteOrder() throws Exception {
    perform(delete("/api/v1/orders/{id}", 1L)).andExpect(status().isNoContent());

    verify(orderService).delete(1L, 7L);
  }

  @Test
  void shouldRejectNonPositiveOrderId() throws Exception {
    perform(get("/api/v1/orders/{id}", 0L)).andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Validation failed"))
        .andExpect(jsonPath("$.detail").value("Request parameter validation failed"));

    verifyNoInteractions(orderService);
  }

  @Test
  void shouldReturnUnauthorizedWithoutToken() throws Exception {
    mockMvc.perform(get("/api/v1/orders/{id}", 1L)).andExpect(status().isUnauthorized());

    verifyNoInteractions(orderService);
  }

  private ResultActions perform(MockHttpServletRequestBuilder requestBuilder) throws Exception {
    return mockMvc.perform(requestBuilder.with(jwt().jwt(
            token -> token.claim("userId", "7").claim("realm_access", Map.of("roles", List.of("USER"))))
        .authorities(new SimpleGrantedAuthority("ROLE_USER"))));
  }

  private OrderResponseDto createResponse(Long id, OrderStatus status) {
    return new OrderResponseDto(id, 7L, status, new BigDecimal("100.00"), List.of(), null, null);
  }
}
