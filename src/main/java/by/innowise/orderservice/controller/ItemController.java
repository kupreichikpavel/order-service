package by.innowise.orderservice.controller;

import by.innowise.orderservice.dto.item.ItemRequestDto;
import by.innowise.orderservice.dto.item.ItemResponseDto;
import by.innowise.orderservice.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/items")
@Tag(name = "Items", description = "Operations for managing items")
public class ItemController {

  private final ItemService itemService;

  @PostMapping
  @Operation(summary = "Create a new item")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Item successfully created"),
      @ApiResponse(responseCode = "400", description = "Request validation failed")
  })
  public ResponseEntity<ItemResponseDto> create(
      @Valid @RequestBody ItemRequestDto request
  ) {
    ItemResponseDto response = itemService.create(request);

    URI location = ServletUriComponentsBuilder
        .fromCurrentRequest()
        .path("/{id}")
        .buildAndExpand(response.id())
        .toUri();

    return ResponseEntity.created(location).body(response);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get an item by id")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Item successfully returned"),
      @ApiResponse(responseCode = "400", description = "Item id is invalid"),
      @ApiResponse(responseCode = "404", description = "Item was not found")
  })
  public ResponseEntity<ItemResponseDto> getById(
      @PathVariable
      @Positive(message = "Item id must be positive")
      Long id
  ) {
    return ResponseEntity.ok(
        itemService.getById(id)
    );
  }

  @GetMapping
  @Operation(summary = "Get all items")
  public ResponseEntity<Page<ItemResponseDto>> getAll(
      @ParameterObject Pageable pageable
  ) {
    return ResponseEntity.ok(
        itemService.getAll(pageable)
    );
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update an item")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Item successfully updated"),
      @ApiResponse(responseCode = "400", description = "Request validation failed"),
      @ApiResponse(responseCode = "404", description = "Item was not found")
  })
  public ResponseEntity<ItemResponseDto> update(
      @PathVariable
      @Positive(message = "Item id must be positive")
      Long id,
      @Valid @RequestBody ItemRequestDto request
  ) {
    return ResponseEntity.ok(
        itemService.update(
            id,
            request
        )
    );
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete an item")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Item successfully deleted"),
      @ApiResponse(responseCode = "400", description = "Item id is invalid"),
      @ApiResponse(responseCode = "404", description = "Item was not found")
  })
  public ResponseEntity<Void> delete(
      @PathVariable
      @Positive(message = "Item id must be positive")
      Long id
  ) {
    itemService.delete(id);

    return ResponseEntity.noContent().build();
  }
}
