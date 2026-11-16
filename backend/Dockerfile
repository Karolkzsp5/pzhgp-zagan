# Etap 1: budowanie pliku JAR (Maven + JDK 25).
# Testy są pomijane, bo wymagają osobnej bazy testowej; uruchamia się je poza Dockerem (mvnw test).
FROM maven:3-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests

# Etap 2: uruchomienie na samym środowisku uruchomieniowym Javy (bez Mavena i kompilatora)
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app app
COPY --from=build /app/target/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
