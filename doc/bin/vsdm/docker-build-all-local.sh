#!/bin/bash

# stop on every error
set -e

client/card-terminal-client-simservice/bin/docker-build.sh local
client/vsdm-client-simservice-java/bin/docker-build.sh local

server/zeta-pep-server-mockservice/bin/docker-build.sh local
server/zeta-pdp-server-mockservice/bin/docker-build.sh local

server/popp-server-mockservice/bin/docker-build.sh local
server/vsdm-server-simservice/bin/docker-build.sh local

