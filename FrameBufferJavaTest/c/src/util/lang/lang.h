/**
 * @file lang.h
 * @brief Utility functions for the C language.
 */

#ifndef FRAMEBUFFERJAVATEST_LANG_H
#define FRAMEBUFFERJAVATEST_LANG_H

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Gets the size of an array. NOTE: this can only be used if the given array argument is an explicit C array declared in
 * the same function that this macro is called in. It will return an incorrect number if called on a function parameter.
 */
#define SIZE_OF_ARRAY(x) (sizeof(x) / sizeof((x)[0]))

/**
 * Calls the given function argument and if it returns a negative value, an error message will be printed and will
 * return the return value.
 */
#define CHECK(x)                                                                                                       \
    do {                                                                                                               \
        int32_t return_value = (int32_t) (x);                                                                          \
        if (return_value < 0) {                                                                                        \
            fprintf(stderr, "Runtime error: %s returned %d at %s:%d\n", #x, return_value, __FILE__, __LINE__);         \
            return return_value;                                                                                       \
        }                                                                                                              \
    } while (0)

#ifdef __cplusplus
}
#endif

#endif // FRAMEBUFFERJAVATEST_LANG_H
