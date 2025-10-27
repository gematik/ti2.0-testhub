# FROM simsvc-base-jdk
FROM amazoncorretto:21-alpine-jdk

WORKDIR /app

EXPOSE 80

COPY target/zeta-pdp-server-mockservice-*.jar /app/zeta-pdp-server-mockservice.jar

ENTRYPOINT ["java", "-jar", "/app/zeta-pdp-server-mockservice.jar"]
