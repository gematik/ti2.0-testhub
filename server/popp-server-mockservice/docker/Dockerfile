# FROM simsvc-base-jdk
FROM amazoncorretto:21-alpine-jdk

WORKDIR /app

EXPOSE 80

COPY target/popp-server-mockservice-*.jar /app/popp-server-mockservice.jar

ENTRYPOINT ["java", "-jar", "/app/popp-server-mockservice.jar"]
