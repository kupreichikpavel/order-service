package by.innowise.orderservice.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import by.innowise.orderservice.entity.Item;
import by.innowise.orderservice.entity.Order;
import by.innowise.orderservice.repository.ItemRepository;
import by.innowise.orderservice.repository.OrderRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(properties = "spring.docker.compose.enabled=false")
@AutoConfigureMockMvc
@Testcontainers
class OrderFlowIntegrationTest {

  private static final long USER_ID = 7L;
  private static final long OTHER_USER_ID = 8L;

  @Container
  private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
      DockerImageName.parse("postgres:16-alpine")).withDatabaseName("order_service_test")
      .withUsername("test").withPassword("test");

  @DynamicPropertySource
  static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ItemRepository itemRepository;

  @Autowired
  private OrderRepository orderRepository;

  @MockitoBean
  private JwtDecoder jwtDecoder;

  private Long itemId;

  @BeforeEach
  void setUp() {
    orderRepository.deleteAll();
    itemRepository.deleteAll();

    Item item = Item.builder().name("Keyboard").price(new BigDecimal("50.00")).build();

    itemId = itemRepository.save(item).getId();
  }

  @Test
  void shouldCreateOrderAndPersistIt() throws Exception {
    performAsUser(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content("""
        {
          "items": [
            {
              "itemId": %d,
              "quantity": 2
            }
          ]
        }
        """.formatted(itemId)), USER_ID).andExpect(status().isCreated())
        .andExpect(header().exists("Location")).andExpect(jsonPath("$.userId").value(USER_ID))
        .andExpect(jsonPath("$.status").value("CREATED"))
        .andExpect(jsonPath("$.totalPrice").value(100.00)).andExpect(jsonPath("$.items").isArray())
        .andExpect(jsonPath("$.items[0].itemId").value(itemId))
        .andExpect(jsonPath("$.items[0].itemName").value("Keyboard"))
        .andExpect(jsonPath("$.items[0].quantity").value(2))
        .andExpect(jsonPath("$.items[0].totalPrice").value(100.00));

    List<Order> orders = orderRepository.findAll();

    assertThat(orders).hasSize(1);

    Long orderId = orders.getFirst().getId();

    Order savedOrder = orderRepository.findByIdAndUserIdAndDeletedFalse(orderId, USER_ID)
        .orElseThrow();

    assertThat(savedOrder.getUserId()).isEqualTo(USER_ID);

    assertThat(savedOrder.getStatus()).isEqualTo("CREATED");

    assertThat(savedOrder.getTotalPrice()).isEqualByComparingTo("100.00");

    assertThat(savedOrder.getOrderItems()).hasSize(1);

    assertThat(savedOrder.getOrderItems().getFirst().getQuantity()).isEqualTo(2);
  }

  @Test
  void shouldNotReturnAnotherUsersOrder() throws Exception {
    createOrderForUser(USER_ID);

    Long orderId = orderRepository.findAll().getFirst().getId();

    performAsUser(get("/api/v1/orders/{id}", orderId), OTHER_USER_ID).andExpect(
            status().isNotFound()).andExpect(jsonPath("$.title").value("Resource not found"))
        .andExpect(jsonPath("$.detail").value("Order with id %d was not found".formatted(orderId)));

    performAsUser(get("/api/v1/orders/{id}", orderId), USER_ID).andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(orderId)).andExpect(jsonPath("$.userId").value(USER_ID));
  }

  @Test
  void shouldRejectRequestWithoutAuthentication() throws Exception {
    mockMvc.perform(get("/api/v1/orders/{id}", 1L)).andExpect(status().isUnauthorized());
  }

  private void createOrderForUser(long userId) throws Exception {
    performAsUser(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content("""
        {
          "items": [
            {
              "itemId": %d,
              "quantity": 1
            }
          ]
        }
        """.formatted(itemId)), userId).andExpect(status().isCreated());
  }

  private ResultActions performAsUser(MockHttpServletRequestBuilder request, long userId)
      throws Exception {
    return mockMvc.perform(request.with(jwt().jwt(
            token -> token.claim("userId", Long.toString(userId))
                .claim("realm_access", Map.of("roles", List.of("USER"))))
        .authorities(new SimpleGrantedAuthority("ROLE_USER"))));
  }
}
