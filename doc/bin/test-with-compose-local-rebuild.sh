#!/bin/bash

# stop on every error
set -e

doc/bin/docker-compose-local-rebuild.sh
doc/bin/test-with-compose-local.sh
