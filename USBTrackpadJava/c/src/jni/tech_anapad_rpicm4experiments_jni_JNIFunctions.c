/**
 * @file tech_anapad_rpicm4experiments_jni_JNIFunctions.c
 */

#include "tech_anapad_rpicm4experiments_jni_JNIFunctions.h"

#include "../util/i2c/i2c.h"
#include "../util/lang/lang.h"
#include <fcntl.h>
#include <unistd.h>

int32_t i2c_dev_fd = -1;

JNIEXPORT void JNICALL Java_tech_anapad_rpicm4experiments_jni_JNIFunctions_i2cStart(JNIEnv* env, jobject object) {
    i2c_dev_fd = open("/dev/i2c-5", O_RDWR);
    if (i2c_dev_fd < 0) {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/Exception"), "Could not open I2C device!");
    }
}

JNIEXPORT void JNICALL Java_tech_anapad_rpicm4experiments_jni_JNIFunctions_i2cStop(JNIEnv* env, jobject object) {
    int32_t code = close(i2c_dev_fd);
    if (code < 0) {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/Exception"), "Could not close I2C device!");
    }
}

JNIEXPORT void JNICALL Java_tech_anapad_rpicm4experiments_jni_JNIFunctions_i2cWriteByte(JNIEnv* env, jobject object,
        jshort slave_address, jbyte byte) {
    int32_t code = i2c_write_byte(i2c_dev_fd, slave_address, byte);
    if (code < 0) {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/Exception"), "Could not write to I2C device!");
    }
}

JNIEXPORT void JNICALL Java_tech_anapad_rpicm4experiments_jni_JNIFunctions_i2cWriteRegisterByte(JNIEnv* env,
        jobject object, jshort slave_address, jshort register_address, jbyte register_data,
        jboolean is8BitRegisterAddress) {
    int32_t code =
            i2c_write_register_byte(i2c_dev_fd, slave_address, register_address, register_data, is8BitRegisterAddress);
    if (code < 0) {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/Exception"), "Could not write to I2C device!");
    }
}

JNIEXPORT void JNICALL Java_tech_anapad_rpicm4experiments_jni_JNIFunctions_i2cWriteRegisterBytes(JNIEnv* env,
        jobject object, jshort slave_address, jshort register_address, jbyteArray register_data,
        jboolean is8BitRegisterAddress) {
    int8_t* register_data_array = (*env)->GetByteArrayElements(env, register_data, 0);
    int32_t register_data_size = (*env)->GetArrayLength(env, register_data);
    int32_t code = i2c_write_register_bytes(i2c_dev_fd, slave_address, register_address, (uint8_t*) register_data_array,
            register_data_size, is8BitRegisterAddress);
    (*env)->ReleaseByteArrayElements(env, register_data, register_data_array, 0);
    if (code < 0) {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/Exception"), "Could not write to I2C device!");
    }
}

JNIEXPORT jbyte JNICALL Java_tech_anapad_rpicm4experiments_jni_JNIFunctions_i2cReadByte(JNIEnv* env, jobject object,
        jshort slave_address) {
    int32_t byte = i2c_read_byte(i2c_dev_fd, slave_address);
    if (byte < 0) {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/Exception"), "Could not read from I2C device!");
    }
    return (int8_t) byte;
}

JNIEXPORT jbyte JNICALL Java_tech_anapad_rpicm4experiments_jni_JNIFunctions_i2cReadRegisterByte(JNIEnv* env,
        jobject object, jshort slave_address, jshort register_address, jboolean is8BitRegisterAddress) {
    int32_t register_data_byte =
            i2c_read_register_byte(i2c_dev_fd, slave_address, register_address, is8BitRegisterAddress);
    if (register_data_byte < 0) {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/Exception"), "Could not read from I2C device!");
    }
    return (int8_t) register_data_byte;
}

JNIEXPORT jbyteArray JNICALL Java_tech_anapad_rpicm4experiments_jni_JNIFunctions_i2cReadRegisterBytes(JNIEnv* env,
        jobject object, jshort slave_address, jshort register_address, jint read_size, jboolean is8BitRegisterAddress) {
    int8_t register_data[read_size];
    int32_t code = i2c_read_register_bytes(i2c_dev_fd, slave_address, register_address, (uint8_t*) register_data,
            SIZE_OF_ARRAY(register_data), is8BitRegisterAddress);
    if (code < 0) {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/Exception"), "Could not read from I2C device!");
    }
    jbyteArray java_byte_array = (*env)->NewByteArray(env, read_size);
    if (java_byte_array == NULL) {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/Exception"), "Out of memory error!");
    }
    (*env)->SetByteArrayRegion(env, java_byte_array, 0, SIZE_OF_ARRAY(register_data), register_data);
    return java_byte_array;
}
