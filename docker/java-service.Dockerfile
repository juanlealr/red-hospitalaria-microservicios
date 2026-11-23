# syntax=docker/dockerfile:1
# Un solo Dockerfile con varias salidas (stages) para los 7 servicios Spring Boot.
# El stage "builder" compila TODO una sola vez y BuildKit reutiliza su capa en
# cada target, asi que levantar los 7 servicios no recompila 7 veces.

FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /workspace
COPY . .
RUN --mount=type=cache,target=/root/.m2 mvn -q -B -DskipTests -f admisiones-service/pom.xml package
RUN --mount=type=cache,target=/root/.m2 mvn -q -B -DskipTests -f auth_service/pom.xml package
RUN --mount=type=cache,target=/root/.m2 mvn -q -B -DskipTests -f consultas_admisiones_service/pom.xml package
RUN --mount=type=cache,target=/root/.m2 mvn -q -B -DskipTests -f eureka-server/pom.xml package
RUN --mount=type=cache,target=/root/.m2 mvn -q -B -DskipTests -f farmacia-service/pom.xml package
RUN --mount=type=cache,target=/root/.m2 mvn -q -B -DskipTests -f gateway/pom.xml package
RUN --mount=type=cache,target=/root/.m2 mvn -q -B -DskipTests -f laboratorio-service/pom.xml package

FROM eclipse-temurin:25-jre AS eureka
WORKDIR /app
COPY --from=builder /workspace/eureka-server/target/eureka-server-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

FROM eclipse-temurin:25-jre AS auth
WORKDIR /app
COPY --from=builder /workspace/auth_service/target/auth_service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

FROM eclipse-temurin:25-jre AS admisiones
WORKDIR /app
COPY --from=builder /workspace/admisiones-service/target/admisiones-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

FROM eclipse-temurin:25-jre AS farmacia
WORKDIR /app
COPY --from=builder /workspace/farmacia-service/target/farmacia-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

FROM eclipse-temurin:25-jre AS laboratorio
WORKDIR /app
COPY --from=builder /workspace/laboratorio-service/target/laboratorio-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8082 9091
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

FROM eclipse-temurin:25-jre AS consultas
WORKDIR /app
COPY --from=builder /workspace/consultas_admisiones_service/target/consultas_admisiones_service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

FROM eclipse-temurin:25-jre AS gateway
WORKDIR /app
COPY --from=builder /workspace/gateway/target/gateway-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
