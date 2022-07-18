#!/bin/bash

JAVA_GRADLE_PROJECT_DIRECTORY="../java/"
TARGET_JAVA_SOURCE_FILE_TO_GENERATE_HEADERS="${JAVA_GRADLE_PROJECT_DIRECTORY}/src/main/java/tech/anapad/rpicm4experiments/jni/JNIFunctions.java"
TARGET_C_HEADER_FILE="src/jni/"

SOURCE_DIRECTORY="src/"
if [[ ! -d "$SOURCE_DIRECTORY" ]]; then
    echo "This script must be called from the project directory (e.g. where the \"${SOURCE_DIRECTORY}\" directory is)."
    exit 1
fi

javac "${TARGET_JAVA_SOURCE_FILE_TO_GENERATE_HEADERS}" -h "${TARGET_C_HEADER_FILE}"
