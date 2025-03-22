#!/bin/bash

docker-compose -f sandbox/infrastructure/docker-compose.yml up -d
docker-compose -f sandbox/observability/docker-compose.yml up -d
docker-compose -f sandbox/application/docker-compose.yml up -d

