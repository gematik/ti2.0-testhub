#!/usr/bin/env bash
set -euo pipefail

./mvnw clean install -Pdocker
doc/bin/docker-compose-local-restart.sh
