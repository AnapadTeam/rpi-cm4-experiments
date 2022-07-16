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
     *
     * @throws Exception thrown for {@link Exception}s
     */
    public native void i2cStart() throws Exception;

    /**
     * Stops the low-level I2C interface.
     *
     * @throws Exception thrown for {@link Exception}s
     */
    public native void i2cStop() throws Exception;

    /**
     * Writes a byte to a register of an I2C slave.
     *
     * @param slaveAddress    the slave address
     * @param registerAddress the register address
     * @param registerData    the register data
     *
     * @throws Exception thrown for {@link Exception}s
     */
    public native void i2cWriteRegisterByte(short slaveAddress, byte registerAddress, byte registerData)
            throws Exception;

    /**
     * Writes an array of bytes to the registers of an I2C slave.
     *
     * @param slaveAddress    the slave address
     * @param registerAddress the register address
     * @param registerData    the register data
     *
     * @throws Exception thrown for {@link Exception}s
     */
    public native void i2cWriteRegisterBytes(short slaveAddress, byte registerAddress, byte[] registerData)
            throws Exception;

    /**
     * Reads a byte from a register of an I2C slave.
     *
     * @param slaveAddress    the slave address
     * @param registerAddress the register address
     *
     * @return the read byte
     *
     * @throws Exception thrown for {@link Exception}s
     */
    public native byte i2cReadRegisterByte(short slaveAddress, byte registerAddress) throws Exception;

    /**
     * Reads an array of bytes from the registers of an I2C slave.
     *
     * @param slaveAddress    the slave address
     * @param registerAddress the register address
     * @param readSize        the number of bytes to read
     *
     * @return the read byte array
     *
     * @throws Exception thrown for {@link Exception}s
     */
    public native byte[] i2cReadRegisterBytes(short slaveAddress, byte registerAddress, int readSize) throws Exception;
}
