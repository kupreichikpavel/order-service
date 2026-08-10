package by.innowise.orderservice.controller;

import by.innowise.orderservice.config.SecurityConfig;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import by.innowise.orderservice.dto.item.ItemRequestDto;
import by.innowise.orderservice.dto.item.ItemResponseDto;
import by.innowise.orderservice.exception.ResourceNotFoundException;
import by.innowise.orderservice.handler.GlobalExceptionHandler;
import by.innowise.orderservice.service.ItemService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ItemController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class ItemControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ItemService itemService;

  @MockitoBean
  private JwtDecoder jwtDecoder;

  @MockitoBean(name = "jpaMappingContext")
  private JpaMetamodelMappingContext jpaMappingContext;

  @Test
  void shouldCreateItem() throws Exception {
    ItemResponseDto response = response();

    when(itemService.create(any(ItemRequestDto.class)))
        .thenReturn(response);

    mockMvc.perform(
            post("/api/v1/items")
                .with(jwt().authorities(
                    new SimpleGrantedAuthority("ROLE_ADMIN")
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Keyboard",
                      "price": 50.00
                    }
                    """)
        )
        .andExpect(status().isCreated())
        .andExpect(header().string(
            "Location",
            "http://localhost/api/v1/items/1"
        ))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.name").value("Keyboard"))
        .andExpect(jsonPath("$.price").value(50.00));

    verify(itemService).create(any(ItemRequestDto.class));
  }

  @Test
  void shouldGetItemById() throws Exception {
    when(itemService.getById(1L))
        .thenReturn(response());

    mockMvc.perform(
            get("/api/v1/items/{id}", 1L)
                .with(jwt().authorities(
                    new SimpleGrantedAuthority("ROLE_USER")
                ))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.name").value("Keyboard"));

    verify(itemService).getById(1L);
  }

  @Test
  void shouldGetAllItems() throws Exception {
    when(itemService.getAll(any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(response())));

    mockMvc.perform(
            get("/api/v1/items")
                .with(jwt().authorities(
                    new SimpleGrantedAuthority("ROLE_USER")
                ))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[0].id").value(1));

    verify(itemService).getAll(any(Pageable.class));
  }

  @Test
  void shouldUpdateItem() throws Exception {
    when(itemService.update(
        eq(1L),
        any(ItemRequestDto.class)
    )).thenReturn(response());

    mockMvc.perform(
            put("/api/v1/items/{id}", 1L)
                .with(jwt().authorities(
                    new SimpleGrantedAuthority("ROLE_ADMIN")
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Keyboard",
                      "price": 50.00
                    }
                    """)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1));

    verify(itemService).update(
        eq(1L),
        any(ItemRequestDto.class)
    );
  }

  @Test
  void shouldDeleteItem() throws Exception {
    mockMvc.perform(
            delete("/api/v1/items/{id}", 1L)
                .with(jwt().authorities(
                    new SimpleGrantedAuthority("ROLE_ADMIN")
                ))
        )
        .andExpect(status().isNoContent());

    verify(itemService).delete(1L);
  }

  @Test
  void shouldRejectInvalidItem() throws Exception {
    mockMvc.perform(
            post("/api/v1/items")
                .with(jwt().authorities(
                    new SimpleGrantedAuthority("ROLE_ADMIN")
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "",
                      "price": -1
                    }
                    """)
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Validation failed"))
        .andExpect(jsonPath("$.errors.name").exists())
        .andExpect(jsonPath("$.errors.price").exists());

    verifyNoInteractions(itemService);
  }

  @Test
  void shouldReturnNotFoundForMissingItem() throws Exception {
    when(itemService.getById(99L))
        .thenThrow(
            new ResourceNotFoundException(
                "Item with id 99 was not found"
            )
        );

    mockMvc.perform(
            get("/api/v1/items/{id}", 99L)
                .with(jwt().authorities(
                    new SimpleGrantedAuthority("ROLE_USER")
                ))
        )
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Resource not found"))
        .andExpect(
            jsonPath("$.detail")
                .value("Item with id 99 was not found")
        );
  }

  private ItemResponseDto response() {
    return new ItemResponseDto(
        1L,
        "Keyboard",
        new BigDecimal("50.00"),
        null,
        null
    );
  }
}
