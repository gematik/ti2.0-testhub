#!/bin/bash

if [ -z "$1" ]; then
  echo "Bitte den Image-Tag angeben"
  exit 1
fi

TAG=$1


ORIGINAL_DIR=$(pwd)

cd "$(dirname "$(readlink -f "$0")")" || exit 1
cd ..

docker build -f docker/Dockerfile -t popp-server-mockservice:"$TAG" .



cd "$ORIGINAL_DIR" || exit 1