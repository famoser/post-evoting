ARG DOCKER_REGISTRY
FROM $DOCKER_REGISTRY/ev/java:1.8

ENV WORKPATH /home/evote
ENV BUILD_RESOURCES=./resources

USER evote
COPY ${BUILD_RESOURCES}/ ${WORKPATH}
COPY ./ech-file-converter/target/ech-file-converter-*.jar ${WORKPATH}/file-converter.jar

WORKDIR ${WORKPATH}/importAndActivation

CMD /bin/bash
HEALTHCHECK --timeout=3s --retries=4 CMD /home/evote/healthcheck.sh || exit 1
