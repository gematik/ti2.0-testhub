#!/bin/bash

# stop on every error
set -e

cd test/vsdm-testsuite || exit 1

mvn verify -Dskip.inttests=false
