ARG DOCKER_REGISTRY
FROM $DOCKER_REGISTRY/ev/tomee:8

WORKDIR $CATALINA_HOME

COPY ./resources/server_*.xml ./conf/server.xml
COPY ./resources/tomee_*.xml ./conf/tomee.xml
COPY ./voting-workflow-main/target/vw-ws-rest.war ./webapps/vw-ws-rest.war

ENV TC_HTTP_PORT=8004 \
 DEBUG_PORT=6004 \
 APP_CONTEXT=vw-ws-rest

ENV CATALINA_OPTS -Dtc.http.port=${TC_HTTP_PORT} -Djava.security.egd=file:/dev/./urandom -Djava.awt.headless=true -server -XX:+UseParallelGC -Dch.post.it.environment=dev \
    -Dch.post.it.container=tomcat -Dch.post.it.config.dir=${CATALINA_HOME}/etc -Dch.post.it.logging.dir=${CATALINA_HOME}/logs \
    -Dch.post.it.logging.defaultSeverity=WARN -XX:+HeapDumpOnOutOfMemoryError \
    -Dtenantpasswordsdirectory=${CATALINA_HOME}/conf/password/tenant \
    -Donetimefiledirectory=${CATALINA_HOME}/conf/password '-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address='${DEBUG_PORT}

RUN chown evote:evote -R /data/appl

# REST TIME OUT PROPERTIES
ENV READ_TIME_OUT=60
ENV WRITE_TIME_OUT=60
ENV CONNECTION_TIME_OUT=60

# CRYPTO POOLS PROPERTIES
ENV ASYMMETRIC_MAX_ELEMENTS_CRYPTO_POOL=50
ENV PROOFS_MAX_ELEMENTS_CRYPTO_POOL=50
ENV PRIMITIVES_MAX_ELEMENTS_CRYPTO_POOL=50
ENV ELGAMAL_MAX_ELEMENTS_CRYPTO_POOL=50
ENV STORES_MAX_ELEMENTS_CRYPTO_POOL=50
ENV SYMMETRIC_MAX_ELEMENTS_CRYPTO_POOL=50

USER evote
ENV JAVA_OPTS="-Dorg.apache.johnzon.max-string-length=262144"

EXPOSE ${TC_HTTP_PORT} ${DEBUG_PORT}
CMD ./bin/catalina.sh run
HEALTHCHECK --timeout=3s --retries=4 CMD curl --noproxy localhost -fk http://localhost:${TC_HTTP_PORT}/${APP_CONTEXT}/check || exit 1
