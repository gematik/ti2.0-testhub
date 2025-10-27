# FROM simsvc-base-jdk
FROM amazoncorretto:21-alpine-jdk

WORKDIR /app

EXPOSE 80

COPY target/zeta-pep-server-mockservice-*.jar /app/zeta-pep-server-mockservice.jar
#COPY target/classes/well-known.json /app/well-known.json

ENTRYPOINT ["java", "-jar", "/app/zeta-pep-server-mockservice.jar"]
