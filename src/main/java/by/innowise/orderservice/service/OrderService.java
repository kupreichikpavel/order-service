package by.innowise.orderservice.service;

import by.innowise.orderservice.dto.order.OrderCreateDto;
import by.innowise.orderservice.dto.order.OrderResponseDto;
import by.innowise.orderservice.dto.order.OrderStatusUpdateDto;
import by.innowise.orderservice.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {

  OrderResponseDto create(
      Long userId,
      OrderCreateDto request
  );

  OrderResponseDto getById(
      Long id,
      Long userId
  );

  Page<OrderResponseDto> getAll(
      Long userId,
      boolean admin,
      Pageable pageable
  );

  OrderResponseDto updateStatus(
      Long id,
      Long userId,
      OrderStatusUpdateDto request
  );

  void delete(
      Long id,
      Long userId
  );
  void updateStatusFromPayment(
      Long orderId,
      OrderStatus status
  );
}
