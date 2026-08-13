package by.innowise.orderservice.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(
      name = "user_id",
      nullable = false
  )
  private Long userId;

  @Column(
      name = "status",
      nullable = false,
      length = 50
  )
  @Enumerated(EnumType.STRING)
  private OrderStatus status;
  @Builder.Default
  @Column(
      name = "total_price",
      nullable = false,
      precision = 19,
      scale = 2
  )
  private BigDecimal totalPrice = BigDecimal.ZERO;

  @Builder.Default
  @Column(
      name = "deleted",
      nullable = false
  )
  private boolean deleted = false;

  @Builder.Default
  @OneToMany(
      mappedBy = "order",
      cascade = CascadeType.ALL,
      orphanRemoval = true
  )
  private List<OrderItem> orderItems = new ArrayList<>();
}
