FROM wurstmeister/kafka as runner

WORKDIR /container_libs/

COPY build/libs/kafka-connect-ftp-all.jar ./kafka-connect-ftp-all.jar

COPY build/resources/main/ ./
RUN chmod a+x ./start-kafka-and-ftp-connect.sh

# topic:partions:replication
ENV KAFKA_CREATE_TOPICS=test:3:1
ENV KAFKA_ADVERTISED_HOST_NAME=kafka-00
ENV KAFKA_ZOOKEEPER_CONNECT=zookeeper-00:2181

CMD sh ./start-kafka-and-ftp-connect.sh