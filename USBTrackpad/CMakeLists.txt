cmake_minimum_required(VERSION 3.18)
project(USBTrackpad)

set(CMAKE_C_STANDARD 99)

# Replace this path with the path to the linux include directory (e.g. linux-5.18.9/include/).
include_directories(/Users/jacob/Public/linux-5.18.9/include)

add_executable(USBTrackpad src/main.c
        src/util/number/get_set_bits.h
        src/util/i2c/i2c.c
        src/util/i2c/i2c.h
        src/util/lang/lang.h
        src/util/file/file.c
        src/util/file/file.h
        src/util/number/number_util.h
        src/gt9110_config.c src/gt9110_config.h)

# Add math libraries to the linker
target_link_libraries(USBTrackpad PRIVATE m)
