# Single shared build for every module. Pass -build-arg MODULE=<module-name>.
# The build is identical for each service (Maven multi-module, same JDK), so
# duplicating a Dockerfile per service would just be maintenance overhead.
FROM eclipse-temurin:21-jdk AS build
ARG MODULE
WORKDIR /app
COPY . .
# -pl <module> -am builds the module and any local modules it depends on.
RUN ./mvnw -q -pl ${MODULE} -am clean package -DskipTests

FROM eclipse-temurin:21-jre
ARG MODULE
WORKDIR /app
COPY --from=build /app/${MODULE}/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
