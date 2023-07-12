# sh run_performance_test.sh <local|dev|uat|prod> <load|stress|spike|soak|...> <script-name> <db-name> <subkey> <generate-zipped> <template-file-name>

ENVIRONMENT=$1
TYPE=$2
SCRIPT=$3
DB_NAME=$4
SUBSCRIPTION_KEY=$5
GENERATE_ZIPPED=$6
TEMPLATE_FILE_NAME=$7

if [ -z "$ENVIRONMENT" ]
then
  echo "No env specified: sh run_performance_test.sh <local|dev|uat|prod> <load|stress|spike|soak|...> <script-name> <db-name> <subkey> <generate-zipped> <template-file-name>"
  exit 1
fi

if [ -z "$TYPE" ]
then
  echo "No test type specified: sh run_performance_test.sh <local|dev|uat|prod> <load|stress|spike|soak|...> <script-name> <db-name> <subkey> <generate-zipped> <template-file-name>"
  exit 1
fi
if [ -z "$SCRIPT" ]
then
  echo "No script name specified: sh run_performance_test.sh <local|dev|uat|prod> <load|stress|spike|soak|...> <script-name> <db-name> <subkey> <generate-zipped> <template-file-name>"
  exit 1
fi

export env=${ENVIRONMENT}
export type=${TYPE}
export script=${SCRIPT}
export db_name=${DB_NAME}
export sub_key=${SUBSCRIPTION_KEY}
export generate_zipped=${GENERATE_ZIPPED}
export template_file_name=${TEMPLATE_FILE_NAME}

stack_name=$(cd .. && basename "$PWD")
docker compose -p "${stack_name}-k6" up -d --remove-orphans --force-recreate --build
docker logs -f k6
docker stop nginx
