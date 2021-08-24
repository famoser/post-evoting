ARG DOCKER_REGISTRY
FROM $DOCKER_REGISTRY/ev/tomcat:8.5.69

USER root
COPY --chown=evote:evote ./target/config-generator.war ./webapps/config-generator.war

USER evote
CMD ./bin/catalina.sh run
HEALTHCHECK --timeout=3s --retries=4 CMD curl --noproxy localhost -fk http://localhost:${TC_HTTP_PORT} || exit 1
