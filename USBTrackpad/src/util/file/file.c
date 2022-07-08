/**
 * @file file.c
 */

#include "file.h"

int32_t make_path(char* the_path) {
    char* path = strdup(the_path);
    char* save_path = path;
    char* sep1;
    char* sep2 = 0;
    do {
        size_t idx = (sep2 - path) < 0 ? 0 : sep2 - path;
        sep1 = strchr(path + idx, '/');
        sep2 = strchr(sep1 + 1, '/');
        if (sep2) {
            path[sep2 - path] = 0;
        }
        if (mkdir(path, 0755) && errno != EEXIST)
            return -1;
        if (sep2) {
            path[sep2 - path] = '/';
        }
    } while (sep2);

    free(save_path);
    return 0;
}

int32_t remove_path(char* the_path) {
    return rmdir(the_path);
}

size_t make_file_with_string_contents(char* file_path, char* string_content) {
    return make_file_with_binary_contents(file_path, string_content, strlen(string_content));
}

size_t make_file_with_binary_contents(char* file_path, void* binary_content, uint32_t binary_content_length) {
    int32_t fd = open(file_path, O_RDWR);
    if (fd < 0)
        return -1;
    size_t n = write(fd, binary_content, binary_content_length);
    close(fd);
    return n;
}
