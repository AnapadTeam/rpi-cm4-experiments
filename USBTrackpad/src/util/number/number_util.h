/**
 * @file number_util.h
 * @brief Utility functions for numbers.
 */

#ifndef USBTRACKPAD_NUMBER_UTIL_H
#define USBTRACKPAD_NUMBER_UTIL_H

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Calculates the absolute value of the given argument.
 * @param x the number
 */
#define ABS(x) (((x) < 0) ? -(x) : (x))

/**
 * Calculates the greater of the two arguments.
 * @param x the first number
 * @param y the second number
 */
#define MAX(x, y) (((x) > (y)) ? (x) : (y))

/**
 * Calculates the smaller of the two arguments.
 * @param x the first number
 * @param y the second number
 */
#define MIN(x, y) (((x) < (y)) ? (x) : (y))

/**
 * Clamps given argument to the given range.
 * @param x the number to clamp
 * @param min the minimum of the clamping range
 * @param max the maximum of the clamping range
 */
#define CLAMP(x, min, max) MAX(min, MIN(x, max))

#ifdef __cplusplus
}
#endif

#endif // USBTRACKPAD_NUMBER_UTIL_H
