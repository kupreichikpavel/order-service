package by.innowise.orderservice.service;

import by.innowise.orderservice.dto.item.ItemRequestDto;
import by.innowise.orderservice.dto.item.ItemResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ItemService {

  ItemResponseDto create(ItemRequestDto request);

  ItemResponseDto getById(Long id);

  Page<ItemResponseDto> getAll(Pageable pageable);

  ItemResponseDto update(
      Long id,
      ItemRequestDto request
  );

  void delete(Long id);
}
