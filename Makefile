.DEFAULT: docker

GET_VERSION = $(shell mvn org.apache.maven.plugins:maven-help-plugin:evaluate -Dexpression=project.version | grep -v "^\[")
SET_VERSION = $(eval VERSION=$(GET_VERSION))

# Eventually use the --squash flag when building (currently an experimental feature)
.PHONY: docker-build
docker-build:
	$(SET_VERSION)
	docker build -f docker/Dockerfile  --target exporter -t gcr.io/zeebe-io/zeebe-hazelcast-exporter:$(VERSION) .

.PHONY: docker-push
docker-push:
	$(SET_VERSION)
	docker push gcr.io/zeebe-io/zeebe-hazelcast-exporter:$(VERSION)

.PHONY: docker
docker: docker-build docker-push
