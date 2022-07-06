/**
 * @file i2c.h
 * @brief Utility functions for register interfacing through the I2C protocol.
 */

#ifndef USBTRACKPAD_I2C_H
#define USBTRACKPAD_I2C_H

#ifdef __cplusplus
extern "C" {
#endif

#include "../lang/lang.h"
#include <linux/i2c-dev.h>
#include <linux/i2c.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>
#include <sys/ioctl.h>

/**
 * Writes a byte to a register of an I2C slave.
 * @param i2c_dev_fd the i2c device file descriptor
 * @param slave_address the slave address
 * @param register_address the slave register address to write data to
 * @param register_data the register data to write
 * @return a negative number if an error occurred or zero if successful
 */
int32_t i2c_write_register_byte(uint32_t i2c_dev_fd, uint16_t slave_address, uint16_t register_address,
        uint8_t register_data);

/**
 * Writes an array of bytes to the registers of an I2C slave.
 * @param i2c_dev_fd the i2c device file descriptor
 * @param slave_address the slave address
 * @param register_address the slave register address to write data to
 * @param register_data the register data array to write
 * @param register_data_length the length of 'register_data'
 * @return a negative number if an error occurred or zero if successful
 */
int32_t i2c_write_register_bytes(uint32_t i2c_dev_fd, uint16_t slave_address, uint16_t register_address,
        uint8_t* register_data, uint16_t register_data_length);

/**
 * Reads a byte from a register of an I2C slave.
 * @param i2c_dev_fd the i2c device file descriptor
 * @param slave_address the slave address
 * @param register_address the slave register address to read data from
 * @return the register data byte or a negative number if an error occurred
 */
int32_t i2c_read_register_byte(uint32_t i2c_dev_fd, uint16_t slave_address, uint16_t register_address);

/**
 * Reads an array of bytes from the registers of an I2C slave.
 * @param i2c_dev_fd the i2c device file descriptor
 * @param slave_address the slave address
 * @param register_address the slave register address to read data from
 * @param register_data the register data array buffer to read into
 * @param register_data_length the length of 'register_data'
 * @return a negative number if an error occurred or zero if successful
 */
uint32_t i2c_read_register_bytes(uint32_t i2c_dev_fd, uint16_t slave_address, uint16_t register_address,
        const uint8_t* register_data, uint16_t register_data_length);

#ifdef __cplusplus
}
#endif

#endif // USBTRACKPAD_I2C_H
