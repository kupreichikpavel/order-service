package by.innowise.orderservice.repository;

import by.innowise.orderservice.entity.Order;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

  @EntityGraph(attributePaths = {
      "orderItems",
      "orderItems.item"
  })
  Optional<Order> findByIdAndUserIdAndDeletedFalse(
      Long id,
      Long userId
  );

  Optional<Order> findByIdAndDeletedFalse(Long id);

  Page<Order> findAllByDeletedFalse(Pageable pageable);

  Page<Order> findAllByUserIdAndDeletedFalse(
      Long userId,
      Pageable pageable
  );
}
