#!/bin/bash

# sh ./run_docker.sh

stack_name=$(cd .. && basename "$PWD")
docker build -t pagopa-pdf-engine-node ../node
docker build -t pagopa-pdf-engine ../
docker run -d -p 3000:3000 --name="${stack_name}-node" --network=host pagopa-pdf-engine-node
docker run -d -p 60486:80 --name="${stack_name}" --network=host pagopa-pdf-engine

# waiting the containers
printf 'Waiting for the service'
attempt_counter=0
max_attempts=50
until [ $(curl -s -o /dev/null -w "%{http_code}" http://localhost/info) -eq 200 ]; do
    if [ ${attempt_counter} -eq ${max_attempts} ];then
      echo "Max attempts reached"
      exit 1
    fi

    printf '.'
    attempt_counter=$((attempt_counter+1))
    sleep 5
done
echo 'Service Started'
