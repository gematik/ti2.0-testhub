# FROM simsvc-base-jdk
FROM amazoncorretto:21-alpine-jdk

WORKDIR /app

EXPOSE 80

COPY target/vsdm-client-simservice-*.jar /app/vsdm-client-simservice.jar
COPY src/main/resources/tigerProxy.jks /app/tigerProxy.jks
ENTRYPOINT ["java", "-Djavax.net.ssl.trustStore=/app/tigerProxy.jks", "-Djavax.net.ssl.trustStorePassword=gematik", "-jar", "/app/vsdm-client-simservice.jar"]
