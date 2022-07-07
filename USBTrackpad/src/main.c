/**
 * @file main.c
 * @brief The entry point of the application.
 */

#include "util/file/file.h"
#include "util/i2c/i2c.h"
#include "util/number/get_set_bits.h"
#include <fcntl.h>
#include <signal.h>
#include <stdio.h>
#include <unistd.h>

#define I2C_GT9110_ADDRESS_SLAVE (uint16_t) 0x5D
#define I2C_GT9110_ADDRESS_REGISTER_RESOLUTION (uint16_t) 0x8146
#define I2C_GT9110_ADDRESS_REGISTER_STATUS (uint16_t) 0x814E
#define I2C_GT9110_ADDRESS_REGISTER_TOUCH_DATA (uint16_t) 0x814F

#define USB_GADGET_CONFIG_PATH "/sys/kernel/config/usb_gadget/"
#define USB_GADGET_NAME "trackpad/"
#define USB_GADGET_TRACKPAD_CONFIG_PATH USB_GADGET_CONFIG_PATH USB_GADGET_NAME
#define USB_GADGET_TRACKPAD_STRINGS_PATH USB_GADGET_TRACKPAD_CONFIG_PATH "strings/0x409/"
#define USB_GADGET_TRACKPAD_CONFIGURATION_PATH USB_GADGET_TRACKPAD_CONFIG_PATH "configs/c.1/"
#define USB_GADGET_TRACKPAD_CONFIGURATION_STRINGS_PATH USB_GADGET_TRACKPAD_CONFIGURATION_PATH "strings/0x409/"

static volatile sig_atomic_t run_loop = 1;

void signal_interrupt_handler(int32_t _) {
    printf("\nInterrupt signal received.\n");
    run_loop = 0;
}

int32_t create_usb_hid_mouse_gadget() {
    if (make_path(USB_GADGET_TRACKPAD_CONFIG_PATH)) {
        perror("Could not create USB Gadget Trackpad configfs path");
        return -1;
    }

    make_file_with_string_contents(USB_GADGET_TRACKPAD_CONFIG_PATH "idVendor", "0x1d6b"); // Linux Foundation
    make_file_with_string_contents(USB_GADGET_TRACKPAD_CONFIG_PATH "idProduct",
            "0x0104"); // Multifunction Composite Gadget
    make_file_with_string_contents(USB_GADGET_TRACKPAD_CONFIG_PATH "bcdDevice", "0x0100"); // v1.0.0
    make_file_with_string_contents(USB_GADGET_TRACKPAD_CONFIG_PATH "bcdUSB", "0x0200"); // USB2

    make_path(USB_GADGET_TRACKPAD_STRINGS_PATH);
    make_file_with_string_contents(USB_GADGET_TRACKPAD_CONFIG_PATH "serialnumber", "a1b2c3d4e5");
    make_file_with_string_contents(USB_GADGET_TRACKPAD_CONFIG_PATH "manufacturer", "Anapad Team");
    make_file_with_string_contents(USB_GADGET_TRACKPAD_CONFIG_PATH "product", "Anapad");

    make_path(USB_GADGET_TRACKPAD_CONFIGURATION_STRINGS_PATH);
    make_file_with_string_contents(USB_GADGET_TRACKPAD_CONFIGURATION_STRINGS_PATH, "Anapad Configuration");
    make_file_with_string_contents(USB_GADGET_TRACKPAD_CONFIGURATION_PATH "MaxPower", "250");


    // TODO

    /*

    # Add functions here
    mkdir -p functions/hid.usb0
    echo 1 > functions/hid.usb0/protocol
    echo 1 > functions/hid.usb0/subclass
    echo 8 > functions/hid.usb0/report_length
    echo -ne <hid descriptor hex> > functions/hid.usb0/report_desc
    ln -s functions/hid.usb0 configs/c.1/ # End functions

    ls /sys/class/udc > UDC

    */
    return 0;
}

int32_t remove_usb_hid_mouse_gadget() {
    if (remove_path(USB_GADGET_TRACKPAD_CONFIG_PATH)) {
        perror("");
        return -1;
    }
}

int32_t trackpad_control_loop() {
    printf("Opening I2C device...\n");
    int32_t i2c_dev_fd = open("/dev/i2c-1", O_RDWR);
    if (i2c_dev_fd < 0) {
        perror("Failed to open I2C device");
        return 1;
    }
    printf("Opened I2C device.\n");

    signal(SIGINT, signal_interrupt_handler);

    printf("Reading screen resolution...\n");
    uint8_t touchscreen_resolution_data[4];
    if (i2c_read_register_bytes(i2c_dev_fd, I2C_GT9110_ADDRESS_SLAVE, I2C_GT9110_ADDRESS_REGISTER_RESOLUTION,
                touchscreen_resolution_data, sizeof(touchscreen_resolution_data))) {
        perror("Failed to read touchscreen resolution data");
        return 1;
    }
    uint16_t x_resolution = (touchscreen_resolution_data[1] << 8) | touchscreen_resolution_data[0];
    uint16_t y_resolution = (touchscreen_resolution_data[3] << 8) | touchscreen_resolution_data[2];
    printf("Screen resolution: %dx%d\n", x_resolution, y_resolution);

    // Continuously read touchscreen touch data
    uint8_t touchscreen_coordinate_data[8 * 10] = {0}; // 10 8-byte touch data
    while (run_loop) {
        // Wait until touchscreen data is ready to be read
        uint8_t coordinate_status_register = 0;
        uint8_t buffer_ready;
        do {
            if ((coordinate_status_register = i2c_read_register_byte(i2c_dev_fd, I2C_GT9110_ADDRESS_SLAVE,
                         I2C_GT9110_ADDRESS_REGISTER_STATUS)) < 0) {
                perror("Failed to read touchscreen resolution data");
                return 1;
            }
            buffer_ready = get_bit((uint32_t*) &coordinate_status_register, 7);

            if (!run_loop) {
                goto after_run_loop;
            }
        } while (!buffer_ready);

        // Reset buffer status to trigger another touchscreen sample
        i2c_write_register_byte(i2c_dev_fd, I2C_GT9110_ADDRESS_SLAVE, I2C_GT9110_ADDRESS_REGISTER_STATUS, 0);

        // Read touch data
        uint8_t number_of_touches = get_bits((uint32_t*) &coordinate_status_register, 3, 0);
        i2c_read_register_bytes(i2c_dev_fd, I2C_GT9110_ADDRESS_SLAVE, I2C_GT9110_ADDRESS_REGISTER_TOUCH_DATA,
                touchscreen_coordinate_data, sizeof(touchscreen_coordinate_data));
        for (uint8_t touch_index = 0; touch_index < number_of_touches; touch_index++) {
            uint8_t* touch_coordinate_data = touchscreen_coordinate_data + (touch_index * 8);
            uint8_t id = touch_coordinate_data[0];
            uint16_t x = (touch_coordinate_data[2] << 8) | touch_coordinate_data[1];
            uint16_t y = (touch_coordinate_data[4] << 8) | touch_coordinate_data[3];
            uint16_t size = (touch_coordinate_data[6] << 8) | touch_coordinate_data[5];

            // TODO write HID data to sysfs USB gadget
        }
    }
after_run_loop:

    close(i2c_dev_fd);
    printf("Closed I2C device.\n");

    remove_usb_hid_mouse_gadget();
    return 0;
}

int32_t main() {
    if (create_usb_hid_mouse_gadget()) {
        return -1;
    }
    if (trackpad_control_loop()) {
        return -1;
    }
}
