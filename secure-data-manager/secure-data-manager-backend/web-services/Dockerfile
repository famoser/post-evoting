ARG DOCKER_REGISTRY
FROM ev/config-tools:0.11.0 as config-tools
FROM $DOCKER_REGISTRY/ev/java:1.8

COPY ./resources/ /home/evote
COPY ./target/web-services.jar /home/evote/sdm-ws-rest.jar
COPY --from=config-tools /home/evote/file-converter.jar /home/evote/sdm/integration/file_converter_tool/

ENV HTTP_PORT=8090 \
 APP_CONTEXT=sdm-ws-rest

RUN chown -R evote:evote /home/evote/

WORKDIR /home/evote

USER evote
CMD java -jar sdm-ws-rest.jar
HEALTHCHECK --timeout=3s --retries=4 CMD curl --noproxy localhost -fk http://localhost:${HTTP_PORT}/${APP_CONTEXT}/status || exit 1
