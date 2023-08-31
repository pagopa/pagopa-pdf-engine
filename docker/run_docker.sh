#!/bin/bash

# sh ./run_docker.sh <local|dev|uat|prod>

ENV=$1

if [ -z "$ENV" ]
then
  ENV="local"
  echo "No environment specified: local is used."
fi

pip3 install yq

if [ "$ENV" = "local" ]; then
  image="service-local:latest"
  ENV="dev"
else
  repository=$(yq -r '."microservice-chart".image.repository' ../helm/values-$ENV.yaml)
  image="${repository}:latest"
fi
export image=${image}

FILE=.env
if test -f "$FILE"; then
    rm .env
fi
config=$(yq  -r '."microservice-chart".envConfig' ../helm/values-$ENV.yaml)
for line in $(echo $config | jq -r '. | to_entries[] | select(.key) | "\(.key)=\(.value)"'); do
    echo $line >> .env
done

keyvault=$(yq  -r '."microservice-chart".keyvault.name' ../helm/values-$ENV.yaml)
secret=$(yq  -r '."microservice-chart".envSecret' ../helm/values-$ENV.yaml)
for line in $(echo $secret | jq -r '. | to_entries[] | select(.key) | "\(.key)=\(.value)"'); do
  IFS='=' read -r -a array <<< "$line"
  response=$(az keyvault secret show --vault-name $keyvault --name "${array[1]}")
  value=$(echo $response | jq -r '.value')
  echo "${array[0]}=$value" >> .env
#  if [ "${array[0]}" = "AFM_SA_CONNECTION_STRING" ];then
#      echo "Set secret env ${array[0]}"
#      echo "::add-mask::$value"
#      echo AFM_SA_CONNECTION_STRING=$value >> $GITHUB_ENV
#  fi 
done


stack_name=$(cd .. && basename "$PWD")
docker build -t pagopa-pdf-engine ../java/
docker run -d -p 60486:80 --name="${stack_name}-java" pagopa-pdf-engine-java -e PDF_ENGINE_NODE_GEN_ENDPOINT="http://${stack_name}-node/api/pdf-generate" -e PDF_ENGINE_NODE_INFO_ENDPOINT="http://${stack_name}-node/api/info"
docker build -t pagopa-pdf-engine-node ../node/
docker run -d -p 60487:80 --name="${stack_name}-node" pagopa-pdf-engine-node

# waiting the containers
printf 'Waiting for the service'
attempt_counter=0
max_attempts=50
until [ $(curl -s -o /dev/null -w "%{http_code}" http://localhost:60486/info) -eq 200 ]; do
    if [ ${attempt_counter} -eq ${max_attempts} ];then
      echo "Max attempts reached"
      exit 1
    fi

    printf '.'
    attempt_counter=$((attempt_counter+1))
    sleep 5
done
echo 'Service Started'
