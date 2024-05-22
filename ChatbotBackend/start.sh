#!/bin/bash

# set default port number
PORT=${PORT:-5005}

rasa run actions --logging-config-file logging_config.yaml >/dev/null 2>&1 &
rasa run --logging-config-file logging_config.yaml -m models --enable-api --cors "*" --port $PORT >/dev/null 2>&1 &

# Commands when running locally:
 
# conda activate er_nui

# chmod +x ./start.sh

# ./start.sh

# conda deactivate

# sudo lsof -i :5005,5055

# kill -9 PID