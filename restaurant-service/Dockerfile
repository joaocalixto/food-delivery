# Build context = raiz do monorepo (food-delivery/)
# Build:  docker build -f restaurant-service/Dockerfile -t restaurant-service .

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn -q -pl restaurant-service -am clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/restaurant-service/target/restaurant-service-*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
