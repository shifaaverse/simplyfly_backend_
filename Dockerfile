FROM eclipse-temurin:21-jdk-alpine
COPY target/simplyfly_backend-0.0.1-SNAPSHOT.jar simplyfly-backend.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","simplyfly-backend.jar"]
