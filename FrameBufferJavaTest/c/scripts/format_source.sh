#!/bin/bash

DIRECTORY_TO_FORMAT="src/"

if [[ ! -d "$DIRECTORY_TO_FORMAT" ]]; then
    echo "This script must be called from the project directory (e.g. where the \"${DIRECTORY_TO_FORMAT}\" directory is)."
    exit 1
fi

find "$DIRECTORY_TO_FORMAT" -iname *.c -o -iname *.h | xargs clang-format -i --style=file
