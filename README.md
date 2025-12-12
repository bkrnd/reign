# Reign Backend

Reign is a multiplayer territory conquest game. Players can capture and defend squares on a grid-based board in real-time. 
The backend is built with Spring Boot and uses WebSocket for real-time updates.

This is only the backend service and you will need a [frontend client](https://github.com/bkrnd/reign-client) to interact with the game.

## Technologies

- Java 21
- Spring Boot 3.5.8
- PostgreSQL
- WebSocket (STOMP)
- Maven

## Setup

1. Start PostgreSQL database:
```bash
docker compose up -d
```

2. Run the application:
```bash
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`