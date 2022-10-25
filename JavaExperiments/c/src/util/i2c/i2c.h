/**
 * @file i2c.h
 * @brief Utility functions for register interfacing through the I2C protocol.
 */

#ifndef JAVAEXPERIMENTS_I2C_H
#define JAVAEXPERIMENTS_I2C_H

#ifdef __cplusplus
extern "C" {
#endif

#include "../lang/lang.h"
#include <linux/i2c-dev.h>
#include <linux/i2c.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>
#include <sys/ioctl.h>

/**
 * Writes a byte to an I2C slave.
 * @param i2c_dev_fd the i2c device file descriptor
 * @param slave_address the slave address
 * @param register_data the byte to write
 */
int32_t i2c_write_byte(uint32_t i2c_dev_fd, uint16_t slave_address, uint8_t byte);

/**
 * Writes a byte to a register of an I2C slave.
 * @param i2c_dev_fd the i2c device file descriptor
 * @param slave_address the slave address
 * @param register_address the slave register address to write data to
 * @param register_data the register data to write
 * @param is8BitRegisterAddress true for 8 bit register address, false for 16 bit register address
 * @return a negative number if an error occurred or zero if successful
 */
int32_t i2c_write_register_byte(uint32_t i2c_dev_fd, uint16_t slave_address, uint16_t register_address,
        uint8_t register_data, bool is8BitRegisterAddress);

/**
 * Writes an array of bytes to the registers of an I2C slave.
 * @param i2c_dev_fd the i2c device file descriptor
 * @param slave_address the slave address
 * @param register_address the slave register address to write data to
 * @param register_data the register data array to write
 * @param register_data_length the length of 'register_data'
 * @param is8BitRegisterAddress true for 8 bit register address, false for 16 bit register address
 * @return a negative number if an error occurred or zero if successful
 */
int32_t i2c_write_register_bytes(uint32_t i2c_dev_fd, uint16_t slave_address, uint16_t register_address,
        uint8_t* register_data, uint16_t register_data_length, bool is8BitRegisterAddress);

/**
 * Reads a byte from an I2C slave.
 * @param i2c_dev_fd the i2c device file descriptor
 * @param slave_address the slave address
 * @return the byte or a negative number if an error occurred
 */
int32_t i2c_read_byte(uint32_t i2c_dev_fd, uint16_t slave_address);

/**
 * Reads a byte from a register of an I2C slave.
 * @param i2c_dev_fd the i2c device file descriptor
 * @param slave_address the slave address
 * @param register_address the slave register address to read data from
 * @param is8BitRegisterAddress true for 8 bit register address, false for 16 bit register address
 * @return the register data byte or a negative number if an error occurred
 */
int32_t i2c_read_register_byte(uint32_t i2c_dev_fd, uint16_t slave_address, uint16_t register_address,
        bool is8BitRegisterAddress);

/**
 * Reads an array of bytes from the registers of an I2C slave.
 * @param i2c_dev_fd the i2c device file descriptor
 * @param slave_address the slave address
 * @param register_address the slave register address to read data from
 * @param register_data the register data array buffer to read into
 * @param register_data_length the length of 'register_data'
 * @param is8BitRegisterAddress true for 8 bit register address, false for 16 bit register address
 * @return a negative number if an error occurred or zero if successful
 */
int32_t i2c_read_register_bytes(uint32_t i2c_dev_fd, uint16_t slave_address, uint16_t register_address,
        uint8_t* register_data, uint16_t register_data_length, bool is8BitRegisterAddress);

#ifdef __cplusplus
}
#endif

#endif // JAVAEXPERIMENTS_I2C_H
