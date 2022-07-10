/**
 * @file gt9110_config.c
 */

#include "gt9110_config.h"

uint8_t calculate_config_checksum(const uint8_t* config, uint32_t config_size) {
    // Method for creating this checksum was copied from here: shorturl.at/bkVZ6 and here: shorturl.at/HIMS3
    uint8_t check_sum = 0;
    for (int32_t index = 0; index < config_size; index++) {
        check_sum += config[index];
    }
    check_sum = (~check_sum) + 1;
    return check_sum;
}

uint32_t get_configuration_data_size() {
    return GT9110_ADDRESS_REGISTER_CONFIG_END - GT9110_ADDRESS_REGISTER_CONFIG_START + 1;
}

uint8_t* read_configuration_data(uint32_t i2c_dev_fd, uint16_t gt9110_slave_address) {
    uint8_t* configuration_data = malloc(get_configuration_data_size());
    if (i2c_read_register_bytes(i2c_dev_fd, gt9110_slave_address, GT9110_ADDRESS_REGISTER_CONFIG_START,
                configuration_data, get_configuration_data_size())) {
        perror("Couldn't read configuration data");
        return NULL;
    }
    return configuration_data;
}

int32_t write_configuration_data(uint32_t i2c_dev_fd, uint16_t gt9110_slave_address, const uint8_t* config) {
    uint8_t config_checksum = calculate_config_checksum(config, get_configuration_data_size());
    uint8_t config_fresh_value = 0x01;
    uint8_t all_configuration_data[get_configuration_data_size() + 2];

    memcpy(all_configuration_data, config, get_configuration_data_size());
    memcpy(all_configuration_data + get_configuration_data_size(), &config_checksum, 1);
    memcpy(all_configuration_data + get_configuration_data_size() + 1, &config_fresh_value, 1);

    uint32_t code = i2c_write_register_bytes(i2c_dev_fd, gt9110_slave_address, GT9110_ADDRESS_REGISTER_CONFIG_START,
            all_configuration_data, (uint16_t) sizeof(all_configuration_data));
    if (code) {
        perror("Couldn't write configuration data");
    }
    return code;
}
