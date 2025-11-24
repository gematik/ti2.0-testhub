#!/usr/bin/env bash
set -euo pipefail

mvn clean install -Pdocker
doc/bin/vsdm/docker-compose-local-restart.sh
