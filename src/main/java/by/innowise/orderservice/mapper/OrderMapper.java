package by.innowise.orderservice.mapper;

import by.innowise.orderservice.dto.order.OrderResponseDto;
import by.innowise.orderservice.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    uses = OrderItemMapper.class
)
public interface OrderMapper {

  @Mapping(target = "items", source = "orderItems")
  OrderResponseDto toResponseDto(Order order);
}
