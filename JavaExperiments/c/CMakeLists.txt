cmake_minimum_required(VERSION 3.18)
project(JavaExperiments)

set(CMAKE_C_STANDARD 99)

# Replace this path with the path to the linux include directory (e.g. linux-5.18.9/include/).
include_directories(/Users/jacob/Public/linux-5.18.9/include)

# Include Java JNI headers
set(JAVA_INCLUDE_PATH "$ENV{JAVA_HOME}/include")
set(JAVA_AWT_LIBRARY NotNeeded)
set(JAVA_AWT_INCLUDE_PATH NotNeeded)
find_package(JNI REQUIRED)
include_directories(${JNI_INCLUDE_DIRS})

# Build shared library
add_library(JavaExperiments SHARED
        src/util/lang/lang.h
        src/util/i2c/i2c.c
        src/util/i2c/i2c.h
        src/jni/tech_anapad_rpicm4experiments_jni_JNIFunctions.h
        src/jni/tech_anapad_rpicm4experiments_jni_JNIFunctions.c)
