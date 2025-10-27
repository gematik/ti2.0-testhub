#!/bin/bash

cd doc/docker/vsdm || exit 1

docker compose -f compose-local.yaml down -v
docker compose -f compose-local.yaml up -d --remove-orphans
