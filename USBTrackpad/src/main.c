/**
 * @file main.c
 * @brief The entry point of the application.
 */

#include "gt9110_config.h"
#include "util/file/file.h"
#include "util/i2c/i2c.h"
#include "util/lang/lang.h"
#include "util/number/get_set_bits.h"
#include "util/number/number_util.h"
#include <fcntl.h>
#include <math.h>
#include <signal.h>
#include <stdio.h>
#include <unistd.h>

#define I2C_GT9110_ADDRESS_SLAVE (uint16_t) 0x5D
#define I2C_GT9110_ADDRESS_REGISTER_RESOLUTION (uint16_t) 0x8146
#define I2C_GT9110_ADDRESS_REGISTER_STATUS (uint16_t) 0x814E
#define I2C_GT9110_ADDRESS_REGISTER_TOUCH_DATA (uint16_t) 0x814F

#define USB_GADGET_DEVICE_PATH "/dev/hidg0"
#define USB_GADGET_KERNEL_CONFIG_PATH "/sys/kernel/config/usb_gadget/"
#define USB_GADGET_NAME "trackpad/"
#define USB_GADGET_TRACKPAD_PATH USB_GADGET_KERNEL_CONFIG_PATH USB_GADGET_NAME
#define USB_GADGET_TRACKPAD_STRINGS_PATH USB_GADGET_TRACKPAD_PATH "strings/0x409/"
#define USB_GADGET_TRACKPAD_CONFIGURATION_PATH USB_GADGET_TRACKPAD_PATH "configs/c.1/"
#define USB_GADGET_TRACKPAD_CONFIGURATION_STRINGS_PATH USB_GADGET_TRACKPAD_CONFIGURATION_PATH "strings/0x409/"
#define USB_GADGET_TRACKPAD_FUNCTIONS_NAME "hid.0"
#define USB_GADGET_TRACKPAD_FUNCTIONS_PATH USB_GADGET_TRACKPAD_PATH "functions/" USB_GADGET_TRACKPAD_FUNCTIONS_NAME "/"
#define USB_DEVICE_CONTROLLER_NAME "fe980000.usb" // This is the UDC specifically for the RPi CM4

/**
 * The USB HID mouse/trackpad report struct.
 */
typedef struct {
    uint8_t buttons;
    int8_t x;
    int8_t y;
    int8_t wheel;
} usb_gadget_trackpad_report_t;

// Created using https://eleccelerator.com/usbdescreqparser/ and https://shorturl.at/hU034 (removed wake feature though)
static uint8_t usb_gadget_trackpad_report_descriptor[] = {
        0x05, 0x01, // Usage Page (Generic Desktop)
        0x09, 0x02, // Usage (Mouse)
        0xa1, 0x01, // Collection (Application)
        0x09, 0x01, //   Usage (Pointer)
        0xa1, 0x00, //   Collection (Physical)
        0x05, 0x09, //     Usage Page (Button)
        0x19, 0x01, //     Usage Minimum (0x01)
        0x29, 0x03, //     Usage Maximum (0x03)
        0x15, 0x00, //     Logical Minimum (0)
        0x25, 0x01, //     Logical Maximum (1)
        0x95, 0x03, //     Report Count (3)
        0x75, 0x01, //     Report Size (1)
        0x81, 0x02, //     Input (Data,Var,Abs)
        0x95, 0x01, //     Report Count (1)
        0x75, 0x05, //     Report Size (5)
        0x81, 0x03, //     Input (Const,Array,Abs)
        0x05, 0x01, //     Usage Page (Generic Desktop)
        0x09, 0x30, //     Usage (X)
        0x09, 0x31, //     Usage (Y)
        0x09, 0x38, //     Usage (Wheel)
        0x15, 0x81, //     Logical Minimum (-127)
        0x25, 0x7f, //     Logical Maximum (127)
        0x75, 0x08, //     Report Size (8)
        0x95, 0x03, //     Report Count (3)
        0x81, 0x06, //     Input (Data,Var,Rel)
        0xc0, //         End Collection
        0xc0 //        End Collection
};
static int32_t usb_hid_device_file = -1;

static volatile sig_atomic_t run_loop = 1;

void signal_interrupt_handler(int32_t _) {
    printf("\nInterrupt signal received.\n");
    run_loop = 0;
}

int32_t usb_gadget_trackpad_create() {
    printf("Creating the USB HID mouse gadget...\n");

    CHECK(create_directory_path(USB_GADGET_TRACKPAD_PATH));
    CHECK(write_file_with_string_contents(USB_GADGET_TRACKPAD_PATH "idVendor", "0x1d6b")); // Linux Foundation
    CHECK(write_file_with_string_contents(USB_GADGET_TRACKPAD_PATH "idProduct",
            "0x0104")); // Multifunction Composite Gadget
    CHECK(write_file_with_string_contents(USB_GADGET_TRACKPAD_PATH "bcdDevice", "0x0100")); // v1.0.0
    CHECK(write_file_with_string_contents(USB_GADGET_TRACKPAD_PATH "bcdUSB", "0x0200")); // USB2
    CHECK(write_file_with_string_contents(USB_GADGET_TRACKPAD_PATH "max_speed", "full-speed")); // USB Full-Speed 12Mb/s

    CHECK(create_directory_path(USB_GADGET_TRACKPAD_STRINGS_PATH));
    CHECK(write_file_with_string_contents(USB_GADGET_TRACKPAD_STRINGS_PATH "serialnumber", "a1b2c3d4e5"));
    CHECK(write_file_with_string_contents(USB_GADGET_TRACKPAD_STRINGS_PATH "manufacturer", "Anapad Team"));
    CHECK(write_file_with_string_contents(USB_GADGET_TRACKPAD_STRINGS_PATH "product", "Anapad"));

    CHECK(create_directory_path(USB_GADGET_TRACKPAD_CONFIGURATION_STRINGS_PATH));
    CHECK(write_file_with_string_contents(USB_GADGET_TRACKPAD_CONFIGURATION_STRINGS_PATH "configuration",
            "Anapad Config"));
    CHECK(write_file_with_string_contents(USB_GADGET_TRACKPAD_CONFIGURATION_PATH "MaxPower", "250")); // 250mA

    CHECK(create_directory_path(USB_GADGET_TRACKPAD_FUNCTIONS_PATH));
    CHECK(write_file_with_string_contents(USB_GADGET_TRACKPAD_FUNCTIONS_PATH "protocol", "2")); // 1=keyboard, 2=mouse
    CHECK(write_file_with_string_contents(USB_GADGET_TRACKPAD_FUNCTIONS_PATH "subclass", "1")); // 0 = no boot, 1 = boot
    CHECK(write_file_with_string_contents(USB_GADGET_TRACKPAD_FUNCTIONS_PATH "report_length", "4"));
    CHECK(write_file_with_binary_contents(USB_GADGET_TRACKPAD_FUNCTIONS_PATH "report_desc",
            usb_gadget_trackpad_report_descriptor, SIZE_OF_ARRAY(usb_gadget_trackpad_report_descriptor)));
    if (symlink(USB_GADGET_TRACKPAD_FUNCTIONS_PATH,
                USB_GADGET_TRACKPAD_CONFIGURATION_PATH USB_GADGET_TRACKPAD_FUNCTIONS_NAME)) {
        perror("Could not create symlink " USB_GADGET_TRACKPAD_CONFIGURATION_PATH USB_GADGET_TRACKPAD_FUNCTIONS_NAME
               " -> " USB_GADGET_TRACKPAD_FUNCTIONS_PATH);
        return errno;
    }

    // Enable the gadget
    CHECK(write_file_with_string_contents(USB_GADGET_TRACKPAD_PATH "UDC", USB_DEVICE_CONTROLLER_NAME));

    if ((usb_hid_device_file = open(USB_GADGET_DEVICE_PATH, O_RDWR, 0666)) == -1) {
        perror(USB_GADGET_DEVICE_PATH);
        return -1;
    }

    printf("Created the USB HID mouse gadget.\n");
    return 0;
}

int32_t usb_gadget_trackpad_remove() {
    // None of the following calls need error checking because unsuccessful calls can be ignored here.

    // Disable the gadget
    write_file_with_string_contents(USB_GADGET_TRACKPAD_PATH "UDC", "");

    // Remove directories/files to clean up as needed
    remove_file(USB_GADGET_TRACKPAD_CONFIGURATION_PATH USB_GADGET_TRACKPAD_FUNCTIONS_NAME);
    remove_directory(USB_GADGET_TRACKPAD_CONFIGURATION_STRINGS_PATH);
    remove_directory(USB_GADGET_TRACKPAD_CONFIGURATION_PATH);
    remove_directory(USB_GADGET_TRACKPAD_FUNCTIONS_PATH);
    remove_directory(USB_GADGET_TRACKPAD_STRINGS_PATH);
    remove_directory(USB_GADGET_TRACKPAD_PATH);
}

size_t usb_gadget_trackpad_write_report(usb_gadget_trackpad_report_t* usb_gadget_trackpad_report) {
    return write(usb_hid_device_file, usb_gadget_trackpad_report, sizeof(usb_gadget_trackpad_report_t));
}

int32_t write_gt9110_configuration() {
    printf("Opening I2C device...\n");
    int32_t i2c_dev_fd = open("/dev/i2c-5", O_RDWR);
    if (i2c_dev_fd < 0) {
        perror("Failed to open I2C device");
        return 1;
    }
    printf("Opened I2C device.\n");

    uint32_t code = write_configuration_data(i2c_dev_fd, I2C_GT9110_ADDRESS_SLAVE, gt9110_config);
    if (code) {
        printf("Could not write GT9110 configuration!\n");
    } else {
        printf("Wrote GT9110 configuration.\n");
    }

    close(i2c_dev_fd);
    printf("Closed I2C device.\n");
}

int32_t trackpad_control_loop() {
    printf("Opening I2C device...\n");
    int32_t i2c_dev_fd = open("/dev/i2c-5", O_RDWR);
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
    printf("Entering run loop...\n");
    uint8_t touchscreen_coordinate_data[8 * 10] = {0}; // 10 8-byte touch data
    usb_gadget_trackpad_report_t usb_gadget_trackpad_report;
    int16_t touchscreen_touch_last_x = -1;
    int16_t touchscreen_touch_last_y = -1;
    uint8_t touchscreen_touch_down_delta_non_zero = 0;
    uint8_t touchscreen_multi_touched_down = 0;
    uint8_t touchscreen_last_touch_count = 0;
    uint32_t touchscreen_wheel_move_count = 0;
    const uint32_t wheel_move_count_increment_threshold = 110;
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

        if (!touchscreen_multi_touched_down) {
            touchscreen_multi_touched_down = number_of_touches >= 2;
        }

        if (number_of_touches > 0) {
            uint8_t* first_touch_coordinate_data = touchscreen_coordinate_data;
            uint16_t x = (first_touch_coordinate_data[2] << 8) | first_touch_coordinate_data[1];
            uint16_t y = (first_touch_coordinate_data[4] << 8) | first_touch_coordinate_data[3];
            printf("Touchscreen touch 0: x=%d y=%d\n", x, y);

            if (touchscreen_touch_last_x == -1 || touchscreen_touch_last_y == -1) {
                // Set last touch coordinates to current touch coordinates
                touchscreen_touch_last_x = (int16_t) x;
                touchscreen_touch_last_y = (int16_t) y;
            } else {
                // Calculate deltas
                int32_t delta_x = x - touchscreen_touch_last_x;
                int32_t delta_y = y - touchscreen_touch_last_y;
                int32_t multiplied_delta_x = (int32_t) round((double) delta_x / 5);
                int32_t multiplied_delta_y = (int32_t) round((double) delta_y / 5);

                // Clamp deltas
                delta_x = CLAMP(delta_x, INT8_MIN, INT8_MAX);
                delta_y = CLAMP(delta_y, INT8_MIN, INT8_MAX);
                multiplied_delta_x = CLAMP(multiplied_delta_x, INT8_MIN, INT8_MAX);
                multiplied_delta_y = CLAMP(multiplied_delta_y, INT8_MIN, INT8_MAX);

                // Write "trackpad" report
                if (touchscreen_multi_touched_down) {
                    usb_gadget_trackpad_report.buttons = 0;
                    usb_gadget_trackpad_report.x = 0;
                    usb_gadget_trackpad_report.y = 0;

                    if ((touchscreen_wheel_move_count += ABS(delta_y)) >= wheel_move_count_increment_threshold) {
                        touchscreen_wheel_move_count = 0;
                        usb_gadget_trackpad_report.wheel = (int8_t) -CLAMP(delta_y, -1, 1);
                    } else {
                        usb_gadget_trackpad_report.wheel = 0;
                    }
                } else {
                    usb_gadget_trackpad_report.buttons = 0;
                    usb_gadget_trackpad_report.x = (int8_t) multiplied_delta_x;
                    usb_gadget_trackpad_report.y = (int8_t) multiplied_delta_y;
                    usb_gadget_trackpad_report.wheel = 0;
                }
                usb_gadget_trackpad_write_report(&usb_gadget_trackpad_report);

                // Set last touch coordinates to current touch coordinates
                if (multiplied_delta_x != 0) {
                    touchscreen_touch_last_x = (int16_t) x;
                }
                if (multiplied_delta_y != 0) {
                    touchscreen_touch_last_y = (int16_t) y;
                }

                // Trigger non-zero delta touch as needed
                if (!touchscreen_touch_down_delta_non_zero && delta_x != 0 && delta_y != 0) {
                    touchscreen_touch_down_delta_non_zero = 1;
                }

                printf("Touchpad touch data sent: buttons=%x x=%d y=%d wheel=%d\n", usb_gadget_trackpad_report.buttons,
                        usb_gadget_trackpad_report.x, usb_gadget_trackpad_report.y, usb_gadget_trackpad_report.wheel);
            }
        } else {
            // Reset report
            usb_gadget_trackpad_report.x = 0;
            usb_gadget_trackpad_report.y = 0;
            usb_gadget_trackpad_report.wheel = 0;

            // Handle non-zero delta touch down/up (aka button clicks)
            if (touchscreen_touch_down_delta_non_zero) {
                touchscreen_touch_down_delta_non_zero = 0;
                usb_gadget_trackpad_report.buttons = 0;
            } else if (touchscreen_last_touch_count != 0) {
                if (touchscreen_multi_touched_down) {
                    usb_gadget_trackpad_report.buttons = 0x01 << 1;
                    printf("Touchpad right-click sent.\n");
                } else {
                    usb_gadget_trackpad_report.buttons = 0x01;
                    printf("Touchpad left-click sent.\n");
                }
                usb_gadget_trackpad_write_report(&usb_gadget_trackpad_report);
                usb_gadget_trackpad_report.buttons = 0;
            }

            // Reset variables
            touchscreen_touch_last_x = -1;
            touchscreen_touch_last_y = -1;
            touchscreen_multi_touched_down = 0;

            usb_gadget_trackpad_write_report(&usb_gadget_trackpad_report);
        }

        touchscreen_last_touch_count = number_of_touches;
    }
after_run_loop:

    close(i2c_dev_fd);
    printf("Closed I2C device.\n");
    return 0;
}

int32_t main() {
     write_gt9110_configuration();

//    if (usb_gadget_trackpad_create()) {
//        usb_gadget_trackpad_remove();
//        return -1;
//    }
//
//    if (trackpad_control_loop()) {
//        return -1;
//    }
//
//    usb_gadget_trackpad_remove();
    printf("Removed USB HID mouse gadget.\n");
}
