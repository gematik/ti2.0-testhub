#!/usr/bin/env bash
set -euo pipefail

mvn clean install
doc/bin/vsdm/docker-build-all-local.sh
doc/bin/vsdm/docker-compose-local-restart.sh
