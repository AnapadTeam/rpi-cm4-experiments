/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class tech_anapad_rpicm4experiments_jni_JNIFunctions */

#ifndef _Included_tech_anapad_rpicm4experiments_jni_JNIFunctions
#define _Included_tech_anapad_rpicm4experiments_jni_JNIFunctions
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     tech_anapad_rpicm4experiments_jni_JNIFunctions
 * Method:    i2cStart
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_tech_anapad_rpicm4experiments_jni_JNIFunctions_i2cStart(JNIEnv*, jobject);

/*
 * Class:     tech_anapad_rpicm4experiments_jni_JNIFunctions
 * Method:    i2cStop
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_tech_anapad_rpicm4experiments_jni_JNIFunctions_i2cStop(JNIEnv*, jobject);

/*
 * Class:     tech_anapad_rpicm4experiments_jni_JNIFunctions
 * Method:    i2cWriteRegisterByte
 * Signature: (SBB)V
 */
JNIEXPORT void JNICALL Java_tech_anapad_rpicm4experiments_jni_JNIFunctions_i2cWriteRegisterByte(JNIEnv*, jobject,
        jshort, jbyte, jbyte);

/*
 * Class:     tech_anapad_rpicm4experiments_jni_JNIFunctions
 * Method:    i2cWriteRegisterBytes
 * Signature: (SB[B)V
 */
JNIEXPORT void JNICALL Java_tech_anapad_rpicm4experiments_jni_JNIFunctions_i2cWriteRegisterBytes(JNIEnv*, jobject,
        jshort, jbyte, jbyteArray);

/*
 * Class:     tech_anapad_rpicm4experiments_jni_JNIFunctions
 * Method:    i2cReadRegisterByte
 * Signature: (SB)B
 */
JNIEXPORT jbyte JNICALL Java_tech_anapad_rpicm4experiments_jni_JNIFunctions_i2cReadRegisterByte(JNIEnv*, jobject,
        jshort, jbyte);

/*
 * Class:     tech_anapad_rpicm4experiments_jni_JNIFunctions
 * Method:    i2cReadRegisterBytes
 * Signature: (SBI)[B
 */
JNIEXPORT jbyteArray JNICALL Java_tech_anapad_rpicm4experiments_jni_JNIFunctions_i2cReadRegisterBytes(JNIEnv*, jobject,
        jshort, jbyte, jint);

#ifdef __cplusplus
}
#endif
#endif
