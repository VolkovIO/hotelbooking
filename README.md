# Hotel Booking

A modular monolith backend for hotel booking, built with Java and Spring Boot.

This project is being developed as a learning-oriented implementation of Clean Architecture, DDD, and an evolutionary path from a modular monolith to microservices.

## Current stage

At the current stage, the application supports:

- hotel registration
- room type registration inside a hotel
- room availability configuration by date range
- room availability lookup
- booking creation
- inventory hold placement during booking creation
- booking retrieval by id
- booking cancellation with hold release
- booking confirmation with hold finalization

## Current domain modules

- **booking** — booking lifecycle and booking API
- **inventory** — hotels, room types, availability, holds
- **api** — shared HTTP error handling

## Technical notes

- persistence currently uses an **in-memory profile**
- OpenAPI/Swagger annotations are used for HTTP endpoints
- the codebase is organized as a **modular monolith**
- static analysis is enabled with Checkstyle, PMD, and SpotBugs

## Planned next steps

The following features are planned for future iterations:

- PostgreSQL persistence
- Liquibase migrations
- payment flow
- hold expiration / timeout handling
- richer booking lifecycle
- integration tests stabilization
- observability and metrics
- gradual decomposition toward microservices

## Goal of the project

The goal is not only to build a working backend, but also to practice:

- Clean Architecture
- Domain-Driven Design
- modular monolith design
- API design
- evolutionary architecture
- preparation for microservices, messaging, and production-grade engineering practices