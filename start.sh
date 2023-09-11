#!/bin/bash

nohup yarn --cwd /home/site/wwwroot/node run start &
./azure-functions-host/Microsoft.Azure.WebJobs.Script.WebHost

