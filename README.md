# Hotel Booking

A modular monolith backend for hotel booking, built with Java and Spring Boot.

This project is being developed as a learning-oriented implementation of Clean Architecture, DDD, and an evolutionary path from a modular monolith to microservices.

## Current stage: v0.2.0

The project is a learning modular monolith for hotel booking.

Current focus:
- Clean Architecture boundaries
- DDD-style aggregates and value objects
- explicit booking and inventory modules
- booking-inventory integration through published application use cases
- in-memory infrastructure adapters
- room availability, room holds, booking confirmation and cancellation of held bookings

The project is intentionally not production-ready yet.
Persistence, transactions, outbox/events and stronger consistency guarantees are planned as future improvements.