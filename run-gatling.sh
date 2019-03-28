#!/usr/bin/env bash

set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"

$DIR/build.sh

docker run --rm=true --env JAVA_OPTS quay.io/bbrowning/demo2019-hard-shake-load-test
