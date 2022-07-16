package tech.anapad.rpicm4experiments.jni;

/**
 * {@link JNIFunctions} is used to interface with the low-level C functions via JNI.
 */
public class JNIFunctions {

    static {
        // Load the .so shared library that should be placed in '/lib' and be named exactly "libLRAHapticsJavaTest.so"
        System.loadLibrary("LRAHapticsJavaTest");
    }

    /**
     * Starts the low-level I2C interface.
     */
    public native void i2cStart();

    /**
     * Stops the low-level I2C interface.
     */
    public native void i2cStop();

    /**
     * Writes a byte to a register of an I2C slave.
     *
     * @param slaveAddress    the slave address
     * @param registerAddress the register address
     * @param registerData    the register data
     */
    public native void i2cWriteRegisterByte(short slaveAddress, byte registerAddress, byte registerData);

    /**
     * Writes an array of bytes to the registers of an I2C slave.
     *
     * @param slaveAddress    the slave address
     * @param registerAddress the register address
     * @param registerData    the register data
     */
    public native void i2cWriteRegisterBytes(short slaveAddress, byte registerAddress, byte[] registerData);

    /**
     * Reads a byte from a register of an I2C slave.
     *
     * @param slaveAddress    the slave address
     * @param registerAddress the register address
     *
     * @return the read byte
     */
    public native byte i2cReadRegisterByte(short slaveAddress, byte registerAddress);

    /**
     * Reads an array of bytes from the registers of an I2C slave.
     *
     * @param slaveAddress    the slave address
     * @param registerAddress the register address
     * @param readSize        the number of bytes to read
     *
     * @return the read byte array
     */
    public native byte[] i2cReadRegisterBytes(short slaveAddress, byte registerAddress, int readSize);
}
