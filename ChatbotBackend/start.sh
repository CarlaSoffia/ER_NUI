#!/bin/bash

# set default port number
PORT=${PORT:-5005}

rasa run actions &
rasa run -m models --enable-api --cors "*" --port $PORT