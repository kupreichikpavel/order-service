package by.innowise.orderservice.service;

import by.innowise.orderservice.dto.order.OrderCreateDto;
import by.innowise.orderservice.dto.order.OrderResponseDto;
import by.innowise.orderservice.dto.order.OrderStatusUpdateDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {

  OrderResponseDto create(OrderCreateDto request);

  OrderResponseDto getById(Long id);

  Page<OrderResponseDto> getAll(Pageable pageable);

  Page<OrderResponseDto> getAllByUserId(
      Long userId,
      Pageable pageable
  );

  OrderResponseDto updateStatus(
      Long id,
      OrderStatusUpdateDto request
  );

  void delete(Long id);
}
