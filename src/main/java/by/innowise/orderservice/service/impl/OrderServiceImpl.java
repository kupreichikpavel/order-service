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

  private final OrderRepository orderRepository;
  private final ItemRepository itemRepository;
  private final OrderMapper orderMapper;

  @Override
  @Transactional
  public OrderResponseDto create(
      Long userId,
      OrderCreateDto request
  ) {
    Set<Long> requestedItemIds =
        extractRequestedItemIds(request);

    Map<Long, Item> itemsById =
        getItemsById(requestedItemIds);

    validateItemsExist(
        requestedItemIds,
        itemsById
    );

    Order order = buildOrder(userId);

    BigDecimal totalPrice = addOrderItems(
        order,
        request,
        itemsById
    );

    order.setTotalPrice(totalPrice);

    Order savedOrder = orderRepository.save(order);

    return orderMapper.toResponseDto(savedOrder);
  }

  private Set<Long> extractRequestedItemIds(
      OrderCreateDto request
  ) {
    return request.items()
        .stream()
        .map(orderItem -> orderItem.itemId())
        .collect(Collectors.toSet());
  }

  private Map<Long, Item> getItemsById(
      Set<Long> requestedItemIds
  ) {
    return itemRepository
        .findAllById(requestedItemIds)
        .stream()
        .collect(Collectors.toMap(
            Item::getId,
            Function.identity()
        ));
  }

  private Order buildOrder(Long userId) {
    return Order.builder()
        .userId(userId)
        .status(OrderStatus.CREATED)
        .totalPrice(BigDecimal.ZERO)
        .deleted(false)
        .build();
  }

  private BigDecimal addOrderItems(
      Order order,
      OrderCreateDto request,
      Map<Long, Item> itemsById
  ) {
    BigDecimal totalPrice = BigDecimal.ZERO;

    for (var requestedOrderItem : request.items()) {
      Item item = itemsById.get(
          requestedOrderItem.itemId()
      );

      OrderItem orderItem = OrderItem.builder()
          .order(order)
          .item(item)
          .quantity(requestedOrderItem.quantity())
          .build();

      order.getOrderItems().add(orderItem);

      totalPrice = totalPrice.add(
          calculatePositionPrice(
              item,
              requestedOrderItem.quantity()
          )
      );
    }

    return totalPrice;
  }

  private BigDecimal calculatePositionPrice(
      Item item,
      Integer quantity
  ) {
    return item.getPrice()
        .multiply(BigDecimal.valueOf(quantity));
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
  public Page<OrderResponseDto> getAll(
      Long userId,
      boolean admin,
      Pageable pageable
  ) {
    if (admin) {
      return orderRepository
          .findAllByDeletedFalse(pageable)
          .map(orderMapper::toResponseDto);
    }

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

  @Override
  @Transactional
  public void updateStatusFromPayment(
      Long orderId,
      OrderStatus status
  ) {
    Order order = orderRepository
        .findByIdAndDeletedFalse(orderId)
        .orElseThrow(() ->
            new ResourceNotFoundException(
                "Order not found: " + orderId
            )
        );

    order.setStatus(status);
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
