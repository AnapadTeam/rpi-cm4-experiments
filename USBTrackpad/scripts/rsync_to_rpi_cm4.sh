#!/bin/bash

DIRECTORY_TO_FORMAT="src/"

if [[ ! -d "$DIRECTORY_TO_FORMAT" ]]; then
    echo "This script must be called from the project directory (e.g. where the \"${DIRECTORY_TO_FORMAT}\" directory is)."
    exit 1
fi

if [[ $# -ne 2 ]]; then
    echo "You must supply arguments: <RPi username> <RPi IP address>"
    exit 1
fi

rsync -alPvz --delete --exclude "build" --exclude ".idea" . "${1}@${2}:~/USBTrackpad"
