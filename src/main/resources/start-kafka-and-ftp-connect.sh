#!/bin/bash

echo "DEBUG"

# Script from
# https://docs.docker.com/config/containers/multi-service_container/

# Turn on bash's job control
set -m

# Start the primary process and put it in the background
# This script is in wurstmeister/kafka image
# https://github.com/wurstmeister/kafka-docker/blob/master/start-kafka.sh
start-kafka.sh &

# Wait until Kafka is up
sleep 60s

# Start
connect-standalone.sh worker.properties ftp-client.properties

# now we bring the primary process back into the foreground
# and leave it there
fg %1