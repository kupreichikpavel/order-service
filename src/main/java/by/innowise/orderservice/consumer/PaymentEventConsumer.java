package by.innowise.orderservice.consumer;

import by.innowise.orderservice.entity.OrderStatus;
import by.innowise.orderservice.event.PaymentEvent;
import by.innowise.orderservice.event.PaymentEventType;
import by.innowise.orderservice.event.PaymentStatus;
import by.innowise.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

  private final OrderService orderService;

  @KafkaListener(
      topics = "${app.kafka.payment-events-topic}"
  )
  public void consume(PaymentEvent event) {
    if (event.type() != PaymentEventType.CREATE_PAYMENT) {
      return;
    }

    OrderStatus orderStatus =
        event.status() == PaymentStatus.SUCCESS
            ? OrderStatus.COMPLETED
            : OrderStatus.FAILED;

    orderService.updateStatusFromPayment(
        event.orderId(),
        orderStatus
    );
  }
}