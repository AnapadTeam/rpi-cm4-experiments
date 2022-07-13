#!/bin/bash

SOURCE_DIRECTORY="src/"

if [[ ! -d "$SOURCE_DIRECTORY" ]]; then
    echo "This script must be called from the project directory (e.g. where the \"${SOURCE_DIRECTORY}\" directory is)."
    exit 1
fi

if [[ $# -ne 2 ]]; then
    echo "You must supply arguments: <RPi username> <RPi IP address>"
    exit 1
fi

rsync -alPvz --delete --exclude "build/" --exclude ".idea/" --exclude ".gradle/" . "${1}@${2}:~/LRAHapticsJavaTest"
