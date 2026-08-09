# syntax=docker/dockerfile:1

########################################
# Stage: builder — one reactor build, shared by every service target below
########################################
FROM maven:3.9.11-eclipse-temurin-25-noble AS builder
WORKDIR /build

COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY gateway/pom.xml gateway/pom.xml
COPY chain-ingest/pom.xml chain-ingest/pom.xml
COPY enrichment/pom.xml enrichment/pom.xml
COPY risk-ai/pom.xml risk-ai/pom.xml
COPY monitor/pom.xml monitor/pom.xml
COPY payment-watch/pom.xml payment-watch/pom.xml
RUN mvn -q -B -DskipTests dependency:go-offline || true

COPY common common
COPY gateway gateway
COPY chain-ingest chain-ingest
COPY enrichment enrichment
COPY risk-ai risk-ai
COPY monitor monitor
COPY payment-watch payment-watch

RUN mvn -q -B -DskipTests package

########################################
FROM eclipse-temurin:25-jre-noble AS gateway
WORKDIR /app
COPY --from=builder --chown=ubuntu:ubuntu /build/gateway/target/gateway-0.0.1-SNAPSHOT.jar app.jar
USER ubuntu
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

########################################
FROM eclipse-temurin:25-jre-noble AS chain-ingest
WORKDIR /app
COPY --from=builder --chown=ubuntu:ubuntu /build/chain-ingest/target/chain-ingest-0.0.1-SNAPSHOT.jar app.jar
USER ubuntu
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

########################################
FROM eclipse-temurin:25-jre-noble AS enrichment
WORKDIR /app
COPY --from=builder --chown=ubuntu:ubuntu /build/enrichment/target/enrichment-0.0.1-SNAPSHOT.jar app.jar
USER ubuntu
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

########################################
FROM eclipse-temurin:25-jre-noble AS risk-ai
WORKDIR /app
COPY --from=builder --chown=ubuntu:ubuntu /build/risk-ai/target/risk-ai-0.0.1-SNAPSHOT.jar app.jar
USER ubuntu
ENTRYPOINT ["java", "-XX:+UseParallelGC", "-jar", "/app/app.jar"]

########################################
FROM eclipse-temurin:25-jre-noble AS monitor
WORKDIR /app
COPY --from=builder --chown=ubuntu:ubuntu /build/monitor/target/monitor-0.0.1-SNAPSHOT.jar app.jar
USER ubuntu
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

########################################
FROM eclipse-temurin:25-jre-noble AS payment-watch
WORKDIR /app
COPY --from=builder --chown=ubuntu:ubuntu /build/payment-watch/target/payment-watch-0.0.1-SNAPSHOT.jar app.jar
USER ubuntu
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
