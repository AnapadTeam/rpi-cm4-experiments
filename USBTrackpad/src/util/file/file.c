/**
 * @file file.c
 */

#include "file.h"

int32_t create_directory_path(char* the_path) {
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
        if (mkdir(path, 0755) && errno != EEXIST) {
            perror(the_path);
            return -1;
        }
        if (sep2) {
            path[sep2 - path] = '/';
        }
    } while (sep2);

    free(save_path);
    return 0;
}

int32_t remove_directory(char* the_path) {
    int32_t code = rmdir(the_path);
    if (code < 0) {
        perror(the_path);
    }
    return code;
}

int32_t remove_file(char* file_path) {
    int32_t code = remove(file_path);
    if (code < 0) {
        perror(file_path);
    }
    return code;
}

size_t write_file_with_string_contents(char* file_path, char* string_content) {
    FILE* fd = fopen(file_path, "w");
    if (fd == NULL) {
        perror(file_path);
        return -1;
    }
    size_t n = fprintf(fd, "%s", string_content);
    fclose(fd);
    return n;
}

size_t write_file_with_binary_contents(char* file_path, void* binary_content, uint32_t binary_content_length) {
    FILE* fd = fopen(file_path, "w");
    if (fd == NULL) {
        perror(file_path);
        return -1;
    }
    size_t n = fwrite(binary_content, 1, binary_content_length, fd);
    fclose(fd);
    return n;
}
