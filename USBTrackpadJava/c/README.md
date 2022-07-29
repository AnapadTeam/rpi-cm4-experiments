# USB Trackpad C Project for using JNI
This project is for using the Java Native Interface (JNI) to interface low-level C code with Java code.

Execute the following command to generate the build configuration on a Linux host (only needs to be done once):
```shell
mkdir -p build/ && cmake -B build/ -S .
```
Note: make sure the `JAVA_HOME` environment variable is set accordingly.

Execute the following command to build this project on a Linux host and copy the shared library (needs to be done when source files change):
```shell
make -C build/ && sudo cp ./build/libUSBTrackpadJava.so /lib/
```

Execute the following command to format the source code (requires `clang-format`):
```shell
./scripts/format_source.sh
```

Execute the following command to `rsync` this project with a remote Linux host:
```shell
./scripts/rsync_to_rpi_cm4.sh <remote_host_username> <remote_host_ip_address>
```