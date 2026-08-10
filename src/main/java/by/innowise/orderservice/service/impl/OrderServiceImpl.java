package by.innowise.orderservice.service.impl;

import by.innowise.orderservice.dto.order.OrderCreateDto;
import by.innowise.orderservice.dto.order.OrderResponseDto;
import by.innowise.orderservice.dto.order.OrderStatusUpdateDto;
import by.innowise.orderservice.entity.Item;
import by.innowise.orderservice.entity.Order;
import by.innowise.orderservice.entity.OrderStatus;
import by.innowise.orderservice.entity.OrderItem;
import by.innowise.orderservice.exception.ResourceNotFoundException;
import by.innowise.orderservice.mapper.OrderMapper;
import by.innowise.orderservice.repository.ItemRepository;
import by.innowise.orderservice.repository.OrderRepository;
import by.innowise.orderservice.service.OrderService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

  private static final String INITIAL_STATUS = "CREATED";

  private final OrderRepository orderRepository;
  private final ItemRepository itemRepository;
  private final OrderMapper orderMapper;

  @Override
  @Transactional
  public OrderResponseDto create(
      Long userId,
      OrderCreateDto request
  ) {
    Set<Long> requestedItemIds = request.items().stream()
        .map(orderItem -> orderItem.itemId())
        .collect(Collectors.toSet());

    Map<Long, Item> itemsById = itemRepository
        .findAllById(requestedItemIds)
        .stream()
        .collect(Collectors.toMap(
            Item::getId,
            Function.identity()
        ));

    validateItemsExist(requestedItemIds, itemsById);

    Order order = Order.builder()
        .userId(userId)
        .status(OrderStatus.CREATED)
        .totalPrice(BigDecimal.ZERO)
        .deleted(false)
        .build();

    BigDecimal totalPrice = BigDecimal.ZERO;

    for (var requestedOrderItem : request.items()) {
      Item item = itemsById.get(requestedOrderItem.itemId());

      OrderItem orderItem = OrderItem.builder()
          .order(order)
          .item(item)
          .quantity(requestedOrderItem.quantity())
          .build();

      order.getOrderItems().add(orderItem);

      BigDecimal positionPrice = item.getPrice()
          .multiply(
              BigDecimal.valueOf(
                  requestedOrderItem.quantity()
              )
          );

      totalPrice = totalPrice.add(positionPrice);
    }

    order.setTotalPrice(totalPrice);

    Order savedOrder = orderRepository.save(order);

    return orderMapper.toResponseDto(savedOrder);
  }

  @Override
  public OrderResponseDto getById(
      Long id,
      Long userId
  ) {
    return orderMapper.toResponseDto(
        getActiveOrder(id, userId)
    );
  }

  @Override
  public Page<OrderResponseDto> getAll(Pageable pageable) {
    return orderRepository
        .findAllByDeletedFalse(pageable)
        .map(orderMapper::toResponseDto);
  }

  @Override
  public Page<OrderResponseDto> getAllByUserId(
      Long userId,
      Pageable pageable
  ) {
    return orderRepository
        .findAllByUserIdAndDeletedFalse(userId, pageable)
        .map(orderMapper::toResponseDto);
  }

  @Override
  @Transactional
  public OrderResponseDto updateStatus(
      Long id,
      Long userId,
      OrderStatusUpdateDto request
  ) {
    Order order = getActiveOrder(id, userId);

    order.setStatus(request.status());

    return orderMapper.toResponseDto(order);
  }

  @Override
  @Transactional
  public void delete(
      Long id,
      Long userId
  ) {
    Order order = getActiveOrder(id, userId);

    order.setDeleted(true);
  }

  private Order getActiveOrder(
      Long id,
      Long userId
  ) {
    return orderRepository
        .findByIdAndUserIdAndDeletedFalse(
            id,
            userId
        )
        .orElseThrow(() -> new ResourceNotFoundException(
            "Order with id %d was not found".formatted(id)
        ));
  }

  private void validateItemsExist(
      Set<Long> requestedItemIds,
      Map<Long, Item> itemsById
  ) {
    if (itemsById.size() == requestedItemIds.size()) {
      return;
    }

    List<Long> missingItemIds = requestedItemIds.stream()
        .filter(itemId -> !itemsById.containsKey(itemId))
        .sorted()
        .toList();

    throw new ResourceNotFoundException(
        "Items were not found: " + missingItemIds
    );
  }
}
