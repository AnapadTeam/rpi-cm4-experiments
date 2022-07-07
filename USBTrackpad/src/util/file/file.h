/**
 * @file file.h
 * @brief Contains utility functions relating to file IO.
 */

#ifndef USBTRACKPAD_FILE_H
#define USBTRACKPAD_FILE_H

#ifdef __cplusplus
extern "C" {
#endif

#include <errno.h>
#include <fcntl.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>

#include "../lang/lang.h"

#include <string.h>

#include <stdio.h>

/**
 * Creates a directory path (recursively if needed)
 * @param the_path a string of the path to create
 * @return a negative number if an error occurred or zero if successful
 * @see https://gist.github.com/yairgd/26da61da37c641e3c63f5a870f5c7c02
 */
int32_t make_path(char* the_path);

int32_t remove_path(char* the_path);

/**
 * Creates a file at the specified file path with the contents of the given string
 * @param file_path a string containing the file path
 * @param string_content a string containing the content to put inside 'file_path'
 * @return a negative number if an error occurred or the number of bytes written if successful
 */
size_t make_file_with_string_contents(char* file_path, char* string_content);

/**
 * Creates a file at the specified file path with the contents of the given binary data
 * @param file_path a string containing the file path
 * @param binary_content the binary content
 * @param binary_content_length the binary content length
 * @return a negative number if an error occurred or the number of bytes written if successful
 */
size_t make_file_with_binary_contents(char* file_path, void* binary_content, uint32_t binary_content_length);

#ifdef __cplusplus
}
#endif

#endif // USBTRACKPAD_FILE_H
