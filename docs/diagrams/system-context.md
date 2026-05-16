# System Context Diagram

```mermaid
flowchart LR
    User[User / Interviewer] --> UI[Demo UI<br/>React + Vite]

    UI -->|HTTP + Google JWT| Booking[booking-service]
    UI -->|HTTP| Inventory[inventory-service]
    UI -->|HTTP| Audit[audit-service]

    Booking -->|gRPC + mTLS| Inventory
    Booking -->|HTTP| Payment[payment-service]

    Booking --> BookingDb[(PostgreSQL<br/>booking)]
    Payment --> PaymentDb[(PostgreSQL<br/>payment)]
    Inventory --> InventoryDb[(MongoDB<br/>inventory)]
    Notification --> NotificationDb[(MongoDB<br/>notifications)]
    Audit --> AuditDb[(MongoDB<br/>audit timeline)]

    Booking -->|booking.events| Kafka[(Kafka)]
    Payment -->|payment.events| Kafka

    Kafka --> Notification[notification-service]
    Kafka --> Audit
```

## Notes

- Booking-service owns the main booking lifecycle.
- Inventory-service owns availability and finite room capacity.
- Payment-service owns payment authorization state.
- Kafka is used for asynchronous projections and side effects.
- Audit-service builds timeline read model from events.

