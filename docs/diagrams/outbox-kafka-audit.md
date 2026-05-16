# Outbox, Kafka and Audit Timeline

```mermaid
flowchart LR
    BookingTx[Booking local transaction] --> BookingState[(booking tables)]
    BookingTx --> BookingOutbox[(booking_outbox)]

    PaymentTx[Payment local transaction] --> PaymentState[(payment tables)]
    PaymentTx --> PaymentOutbox[(payment_outbox)]

    BookingPublisher[booking outbox publisher] --> BookingOutbox
    PaymentPublisher[payment outbox publisher] --> PaymentOutbox

    BookingPublisher -->|booking.events| Kafka[(Kafka)]
    PaymentPublisher -->|payment.events| Kafka

    Kafka --> AuditConsumer[audit-service consumer]
    AuditConsumer --> Timeline[(MongoDB booking timeline)]

    Kafka --> NotificationConsumer[notification-service consumer]
    NotificationConsumer --> NotificationTasks[(MongoDB notification tasks)]
```

## Why outbox is used

The outbox event is stored in the same local transaction as business state. This avoids the classic failure where the database commit succeeds but the application crashes before publishing the Kafka event.

