# One image that contains the React app and the Spring Boot API.
# Build:  docker build -t queueless .
# Run:    docker run -p 8080:8080 -e DB_URL=... -e DB_USERNAME=... -e DB_PASSWORD=... -e JWT_SECRET=... queueless

# ---- 1. Build the React front end ----
FROM node:20-alpine AS web
WORKDIR /web
COPY frontend/package*.json ./
RUN npm install
COPY frontend/ ./
RUN npm run build

# ---- 2. Build the Spring Boot jar (React build is copied in as static files) ----
FROM maven:3.9-eclipse-temurin-17 AS api
WORKDIR /api
COPY backend/pom.xml ./
RUN mvn -q -B dependency:go-offline
COPY backend/src ./src
COPY --from=web /web/dist ./src/main/resources/static
RUN mvn -q -B package -DskipTests

# ---- 3. Small runtime image ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=api /api/target/queueless.jar app.jar
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
