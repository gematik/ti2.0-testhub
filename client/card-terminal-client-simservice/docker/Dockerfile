FROM amazoncorretto:21-alpine-jdk

WORKDIR /app

EXPOSE 80

COPY target/card-terminal-client-simservice-*.jar /app/card-terminal-client-simservice.jar

ENTRYPOINT ["java", "-jar", "/app/card-terminal-client-simservice.jar"]
