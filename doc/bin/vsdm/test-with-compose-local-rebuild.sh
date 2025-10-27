#!/bin/bash

# stop on every error
set -e

doc/bin/vsdm/docker-compose-local-rebuild.sh
doc/bin/vsdm/test-with-compose-local.sh
