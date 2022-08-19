/**
 * @file i2c.c
 */

#include "i2c.h"

// Below is pretty crappy code, re-write for production.

int32_t i2c_write_register_byte(uint32_t i2c_dev_fd, uint16_t slave_address, uint16_t register_address,
        uint8_t register_data, bool is8BitRegisterAddress) {
    struct i2c_msg i2c_msg;
    i2c_msg.addr = slave_address;
    i2c_msg.flags = 0; // Write flag
    if (is8BitRegisterAddress) {
        uint8_t register_byte_write_data[2] = {register_address & 0xFF, register_data};
        i2c_msg.len = SIZE_OF_ARRAY(register_byte_write_data);
        i2c_msg.buf = register_byte_write_data;
    } else {
        uint8_t register_byte_write_data[3] = {(register_address >> 8) & 0xFF, register_address & 0xFF, register_data};
        i2c_msg.len = SIZE_OF_ARRAY(register_byte_write_data);
        i2c_msg.buf = register_byte_write_data;
    }

    struct i2c_rdwr_ioctl_data i2c_transfer;
    i2c_transfer.msgs = &i2c_msg;
    i2c_transfer.nmsgs = 1;

    if (ioctl(i2c_dev_fd, I2C_RDWR, &i2c_transfer) < 0) {
        return -1;
    }

    return 0;
}

int32_t i2c_write_register_bytes(uint32_t i2c_dev_fd, uint16_t slave_address, uint16_t register_address,
        uint8_t* register_data, uint16_t register_data_length, bool is8BitRegisterAddress) {


    struct i2c_msg i2c_msg;
    i2c_msg.addr = slave_address;
    i2c_msg.flags = 0; // Write flag
    if (is8BitRegisterAddress) {
        uint8_t register_address_data = register_address & 0xFF;
        uint8_t register_write_data[sizeof(register_address_data) + register_data_length];
        memcpy(register_write_data, &register_address_data, sizeof(register_address_data));
        memcpy(register_write_data + sizeof(register_address_data), register_data, register_data_length);
        i2c_msg.len = SIZE_OF_ARRAY(register_write_data);
        i2c_msg.buf = register_write_data;
    } else {
        uint8_t register_address_data[2] = {(register_address >> 8) & 0xFF, register_address & 0xFF};
        uint8_t register_write_data[sizeof(register_address_data) + register_data_length];
        memcpy(register_write_data, register_address_data, sizeof(register_address_data));
        memcpy(register_write_data + sizeof(register_address_data), register_data, register_data_length);
        i2c_msg.len = SIZE_OF_ARRAY(register_write_data);
        i2c_msg.buf = register_write_data;
    }

    struct i2c_rdwr_ioctl_data i2c_transfer;
    i2c_transfer.msgs = &i2c_msg;
    i2c_transfer.nmsgs = 1;

    if (ioctl(i2c_dev_fd, I2C_RDWR, &i2c_transfer) < 0) {
        return -1;
    }

    return 0;
}

int32_t i2c_read_register_byte(uint32_t i2c_dev_fd, uint16_t slave_address, uint16_t register_address,
        bool is8BitRegisterAddress) {
    uint8_t register_byte = 0;

    struct i2c_msg i2c_msgs[2];

    i2c_msgs[0].addr = slave_address;
    i2c_msgs[0].flags = 0; // Write flag
    if (is8BitRegisterAddress) {
        uint8_t register_address_data[1] = {register_address & 0xFF};
        i2c_msgs[0].len = SIZE_OF_ARRAY(register_address_data);
        i2c_msgs[0].buf = register_address_data;
    } else {
        uint8_t register_address_data[2] = {(register_address >> 8) & 0xFF, register_address & 0xFF};
        i2c_msgs[0].len = SIZE_OF_ARRAY(register_address_data);
        i2c_msgs[0].buf = register_address_data;
    }

    i2c_msgs[1].addr = slave_address;
    i2c_msgs[1].flags = I2C_M_RD; // Read flag
    i2c_msgs[1].len = 1;
    i2c_msgs[1].buf = &register_byte;

    struct i2c_rdwr_ioctl_data i2c_transfer;
    i2c_transfer.msgs = i2c_msgs;
    i2c_transfer.nmsgs = SIZE_OF_ARRAY(i2c_msgs);

    if (ioctl(i2c_dev_fd, I2C_RDWR, &i2c_transfer) < 0) {
        return -1;
    }

    return register_byte;
}

int32_t i2c_read_register_bytes(uint32_t i2c_dev_fd, uint16_t slave_address, uint16_t register_address,
        uint8_t* register_data, uint16_t register_data_length, bool is8BitRegisterAddress) {
    struct i2c_msg i2c_msgs[2];

    i2c_msgs[0].addr = slave_address;
    i2c_msgs[0].flags = 0; // Write flag
    if (is8BitRegisterAddress) {
        uint8_t register_address_data[1] = {register_address & 0xFF};
        i2c_msgs[0].len = SIZE_OF_ARRAY(register_address_data);
        i2c_msgs[0].buf = register_address_data;
    } else {
        uint8_t register_address_data[2] = {(register_address >> 8) & 0xFF, register_address & 0xFF};
        i2c_msgs[0].len = SIZE_OF_ARRAY(register_address_data);
        i2c_msgs[0].buf = register_address_data;
    }

    i2c_msgs[1].addr = slave_address;
    i2c_msgs[1].flags = I2C_M_RD; // Read flag
    i2c_msgs[1].len = register_data_length;
    i2c_msgs[1].buf = register_data;

    struct i2c_rdwr_ioctl_data i2c_transfer;
    i2c_transfer.msgs = i2c_msgs;
    i2c_transfer.nmsgs = sizeof(i2c_msgs) / sizeof(i2c_msgs[0]);

    if (ioctl(i2c_dev_fd, I2C_RDWR, &i2c_transfer) < 0) {
        return -1;
    }

    return 0;
}
