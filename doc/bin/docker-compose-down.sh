#!/bin/bash

cd doc/docker/backend || exit 1

docker compose -f compose-local.yaml down -v
