ARG DOCKER_REGISTRY
FROM $DOCKER_REGISTRY/ev/java:1.8

ENV TZ Europe/Zurich
ENV SPRING_RABBITMQ_HOST=message-broker
ENV SPRING_RABBITMQ_VIRTUAL-HOST=control-components
ENV SPRING_RABBITMQ_PORT=5671
ENV SPRING_RABBITMQ_SSL_ENABLED=true

COPY ./resources/healthcheck.sh /data/appl/
COPY ./target/distributed-mixing-service.jar /data/appl/distributed-mixing-service.jar

RUN chown -R evote:evote /data/appl/ && chmod u+x /data/appl/libs/* && chmod u+x /data/appl/healthcheck.sh

WORKDIR /data/appl
USER evote
CMD java -Dloader.path="libs" -jar distributed-mixing-service.jar
HEALTHCHECK --timeout=3s --retries=4 CMD /data/appl/healthcheck.sh || exit 1
