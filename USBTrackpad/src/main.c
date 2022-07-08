/**
 * @file main.c
 * @brief The entry point of the application.
 */

#include "util/file/file.h"
#include "util/i2c/i2c.h"
#include "util/lang/lang.h"
#include "util/number/get_set_bits.h"
#include <fcntl.h>
#include <signal.h>
#include <stdio.h>
#include <unistd.h>

#define I2C_GT9110_ADDRESS_SLAVE (uint16_t) 0x5D
#define I2C_GT9110_ADDRESS_REGISTER_RESOLUTION (uint16_t) 0x8146
#define I2C_GT9110_ADDRESS_REGISTER_STATUS (uint16_t) 0x814E
#define I2C_GT9110_ADDRESS_REGISTER_TOUCH_DATA (uint16_t) 0x814F

#define USB_GADGET_DEVICE_PATH "/dev/hidg0"
#define USB_GADGET_CONFIG_PATH "/sys/kernel/config/usb_gadget/"
#define USB_GADGET_NAME "trackpad/"
#define USB_GADGET_TRACKPAD_CONFIG_PATH USB_GADGET_CONFIG_PATH USB_GADGET_NAME
#define USB_GADGET_TRACKPAD_STRINGS_PATH USB_GADGET_TRACKPAD_CONFIG_PATH "strings/0x409/"
#define USB_GADGET_TRACKPAD_CONFIGURATION_PATH USB_GADGET_TRACKPAD_CONFIG_PATH "configs/c.1/"
#define USB_GADGET_TRACKPAD_CONFIGURATION_STRINGS_PATH USB_GADGET_TRACKPAD_CONFIGURATION_PATH "strings/0x409/"
#define USB_GADGET_TRACKPAD_FUNCTIONS_PATH USB_GADGET_TRACKPAD_CONFIG_PATH "functions/hid.0"
#define USB_DEVICE_CONTROLLER_NAME "49000000.usb-otg" // TODO
#define USB_GADGET_TRACKPAD_REPORT_LENGTH "3"

/**
 * The USB HID mouse/trackpad report struct.
 */
typedef struct {
    uint8_t buttons;
    int8_t x;
    int8_t y;
} usb_gadget_trackpad_report_t;

// From: https://eleccelerator.com/tutorial-about-usb-hid-report-descriptors/
static uint8_t usb_gadget_trackpad_report_descriptor[] = {
        0x05, 0x01, // USAGE_PAGE (Generic Desktop)
        0x09, 0x02, // USAGE (Mouse)
        0xa1, 0x01, // COLLECTION (Application)
        0x09, 0x01, //   USAGE (Pointer)
        0xa1, 0x00, //   COLLECTION (Physical)
        0x05, 0x09, //     USAGE_PAGE (Button)
        0x19, 0x01, //     USAGE_MINIMUM (Button 1)
        0x29, 0x03, //     USAGE_MAXIMUM (Button 3)
        0x15, 0x00, //     LOGICAL_MINIMUM (0)
        0x25, 0x01, //     LOGICAL_MAXIMUM (1)
        0x95, 0x03, //     REPORT_COUNT (3)
        0x75, 0x01, //     REPORT_SIZE (1)
        0x81, 0x02, //     INPUT (Data,Var,Abs)
        0x95, 0x01, //     REPORT_COUNT (1)
        0x75, 0x05, //     REPORT_SIZE (5)
        0x81, 0x03, //     INPUT (Cnst,Var,Abs)
        0x05, 0x01, //     USAGE_PAGE (Generic Desktop)
        0x09, 0x30, //     USAGE (X)
        0x09, 0x31, //     USAGE (Y)
        0x15, 0x81, //     LOGICAL_MINIMUM (-127)
        0x25, 0x7f, //     LOGICAL_MAXIMUM (127)
        0x75, 0x08, //     REPORT_SIZE (8)
        0x95, 0x02, //     REPORT_COUNT (2)
        0x81, 0x06, //     INPUT (Data,Var,Rel)
        0xc0, //         END_COLLECTION
        0xc0 //        END_COLLECTION
};
static size_t usb_hid_device_file_descriptor = -1;

static volatile sig_atomic_t run_loop = 1;

void signal_interrupt_handler(int32_t _) {
    printf("\nInterrupt signal received.\n");
    run_loop = 0;
}

int32_t usb_gadget_trackpad_create() {
    printf("Creating the USB HID mouse gadget...\n");
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
    make_file_with_string_contents(USB_GADGET_TRACKPAD_CONFIGURATION_PATH "MaxPower", "250"); // 250mA

    make_path(USB_GADGET_TRACKPAD_FUNCTIONS_PATH);
    make_file_with_string_contents(USB_GADGET_TRACKPAD_FUNCTIONS_PATH "protocol", "2"); // 1 = keyboard, 2 = mouse
    make_file_with_string_contents(USB_GADGET_TRACKPAD_FUNCTIONS_PATH "subclass", "1"); // 0 = no boot, 1 = boot
    make_file_with_string_contents(USB_GADGET_TRACKPAD_FUNCTIONS_PATH "report_length",
            USB_GADGET_TRACKPAD_REPORT_LENGTH);
    make_file_with_binary_contents(USB_GADGET_TRACKPAD_FUNCTIONS_PATH "report_desc",
            usb_gadget_trackpad_report_descriptor, SIZE_OF_ARRAY(usb_gadget_trackpad_report_descriptor));
    symlink(USB_GADGET_TRACKPAD_FUNCTIONS_PATH, USB_GADGET_TRACKPAD_CONFIGURATION_PATH);

    make_file_with_string_contents(USB_GADGET_TRACKPAD_CONFIG_PATH "UDC", USB_DEVICE_CONTROLLER_NAME);

    if ((usb_hid_device_file_descriptor = open(USB_GADGET_DEVICE_PATH, O_RDWR, 0666)) <= -1) {
        perror(USB_GADGET_DEVICE_PATH);
        return -1;
    }

    printf("Created the USB HID mouse gadget.\n");
    return 0;
}

int32_t usb_gadget_trackpad_remove() {
    if (remove_path(USB_GADGET_TRACKPAD_CONFIG_PATH)) {
        perror("");
        return -1;
    }
}

size_t usb_gadget_trackpad_write_report(usb_gadget_trackpad_report_t* usb_gadget_trackpad_report) {
    return write((int32_t) usb_hid_device_file_descriptor, usb_gadget_trackpad_report,
            sizeof(usb_gadget_trackpad_report_t));
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
    printf("Entering run loop...\n");
    uint8_t touchscreen_coordinate_data[8 * 10] = {0}; // 10 8-byte touch data
    usb_gadget_trackpad_report_t usb_gadget_trackpad_report;
    int16_t touchscreen_touch_last_x = -1;
    int16_t touchscreen_touch_last_y = -1;
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

        if (number_of_touches > 0) {
            uint8_t* first_touch_coordinate_data = touchscreen_coordinate_data;
            uint16_t x = (first_touch_coordinate_data[2] << 8) | first_touch_coordinate_data[1];
            uint16_t y = (first_touch_coordinate_data[4] << 8) | first_touch_coordinate_data[3];

            if (touchscreen_touch_last_x == -1 || touchscreen_touch_last_y == -1) {
                touchscreen_touch_last_x = (int16_t) x;
                touchscreen_touch_last_y = (int16_t) y;
            } else {
                int8_t delta_x = (int8_t) (x - touchscreen_touch_last_x);
                int8_t delta_y = (int8_t) (y - touchscreen_touch_last_y);

                usb_gadget_trackpad_report.buttons = 0;
                usb_gadget_trackpad_report.x = delta_x;
                usb_gadget_trackpad_report.y = delta_y;
                usb_gadget_trackpad_write_report(&usb_gadget_trackpad_report);
            }
        } else {
            touchscreen_touch_last_x = -1;
            touchscreen_touch_last_y = -1;

            usb_gadget_trackpad_report.buttons = 0;
            usb_gadget_trackpad_report.x = 0;
            usb_gadget_trackpad_report.y = 0;
            usb_gadget_trackpad_write_report(&usb_gadget_trackpad_report);
        }
    }
after_run_loop:

    close(i2c_dev_fd);
    printf("Closed I2C device.\n");

    usb_gadget_trackpad_remove();
    printf("Removed USB HID mouse gadget.\n");
    return 0;
}

int32_t main() {
    if (usb_gadget_trackpad_create()) {
        return -1;
    }
    if (trackpad_control_loop()) {
        return -1;
    }
}
