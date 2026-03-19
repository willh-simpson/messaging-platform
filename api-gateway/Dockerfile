FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

# copying full monorepo so Maven can resolve any inter-module dependencies
COPY pom.xml .

COPY common/pom.xml common/
COPY common/src common/src

COPY grpc-stubs/pom.xml grpc-stubs/
COPY grpc-stubs/src grpc-stubs/src

COPY api-gateway/pom.xml api-gateway/
COPY api-gateway/src api-gateway/src

COPY messaging-service/pom.xml messaging-service/
COPY messaging-service/src messaging-service/src

COPY persistence-service/pom.xml persistence-service/
COPY persistence-service/src persistence-service/src

COPY delivery-service/pom.xml delivery-service/
COPY delivery-service/src delivery-service/src

# only this service and its dependencies will be built
RUN ./mvnw clean package -pl api-gateway -am -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# everything from target/ should be excluded except for fat jar from builder stage
COPY --from=builder /workspace/api-gateway/target/*.jar app.jar

RUN chown appuser:appgroup app.jar
USER appuser

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]