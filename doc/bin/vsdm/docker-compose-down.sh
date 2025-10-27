#!/bin/bash

cd doc/docker/vsdm || exit 1

docker compose -f compose-local.yaml down -v
