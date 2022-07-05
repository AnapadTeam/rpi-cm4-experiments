#include <stdio.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/ioctl.h>
#include <fcntl.h>
#include <unistd.h>
#include <inttypes.h>
#include <linux/i2c.h>
#include <linux/i2c-dev.h>

#include "get_set_bits.h"

#define SIZE_OF_ARRAY(x)  (sizeof(x) / sizeof((x)[0]))

#define I2C_GT9110_address (uint16_t)0x5D

int32_t i2c_write_register_byte(uint32_t i2c_dev_fd, uint16_t slave_address, uint16_t register_address, uint8_t register_data) {
    uint8_t register_byte_write_data[3] = {(register_address >> 8) & 0xFF, register_address & 0xFF, register_data};

    struct i2c_msg i2c_msg;
    i2c_msg.addr = slave_address;
    i2c_msg.flags = 0; // Write flag
    i2c_msg.len = SIZE_OF_ARRAY(register_byte_write_data);
    i2c_msg.buf = register_byte_write_data;

    struct i2c_rdwr_ioctl_data i2c_transfer;
    i2c_transfer.msgs = &i2c_msg;
    i2c_transfer.nmsgs = 1;

    if (ioctl(i2c_dev_fd, I2C_RDWR, &i2c_transfer) < 0) {
        perror("i2c r/w ioctl");
    return -1;
    }

    return 0;
}

int32_t i2c_write_register_bytes(uint32_t i2c_dev_fd, uint16_t slave_address, uint16_t register_address, uint8_t *register_data,
        uint16_t register_data_length) {
    uint8_t register_address_data[2] = {(register_address >> 8) & 0xFF, register_address & 0xFF};
    uint8_t register_write_data[sizeof(register_address_data) + register_data_length];
    memcpy(register_write_data, register_address_data, sizeof(register_address_data));
    memcpy(register_write_data + sizeof(register_address_data), register_data, register_data_length);

    struct i2c_msg i2c_msg;
    i2c_msg.addr = slave_address;
    i2c_msg.flags = 0; // Write flag
    i2c_msg.len = SIZE_OF_ARRAY(register_write_data);
    i2c_msg.buf = register_write_data;

    struct i2c_rdwr_ioctl_data i2c_transfer;
    i2c_transfer.msgs = &i2c_msg;
    i2c_transfer.nmsgs = 1;

    if (ioctl(i2c_dev_fd, I2C_RDWR, &i2c_transfer) < 0) {
        perror("i2c r/w ioctl");
    return -1;
    }

    return 0;
}

int32_t i2c_read_register_byte(uint32_t i2c_dev_fd, uint16_t slave_address, uint16_t register_address) {
    uint8_t register_byte = 0;
    uint8_t register_address_data[2] = {(register_address >> 8) & 0xFF, register_address & 0xFF};

    struct i2c_msg i2c_msgs[2];

    i2c_msgs[0].addr = slave_address;
    i2c_msgs[0].flags = 0; // Write flag
    i2c_msgs[0].len = SIZE_OF_ARRAY(register_address_data);
    i2c_msgs[0].buf = register_address_data;

    i2c_msgs[1].addr = slave_address;
    i2c_msgs[1].flags = I2C_M_RD; // Read flag
    i2c_msgs[1].len = 1;
    i2c_msgs[1].buf = &register_byte;

    struct i2c_rdwr_ioctl_data i2c_transfer;
    i2c_transfer.msgs = i2c_msgs;
    i2c_transfer.nmsgs = SIZE_OF_ARRAY(i2c_msgs);

    if (ioctl(i2c_dev_fd, I2C_RDWR, &i2c_transfer) < 0) {
        perror("i2c r/w ioctl");
    return -1;
    }

    return register_byte;
}

uint32_t i2c_read_register_bytes(uint32_t i2c_dev_fd, uint16_t slave_address, uint16_t register_address, uint8_t *register_data, uint16_t register_data_length) {
    uint8_t register_address_data[2] = {(register_address >> 8) & 0xFF, register_address & 0xFF};

    struct i2c_msg i2c_msgs[2];

    i2c_msgs[0].addr = slave_address;
    i2c_msgs[0].flags = 0; // Write flag
    i2c_msgs[0].len = SIZE_OF_ARRAY(register_address_data);
    i2c_msgs[0].buf = register_address_data;

    i2c_msgs[1].addr = slave_address;
    i2c_msgs[1].flags = I2C_M_RD; // Read flag
    i2c_msgs[1].len = register_data_length;
    i2c_msgs[1].buf = register_data;

    struct i2c_rdwr_ioctl_data i2c_transfer;
    i2c_transfer.msgs = i2c_msgs;
    i2c_transfer.nmsgs = sizeof(i2c_msgs) / sizeof(i2c_msgs[0]);

    if (ioctl(i2c_dev_fd, I2C_RDWR, &i2c_transfer) < 0) {
        perror("i2c r/w ioctl");
    return -1;
    }

    return 0;
}


void serialize_touchscreen_coordinate_data(uint8_t number_of_touches, uint8_t *touchscreen_coordinate_data) {
    printf("t,");
    for (uint8_t touch_index = 0; touch_index < 10; touch_index++) {
        uint8_t *touch_coordinate_data = touchscreen_coordinate_data + (touch_index * 8);
        uint8_t id = touch_coordinate_data[0];
        uint16_t x = (touch_coordinate_data[2] << 8) | touch_coordinate_data[1];
        uint16_t y = (touch_coordinate_data[4] << 8) | touch_coordinate_data[3];
        uint16_t size = (touch_coordinate_data[6] << 8) | touch_coordinate_data[5];

        // Zero out touch data when the touch index is greater than the number of touches
        if (touch_index + 1 > number_of_touches) {
            x = 0;
            y = 0;
            size = 0;
        }

        char formatted_touch_data[100];
        snprintf(formatted_touch_data, sizeof(formatted_touch_data), "%d,%d,%d,%d,%d%s", touch_index, id, x, y, size,
                 touch_index == 9 ? "" : ",");
        printf("%s", formatted_touch_data);
    }
    printf("\n");
}

void gt9110_poll_and_serialize_touchscreen_data(uint32_t i2c_dev_fd) {
    // Send touchscreen configuration (e.g. the X/Y resolution)
    uint8_t touchscreen_resolution_data[4];
    i2c_read_register_bytes(i2c_dev_fd, I2C_GT9110_address, 0x8146, touchscreen_resolution_data,
                            sizeof(touchscreen_resolution_data));
    uint16_t x_resolution = (touchscreen_resolution_data[1] << 8) | touchscreen_resolution_data[0];
    uint16_t y_resolution = (touchscreen_resolution_data[3] << 8) | touchscreen_resolution_data[2];
    char formatted_ts_config_data[50];
    snprintf(formatted_ts_config_data, sizeof(formatted_ts_config_data), "c,%d,%d", x_resolution, y_resolution);
    printf("%s\n", formatted_ts_config_data);

    // Continuously send touchscreen touch data
    uint8_t touchscreen_coordinate_data[8 * 10] = {0}; // 10 8-byte touch data
    while (1) {
        // Wait until touchscreen data is ready to be read
        uint8_t coordinate_status_register = 0;
        uint8_t buffer_ready = 0;
        do {
            coordinate_status_register = i2c_read_register_byte(i2c_dev_fd, I2C_GT9110_address, 0x814E);
            buffer_ready = get_bit((uint32_t *)&coordinate_status_register, 7);
        } while (!buffer_ready);

        // Reset buffer status to trigger another touchscreen sample
        i2c_write_register_byte(i2c_dev_fd, I2C_GT9110_address, 0x814E, 0);

        // Read and send all coordinate data
        uint8_t number_of_touches = get_bits((uint32_t *)&coordinate_status_register, 3, 0);
        i2c_read_register_bytes(i2c_dev_fd, I2C_GT9110_address, 0x814F, touchscreen_coordinate_data,
                                sizeof(touchscreen_coordinate_data));
        serialize_touchscreen_coordinate_data(number_of_touches, touchscreen_coordinate_data);
    }
}

int main(int argc, char* argv[]) {
    int fd;

    /* Open I2C device. */
    fd = open("/dev/i2c-1", O_RDWR);
    if (fd < 0) {
        perror("i2c device open");
        return 2;
    }

    gt9110_poll_and_serialize_touchscreen_data(fd);

    // i2c_write_register_byte(fd, 0x5D, 0x814E, 0);

    // uint8_t data[2]={0, 0};
    // i2c_write_register_bytes(fd, 0x5D, 0x814E, data, sizeof(data));

    // uint16_t data = i2c_read_register_byte(fd, 0x5D, 0x814E);
    // printf("%x", data);

    // i2c_write_register_byte(fd, 0x5D, 0x814E, 0);
    // uint8_t data[5];
    // i2c_read_register_bytes(fd, 0x5D, 0x814E, data, sizeof(data));

    /* Finish. */
    close(fd);
    return 0;
}
