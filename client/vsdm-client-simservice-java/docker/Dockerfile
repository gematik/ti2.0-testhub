# FROM simsvc-base-jdk
FROM amazoncorretto:21-alpine-jdk

WORKDIR /app

EXPOSE 80

COPY target/vsdm-client-simservice-*.jar /app/vsdm-client-simservice.jar
ENTRYPOINT ["java", "-jar", "/app/vsdm-client-simservice.jar"]
