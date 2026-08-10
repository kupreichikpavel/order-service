package by.innowise.orderservice.mapper;

import by.innowise.orderservice.dto.item.ItemRequestDto;
import by.innowise.orderservice.dto.item.ItemResponseDto;
import by.innowise.orderservice.entity.Item;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ItemMapper {

  ItemResponseDto toResponseDto(Item item);

  @Mapping(target = "id", ignore = true)
  Item toEntity(ItemRequestDto request);

  @Mapping(target = "id", ignore = true)
  void updateEntity(
      ItemRequestDto request,
      @MappingTarget Item item
  );
}
