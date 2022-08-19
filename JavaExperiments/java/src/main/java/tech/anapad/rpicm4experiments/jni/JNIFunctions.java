package tech.anapad.rpicm4experiments.jni;

/**
 * {@link JNIFunctions} is used to interface with the low-level C functions via JNI.
 */
public class JNIFunctions {

    static {
        // Load the .so shared library that should be placed in '/lib' and be named exactly "libJavaExperiments.so"
        System.loadLibrary("JavaExperiments");
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
     * @param slaveAddress          the slave address
     * @param registerAddress       the register address
     * @param registerData          the register data
     * @param is8BitRegisterAddress <code>true</code> for 8 bit register address, <code>false</code> for 16 bit
     *                              register address
     *
     * @throws Exception thrown for {@link Exception}s
     */
    public native void i2cWriteRegisterByte(short slaveAddress, short registerAddress, byte registerData,
            boolean is8BitRegisterAddress) throws Exception;

    /**
     * Writes an array of bytes to the registers of an I2C slave.
     *
     * @param slaveAddress          the slave address
     * @param registerAddress       the register address
     * @param registerData          the register data
     * @param is8BitRegisterAddress <code>true</code> for 8 bit register address, <code>false</code> for 16 bit
     *                              register address
     *
     * @throws Exception thrown for {@link Exception}s
     */
    public native void i2cWriteRegisterBytes(short slaveAddress, short registerAddress, byte[] registerData,
            boolean is8BitRegisterAddress) throws Exception;

    /**
     * Reads a byte from a register of an I2C slave.
     *
     * @param slaveAddress          the slave address
     * @param registerAddress       the register address
     * @param is8BitRegisterAddress <code>true</code> for 8 bit register address, <code>false</code> for 16 bit
     *                              register address
     *
     * @return the read byte
     *
     * @throws Exception thrown for {@link Exception}s
     */
    public native byte i2cReadRegisterByte(short slaveAddress, short registerAddress, boolean is8BitRegisterAddress)
            throws Exception;

    /**
     * Reads an array of bytes from the registers of an I2C slave.
     *
     * @param slaveAddress          the slave address
     * @param registerAddress       the register address
     * @param readSize              the number of bytes to read
     * @param is8BitRegisterAddress <code>true</code> for 8 bit register address, <code>false</code> for 16 bit
     *                              register address
     *
     * @return the read byte array
     *
     * @throws Exception thrown for {@link Exception}s
     */
    public native byte[] i2cReadRegisterBytes(short slaveAddress, short registerAddress, int readSize,
            boolean is8BitRegisterAddress) throws Exception;
}
