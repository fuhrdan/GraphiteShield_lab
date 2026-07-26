#!/usr/bin/env sh
set -eu
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$SCRIPT_DIR"
if ! command -v java >/dev/null 2>&1; then
    echo "Java 17 or newer is required."
    exit 1
fi
java -jar GraphiteShield-Lab.jar
