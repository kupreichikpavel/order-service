package by.innowise.orderservice.mapper;

import by.innowise.orderservice.dto.orderitem.OrderItemResponseDto;
import by.innowise.orderservice.entity.OrderItem;
import java.math.BigDecimal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrderItemMapper {

  @Mapping(target = "itemId", source = "item.id")
  @Mapping(target = "itemName", source = "item.name")
  @Mapping(target = "itemPrice", source = "item.price")
  @Mapping(
      target = "totalPrice",
      expression = "java(calculateTotalPrice(orderItem))"
  )
  OrderItemResponseDto toResponseDto(OrderItem orderItem);

  default BigDecimal calculateTotalPrice(OrderItem orderItem) {
    if (orderItem.getItem() == null
        || orderItem.getItem().getPrice() == null
        || orderItem.getQuantity() == null) {
      return BigDecimal.ZERO;
    }

    return orderItem
        .getItem()
        .getPrice()
        .multiply(BigDecimal.valueOf(orderItem.getQuantity()));
  }
}
