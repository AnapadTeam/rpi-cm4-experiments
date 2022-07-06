/**
 * @file lang.h
 * @brief Utility functions for the C language.
 */

#ifndef USBTRACKPAD_LANG_H
#define USBTRACKPAD_LANG_H

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Gets the size of an array. NOTE: this can only be used if the given array argument is an explicit C array declared in
 * the same function that this macro is called in. It will return an incorrect number if called on a function parameter.
 */
#define SIZE_OF_ARRAY(x) (sizeof(x) / sizeof((x)[0]))

#ifdef __cplusplus
}
#endif

#endif // USBTRACKPAD_LANG_H
