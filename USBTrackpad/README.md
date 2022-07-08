# USB Trackpad
This project emulates a USB mouse/trackpad using the Linux USB HID Gadget API (via configfs) and the GT9110 touch controller chip (interfaces with via I2C in sysfs).

Execute the following command to generate the build configuration on a Linux host (only needs to be done once):
```shell
mkdir -p build/ && cmake -B build/ -S .
```

Execute the following command to build and run this project on a Linux host (needs to be done when source files change):
```shell
make -C build/ && sudo ./build/USBTrackpad
```

Execute the following command to format the source code (requires `clang-format`):
```shell
./scripts/format_source.sh
```

Execute the following command to `rsync` this project with a remote Linux host:
```shell
./scripts/rsync_to_rpi_cm4.sh <remote_host_username> <remote_host_ip_address>
```
