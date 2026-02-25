# FROM simsvc-base-jdk
FROM amazoncorretto:21-alpine-jdk

# Required so we can change the build context to include public-test-data/ from the
# repository root.
ARG MODULE_PATH=server/vsdm-server-simservice

WORKDIR /app

EXPOSE 80

COPY ./public-test-data/ /opt/public-test-data
COPY $MODULE_PATH/target/classes/de  /app/de/
COPY $MODULE_PATH/target/vsdm-server-simservice-*.jar /app/vsdm-server-simservice.jar

ENV VSDM_PATH_TO_TEST_DATA="/opt/public-test-data"

ENTRYPOINT ["java", "-jar", "/app/vsdm-server-simservice.jar"]
