# USB Trackpad
This project emulates a USB mouse/trackpad using the Linux USB HID Gadget API (via configfs) and the GT9110 touch controller chip (interfaces with via I2C in sysfs).

Execute the following command to generate the build configuration (only needs to be done once):
```shell
mkdir -p build/ && cmake -B build/ -S .
```

Execute the following command to build and run this project (needs to be done when source files change):
```shell
make -C build/ && sudo ./build/USBTrackpad
```
