# Dockerfile to package the Hazelcast exporter as an init container that can be deployed on top of
# an official Zeebe container, automatically configuring itself

FROM maven:3.6.0-jdk-11 as builder
ARG EXPORTER_ID=zeebe-hazelcast-exporter
ARG VERSION=0.8.0-SNAPSHOT

COPY ./ /usr/local/src/zeebe-hazelcast-exporter
WORKDIR /usr/local/src/zeebe-hazelcast-exporter

RUN mvn clean package -DskipTests

# Final image will only contain the built JAR and the configuration file and some UNIX utilities
FROM busybox:latest as exporter
ARG EXPORTER_ID=zeebe-hazelcast-exporter
ARG VERSION=0.8.0-SNAPSHOT
ARG EXPORTER_HOME=/usr/share/zeebe/${EXPORTER_ID}
ARG SHARED_DIR=/usr/share/zeebe/exporters

COPY --from=builder /usr/local/src/zeebe-hazelcast-exporter/exporter/target/zeebe-hazelcast-exporter-${VERSION}-jar-with-dependencies.jar ${EXPORTER_HOME}/${EXPORTER_ID}.jar
COPY docker/entrypoint.sh ${EXPORTER_HOME}/entrypoint.sh

ENV EXPORTER_ID ${EXPORTER_ID}
ENV EXPORTER_HOME ${EXPORTER_HOME}
ENV SHARED_DIR ${SHARED_DIR}
RUN mkdir -p "$EXPORTER_HOME"
WORKDIR ${EXPORTER_HOME}

# Zeebe containers should mount a real volume at /usr/share/zeebe/exporters; on start up, those containers should
# then append the configuration of the exporter to their own configuration
VOLUME ${SHARED_DIR}
ENTRYPOINT ["entrypoint.sh"]