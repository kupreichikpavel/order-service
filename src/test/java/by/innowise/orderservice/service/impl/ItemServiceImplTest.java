package by.innowise.orderservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import by.innowise.orderservice.dto.item.ItemRequestDto;
import by.innowise.orderservice.dto.item.ItemResponseDto;
import by.innowise.orderservice.entity.Item;
import by.innowise.orderservice.exception.ResourceNotFoundException;
import by.innowise.orderservice.mapper.ItemMapper;
import by.innowise.orderservice.repository.ItemRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {

  @Mock
  private ItemRepository itemRepository;

  @Mock
  private ItemMapper itemMapper;

  @InjectMocks
  private ItemServiceImpl itemService;

  private Item item;

  private ItemRequestDto request;

  private ItemResponseDto response;

  @BeforeEach
  void setUp() {
    item = Item.builder()
        .id(1L)
        .name("Keyboard")
        .price(new BigDecimal("50.00"))
        .build();

    request = new ItemRequestDto(
        "Keyboard",
        new BigDecimal("50.00")
    );

    response = new ItemResponseDto(
        1L,
        "Keyboard",
        new BigDecimal("50.00"),
        null,
        null
    );
  }

  @Test
  void shouldCreateItem() {
    when(itemMapper.toEntity(request))
        .thenReturn(item);

    when(itemRepository.save(item))
        .thenReturn(item);

    when(itemMapper.toResponseDto(item))
        .thenReturn(response);

    ItemResponseDto result = itemService.create(request);

    assertThat(result).isEqualTo(response);

    verify(itemMapper).toEntity(request);
    verify(itemRepository).save(item);
    verify(itemMapper).toResponseDto(item);
  }

  @Test
  void shouldGetItemById() {
    when(itemRepository.findById(1L))
        .thenReturn(Optional.of(item));

    when(itemMapper.toResponseDto(item))
        .thenReturn(response);

    ItemResponseDto result = itemService.getById(1L);

    assertThat(result).isEqualTo(response);

    verify(itemRepository).findById(1L);
    verify(itemMapper).toResponseDto(item);
  }

  @Test
  void shouldThrowWhenItemNotFound() {
    when(itemRepository.findById(99L))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> itemService.getById(99L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Item with id 99 was not found");

    verify(itemRepository).findById(99L);
  }

  @Test
  void shouldGetAllItems() {
    Pageable pageable = PageRequest.of(0, 10);

    when(itemRepository.findAll(pageable))
        .thenReturn(new PageImpl<>(List.of(item)));

    when(itemMapper.toResponseDto(item))
        .thenReturn(response);

    Page<ItemResponseDto> result =
        itemService.getAll(pageable);

    assertThat(result.getContent())
        .containsExactly(response);

    assertThat(result.getTotalElements())
        .isEqualTo(1);

    verify(itemRepository).findAll(pageable);
    verify(itemMapper).toResponseDto(item);
  }

  @Test
  void shouldUpdateItem() {
    ItemRequestDto updateRequest =
        new ItemRequestDto(
            "Mechanical Keyboard",
            new BigDecimal("75.00")
        );

    ItemResponseDto updatedResponse =
        new ItemResponseDto(
            1L,
            "Mechanical Keyboard",
            new BigDecimal("75.00"),
            null,
            null
        );

    when(itemRepository.findById(1L))
        .thenReturn(Optional.of(item));

    when(itemMapper.toResponseDto(item))
        .thenReturn(updatedResponse);

    ItemResponseDto result =
        itemService.update(
            1L,
            updateRequest
        );

    assertThat(result).isEqualTo(updatedResponse);

    verify(itemRepository).findById(1L);

    verify(itemMapper).updateEntity(
        updateRequest,
        item
    );

    verify(itemMapper).toResponseDto(item);
  }

  @Test
  void shouldDeleteItem() {
    when(itemRepository.findById(1L))
        .thenReturn(Optional.of(item));

    itemService.delete(1L);

    verify(itemRepository).findById(1L);
    verify(itemRepository).delete(item);
  }

  @Test
  void shouldThrowWhenDeletingMissingItem() {
    when(itemRepository.findById(99L))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> itemService.delete(99L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Item with id 99 was not found");

    verify(itemRepository).findById(99L);
  }
}
