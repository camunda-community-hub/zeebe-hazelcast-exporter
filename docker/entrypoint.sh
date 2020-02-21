#!/bin/sh

EXPORTER_ID=${EXPORTER_ID:-"zeebe-hazelcast-exporter"}
if [[ ! -n "${EXPORTER_ID}" ]]; then
  echo "Expected environment variable EXPORTER_ID, but none defined"
  exit 1
fi

EXPORTER_HOME=${EXPORTER_HOME:-"/usr/share/zeebe/${EXPORTER_ID}"}
SHARED_DIR=${SHARED_DIR:-"/usr/share/zeebe/exporters"}
EXPORTER_JAR=${EXPORTER_JAR:-"${EXPORTER_HOME}/${EXPORTER_ID}.jar"}
SHARED_JAR=${SHARED_JAR:-"${SHARED_DIR}/${EXPORTER_ID}.jar"}
CONFIG_PATH=${CONFIG_PATH:-"${SHARED_DIR}/${EXPORTER_ID}.cfg.toml"}

if [[ ! -f "${EXPORTER_JAR}" ]]; then
  echo "Expected EXPORTER_JAR to point to an existing JAR, but no such file exists at ${EXPORTER_JAR}"
  exit 1
fi

if [[ ! -d "${SHARED_DIR}" ]]; then
  echo "Expected SHARED_DIR to point to an existing directory, but nothing found at ${SHARED_DIR}"
  exit 1
fi

ENABLED_VALUE_TYPES=${ENABLED_VALUE_TYPES:-"JOB,WORKFLOW_INSTANCE,DEPLOYMENT,INCIDENT,TIMER,VARIABLE,MESSAGE,MESSAGE_SUBSCRIPTION,MESSAGE_START_EVENT_SUBSCRIPTION"}
UPDATE_POSITION=${UPDATE_POSITION:-"false"}
cat << EOF >> ${CONFIG_PATH}
[[exporters]]
id = "${EXPORTER_ID}"
className = "io.zeebe.hazelcast.exporter.HazelcastExporter"
jarPath = "${SHARED_JAR}"

[exporters.args]
enabledValueTypes = "${ENABLED_VALUE_TYPES}"
updatePosition = ${UPDATE_POSITION}
EOF

cp ${EXPORTER_JAR} ${SHARED_JAR}
