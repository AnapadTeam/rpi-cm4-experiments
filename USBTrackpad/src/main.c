/**
 * @file main.c
 * @brief The entry point of the application.
 */

#include "util/i2c/i2c.h"
#include "util/number/get_set_bits.h"
#include <fcntl.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char* argv[]) {
    /* Open I2C device. */
    int fd = open("/dev/i2c-1", O_RDWR);
    if (fd < 0) {
        perror("i2c device open");
        return 2;
    }

    // TODO

    close(fd);
    return 1;
}
