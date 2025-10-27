# FROM simsvc-base-jdk
FROM amazoncorretto:21-alpine-jdk

WORKDIR /app

EXPOSE 80

COPY target/classes/de  /app/de/
COPY target/vsdm-server-simservice-*.jar /app/vsdm-server-simservice.jar

ENTRYPOINT ["java", "-jar", "/app/vsdm-server-simservice.jar"]
