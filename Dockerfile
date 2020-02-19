FROM camunda/zeebe:latest

ARG EXPORTERJAR

COPY ${EXPORTERJAR} /usr/local/zeebe/lib/zeebe-hazelcast-exporter-jar-with-dependencies.jar
COPY zeebe.cfg.toml /usr/local/zeebe/conf/zeebe.cfg.toml
