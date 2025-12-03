FROM maven:3.9.4-eclipse-temurin-17 AS builder
WORKDIR /workspace

# Copy only Maven configuration first to leverage Docker layer caching for dependencies
COPY pom.xml mvnw .mvn/ ./
RUN mvn -B -ntp dependency:go-offline

# Copy source and build
COPY src ./src
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:17-jre-jammy
ARG JAR_FILE=target/energif-0.0.1-SNAPSHOT.jar
COPY --from=builder /workspace/${JAR_FILE} /app/app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
