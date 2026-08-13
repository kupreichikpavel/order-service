package by.innowise.orderservice.service.impl;

import by.innowise.orderservice.dto.item.ItemRequestDto;
import by.innowise.orderservice.dto.item.ItemResponseDto;
import by.innowise.orderservice.entity.Item;
import by.innowise.orderservice.exception.ResourceNotFoundException;
import by.innowise.orderservice.mapper.ItemMapper;
import by.innowise.orderservice.repository.ItemRepository;
import by.innowise.orderservice.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemServiceImpl implements ItemService {

  private final ItemRepository itemRepository;

  private final ItemMapper itemMapper;

  @Override
  @Transactional
  public ItemResponseDto create(ItemRequestDto request) {
    Item item = itemMapper.toEntity(request);

    Item savedItem = itemRepository.save(item);

    return itemMapper.toResponseDto(savedItem);
  }

  @Override
  public ItemResponseDto getById(Long id) {
    return itemMapper.toResponseDto(
        getItem(id)
    );
  }

  @Override
  public Page<ItemResponseDto> getAll(Pageable pageable) {
    return itemRepository.findAll(pageable)
        .map(itemMapper::toResponseDto);
  }

  @Override
  @Transactional
  public ItemResponseDto update(
      Long id,
      ItemRequestDto request
  ) {
    Item item = getItem(id);

    itemMapper.updateEntity(
        request,
        item
    );

    return itemMapper.toResponseDto(item);
  }

  @Override
  @Transactional
  public void delete(Long id) {
    Item item = getItem(id);

    itemRepository.delete(item);
  }

  private Item getItem(Long id) {
    return itemRepository.findById(id)
        .orElseThrow(
            () -> new ResourceNotFoundException(
                "Item with id " + id + " was not found"
            )
        );
  }
}
