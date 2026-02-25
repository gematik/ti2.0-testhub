#!/bin/bash
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
ROOT_DIR="$SCRIPT_DIR/../.."

ROOT_DIR=$(pwd)

# stop on every error
set -e

cd "$ROOT_DIR"/test/vsdm-testsuite || exit 1

$ROOT_DIR/mvnw verify -Dskip.inttests=false
