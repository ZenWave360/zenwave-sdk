#!/bin/bash

DELAY=10

#docker-compose --file mongodb-cluster.yml down
#docker rm -f $(docker ps -a -q)
#docker volume rm $(docker volume ls -q)

docker-compose --file mongodb-cluster.yml up -d

echo "****** Waiting for ${DELAY} seconds for containers to go up ******"
sleep $DELAY

docker exec mongo1 /scripts/rs-init.sh
