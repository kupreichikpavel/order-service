package by.innowise.orderservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import by.innowise.orderservice.dto.order.OrderCreateDto;
import by.innowise.orderservice.dto.order.OrderResponseDto;
import by.innowise.orderservice.dto.order.OrderStatusUpdateDto;
import by.innowise.orderservice.dto.orderitem.OrderItemRequestDto;
import by.innowise.orderservice.entity.Item;
import by.innowise.orderservice.entity.Order;
import by.innowise.orderservice.entity.OrderStatus;
import by.innowise.orderservice.exception.ResourceNotFoundException;
import by.innowise.orderservice.mapper.OrderMapper;
import by.innowise.orderservice.repository.ItemRepository;
import by.innowise.orderservice.repository.OrderRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

  @Mock
  private OrderRepository orderRepository;

  @Mock
  private ItemRepository itemRepository;

  @Mock
  private OrderMapper orderMapper;

  @InjectMocks
  private OrderServiceImpl orderService;

  @Test
  void shouldCreateOrderAndCalculateTotalPrice() {
    OrderCreateDto request = new OrderCreateDto(
        List.of(new OrderItemRequestDto(1L, 2), new OrderItemRequestDto(2L, 3)));

    Item laptop = createItem(1L, "Laptop", "1500.00");

    Item mouse = createItem(2L, "Mouse", "45.50");

    OrderResponseDto expectedResponse = new OrderResponseDto(1L, 7L, OrderStatus.CREATED,
        new BigDecimal("3136.50"), List.of(), null, null);

    when(itemRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(laptop, mouse));

    when(orderRepository.save(any(Order.class))).thenAnswer(
        invocation -> invocation.getArgument(0));

    when(orderMapper.toResponseDto(any(Order.class))).thenReturn(expectedResponse);

    OrderResponseDto actualResponse = orderService.create(7L, request);

    assertThat(actualResponse).isSameAs(expectedResponse);

    ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);

    verify(orderRepository).save(orderCaptor.capture());

    Order savedOrder = orderCaptor.getValue();

    assertThat(savedOrder.getUserId()).isEqualTo(7L);
    assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.CREATED);
    assertThat(savedOrder.getTotalPrice()).isEqualByComparingTo("3136.50");
    assertThat(savedOrder.isDeleted()).isFalse();
    assertThat(savedOrder.getOrderItems()).hasSize(2);

    assertThat(savedOrder.getOrderItems().getFirst().getOrder()).isSameAs(savedOrder);
    assertThat(savedOrder.getOrderItems().getFirst().getItem()).isSameAs(laptop);
    assertThat(savedOrder.getOrderItems().getFirst().getQuantity()).isEqualTo(2);

    assertThat(savedOrder.getOrderItems().get(1).getOrder()).isSameAs(savedOrder);
    assertThat(savedOrder.getOrderItems().get(1).getItem()).isSameAs(mouse);
    assertThat(savedOrder.getOrderItems().get(1).getQuantity()).isEqualTo(3);
  }

  @Test
  void shouldThrowExceptionWhenItemDoesNotExist() {
    OrderCreateDto request = new OrderCreateDto(
        List.of(new OrderItemRequestDto(1L, 2), new OrderItemRequestDto(2L, 1)));

    Item existingItem = createItem(1L, "Laptop", "1500.00");

    when(itemRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(existingItem));

    assertThatThrownBy(() -> orderService.create(7L, request)).isInstanceOf(
        ResourceNotFoundException.class).hasMessage("Items were not found: [2]");

    verify(orderRepository, never()).save(any(Order.class));
    verify(orderMapper, never()).toResponseDto(any(Order.class));
  }

  @Test
  void shouldReturnActiveOrderById() {
    Order order = createOrder();

    OrderResponseDto expectedResponse = new OrderResponseDto(1L, 7L, OrderStatus.CREATED,
        new BigDecimal("100.00"), List.of(), null, null);

    when(orderRepository.findByIdAndUserIdAndDeletedFalse(1L, 7L)).thenReturn(Optional.of(order));

    when(orderMapper.toResponseDto(order)).thenReturn(expectedResponse);

    OrderResponseDto actualResponse = orderService.getById(1L, 7L);

    assertThat(actualResponse).isSameAs(expectedResponse);
  }

  @Test
  void shouldThrowExceptionWhenOrderDoesNotExist() {
    when(orderRepository.findByIdAndUserIdAndDeletedFalse(99L, 7L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> orderService.getById(99L, 7L)).isInstanceOf(
        ResourceNotFoundException.class).hasMessage("Order with id 99 was not found");

    verify(orderMapper, never()).toResponseDto(any(Order.class));
  }

  @Test
  void shouldReturnAllActiveOrders() {
    Pageable pageable = PageRequest.of(0, 20);
    Order order = createOrder();

    OrderResponseDto expectedResponse =
        new OrderResponseDto(
            1L,
            7L,
            OrderStatus.CREATED,
            new BigDecimal("100.00"),
            List.of(),
            null,
            null
        );

    when(orderRepository.findAllByDeletedFalse(pageable))
        .thenReturn(
            new PageImpl<>(
                List.of(order),
                pageable,
                1
            )
        );

    when(orderMapper.toResponseDto(order))
        .thenReturn(expectedResponse);

    Page<OrderResponseDto> actualResponse =
        orderService.getAll(pageable);

    assertThat(actualResponse.getContent())
        .containsExactly(expectedResponse);

    verify(orderRepository)
        .findAllByDeletedFalse(pageable);
    verify(orderMapper).toResponseDto(order);
  }

  @Test
  void shouldReturnActiveOrdersByUserId() {
    Pageable pageable = PageRequest.of(0, 20);
    Order order = createOrder();

    OrderResponseDto expectedResponse =
        new OrderResponseDto(
            1L,
            7L,
            OrderStatus.CREATED,
            new BigDecimal("100.00"),
            List.of(),
            null,
            null
        );

    when(
        orderRepository.findAllByUserIdAndDeletedFalse(
            7L,
            pageable
        )
    ).thenReturn(
        new PageImpl<>(
            List.of(order),
            pageable,
            1
        )
    );

    when(orderMapper.toResponseDto(order))
        .thenReturn(expectedResponse);

    Page<OrderResponseDto> actualResponse =
        orderService.getAllByUserId(
            7L,
            pageable
        );

    assertThat(actualResponse.getContent())
        .containsExactly(expectedResponse);

    verify(orderRepository)
        .findAllByUserIdAndDeletedFalse(
            7L,
            pageable
        );
    verify(orderMapper).toResponseDto(order);
  }

  @Test
  void shouldUpdateOrderStatus() {
    Order order = createOrder();

    OrderResponseDto expectedResponse = new OrderResponseDto(1L, 7L, OrderStatus.PROCESSING,
        new BigDecimal("100.00"), List.of(), null, null);

    when(orderRepository.findByIdAndUserIdAndDeletedFalse(1L, 7L)).thenReturn(Optional.of(order));

    when(orderMapper.toResponseDto(order)).thenReturn(expectedResponse);

    OrderResponseDto actualResponse = orderService.updateStatus(
        1L,
        7L,
        new OrderStatusUpdateDto(OrderStatus.PROCESSING));

    assertThat(order.getStatus()).isEqualTo(OrderStatus.PROCESSING);
    assertThat(actualResponse).isSameAs(expectedResponse);
  }

  @Test
  void shouldSoftDeleteOrder() {
    Order order = createOrder();

    when(orderRepository.findByIdAndUserIdAndDeletedFalse(1L, 7L)).thenReturn(Optional.of(order));

    orderService.delete(1L, 7L);

    assertThat(order.isDeleted()).isTrue();
    verify(orderRepository, never()).delete(any(Order.class));
  }

  private Item createItem(Long id, String name, String price) {
    return Item.builder().id(id).name(name).price(new BigDecimal(price)).build();
  }

  private Order createOrder() {
    return Order.builder().id(1L).userId(7L).status(OrderStatus.CREATED).totalPrice(new BigDecimal("100.00"))
        .deleted(false).build();
  }
}
