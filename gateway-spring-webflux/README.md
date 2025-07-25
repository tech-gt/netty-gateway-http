# Spring Cloud Gateway (WebFlux)

This project is a simple API gateway built using Spring Cloud Gateway.

It's configured to forward requests for `/employees/**` to three different backend service instances running on ports 8083, 8084, and 8085.

## Features

- **Routing**: All requests matching the path `/employees/**` are routed.
- **Load Balancing**: Uses the `Weight` predicate to perform weighted random load balancing across the three backend instances. Since all instances have a weight of `1`, they will receive an equal share of the traffic.

## Prerequisites

- Java 17 or later
- Maven 3.6+
- Backend services running on `http://localhost:8083`, `http://localhost:8084`, and `http://localhost:8085`.

## How to Run

1.  **Build the project:**
    ```shell
    mvn clean package
    ```

2.  **Run the application:**
    ```shell
    java -jar target/gateway-spring-webflux-0.0.1-SNAPSHOT.jar
    ```

The gateway will start on port `8080`.

## How to Test

Once the gateway is running, you can send requests to `http://localhost:8080/employees/...`.

For example, using `curl`:
```shell
curl http://localhost:8080/employees/1
```

Each time you send a request, the gateway will forward it to one of the three backend instances (`8083`, `8084`, `8085`) based on the load balancing configuration. You can check the logs of your backend services to see which one received the request. 