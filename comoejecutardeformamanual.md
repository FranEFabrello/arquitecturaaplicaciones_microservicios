# Cómo ejecutar inventory-service de forma manual

Arranque mínimo para levantar **inventory-service** sin Docker.

---

## Requisitos previos

- Java 21+
- Maven 3.8+
- Docker (solo si se desea usar RabbitMQ/Kafka)

---

## Orden de inicio

### Paso 1 — Config Server (puerto 8888)

Es el primero en levantarse. Todos los microservicios dependen de él para obtener su configuración.

> **Importante:** el perfil `native` es obligatorio al ejecutar manualmente. Sin él, Spring intenta usar el perfil `git` por defecto y falla con _"Invalid config server configuration"_ porque no hay un repositorio Git configurado. El perfil `native` le indica que lea los archivos de configuración desde la carpeta local `config/`.

```bash
cd config-server
mvn spring-boot:run -Dspring-boot.run.profiles=native
```

Verificar que está listo: http://localhost:8888/actuator/health

---

### Paso 2 — Eureka Server (puerto 8761)

Registro y descubrimiento de servicios. Debe estar activo antes que cualquier microservicio.

```bash
cd eureka-server
mvn spring-boot:run
```

Verificar que está listo: http://localhost:8761

---

### Paso 3 — Auth Service (puerto 8083)

Maneja autenticación y emisión de tokens JWT. Necesario para acceder a inventory vía API Gateway.

```bash
cd auth-service
mvn spring-boot:run
```

Verificar registro en Eureka: http://localhost:8761

---

### Paso 4 — RabbitMQ (opcional, solo si se usan eventos)

Si se necesita que el `notification-service` reciba eventos de creación de productos, hay que levantar RabbitMQ primero:

```bash
docker compose --profile rabbitmq up rabbitmq -d
```

Verificar que está listo: http://localhost:15672 (usuario: `guest`, contraseña: `guest`)

---

### Paso 5 — Inventory Service (puerto 8082)

Sin messaging (modo NoOp, los eventos se ignoran):
```bash
cd inventory-service
mvn spring-boot:run
```

Con RabbitMQ (publica eventos al broker):
```bash
cd inventory-service
mvn spring-boot:run -Dspring-boot.run.profiles=rabbitmq
```

Verificar registro en Eureka: http://localhost:8761

---

### Paso 6 — Notification Service (puerto 8084, requiere RabbitMQ)

Solo tiene sentido levantarlo si RabbitMQ está activo. Consume eventos de creación de productos y los muestra en los logs.

```bash
cd notification-service
mvn spring-boot:run -Dspring-boot.run.profiles=rabbitmq
```

Verificar registro en Eureka: http://localhost:8761

---

### Paso 7 — API Gateway (puerto 8080)

Punto de entrada único. Debe levantarse después de que los servicios ya estén registrados en Eureka.

```bash
cd api-gateway
mvn spring-boot:run
```

Verificar: http://localhost:8080

---

## Resumen del orden

| Orden | Servicio              | Puerto | Depende de               | Perfil       |
|-------|-----------------------|--------|--------------------------|--------------|
| 1     | Config Server         | 8888   | —                        | native       |
| 2     | Eureka Server         | 8761   | Config Server            | —            |
| 3     | Auth Service          | 8083   | Eureka                   | —            |
| 4     | RabbitMQ (Docker)     | 5672   | —                        | —            |
| 5     | Inventory Service     | 8082   | Eureka                   | rabbitmq (*)   |
| 6     | Notification Service  | 8084   | Eureka, RabbitMQ         | rabbitmq     |
| 7     | API Gateway           | 8080   | Eureka, Auth, Inventory  | —            |

(*) El perfil `rabbitmq` es opcional para inventory-service. Sin él, se usa el adaptador NoOp que ignora eventos.

