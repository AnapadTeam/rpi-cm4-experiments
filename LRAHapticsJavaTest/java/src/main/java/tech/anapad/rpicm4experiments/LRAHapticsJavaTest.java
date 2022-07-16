package tech.anapad.rpicm4experiments;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.anapad.rpicm4experiments.jni.JNIFunctions;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * {@link LRAHapticsJavaTest} is the main class for testing the LRA haptics via the DRV5605L using the RPi CM4 with
 * Java.
 */
public class LRAHapticsJavaTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(LRAHapticsJavaTest.class);

    /**
     * The I2C address of the DRV2605L chip.
     */
    public static final short I2C_DRV2605L_ADDRESS = 0x5A;

    private final JNIFunctions jniFunctions;
    private boolean shutdownGracefully;

    /**
     * Instantiates a new {@link LRAHapticsJavaTest}.
     */
    public LRAHapticsJavaTest() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
        jniFunctions = new JNIFunctions();
        shutdownGracefully = false;
    }

    /**
     * Starts {@link LRAHapticsJavaTest}.
     */
    public void start() throws Exception {
        LOGGER.info("Starting...");

        final BufferedReader inputStreamReader = new BufferedReader(new InputStreamReader(System.in));
        LOGGER.info("Created input reader.");

        jniFunctions.i2cStart();
        LOGGER.info("Started I2C interface.");

        jniFunctions.i2cWriteRegisterByte(I2C_DRV2605L_ADDRESS, (byte) 0x01, (byte) 0x05);
        LOGGER.info("Set DRV2605L to RTP mode.");

        jniFunctions.i2cWriteRegisterByte(I2C_DRV2605L_ADDRESS, (byte) 0x17, (byte) 0xFF);
        LOGGER.info("Set DRV2605L overdrive voltage-clamp to max value.");

        byte feedbackControlRegister = jniFunctions.i2cReadRegisterByte(I2C_DRV2605L_ADDRESS, (byte) 0x1A);
        feedbackControlRegister |= (1 << 7); // Set LRA Mode
        jniFunctions.i2cWriteRegisterByte(I2C_DRV2605L_ADDRESS, (byte) 0x1A, feedbackControlRegister);
        LOGGER.info("Set DRV2605L into LRA mode.");

        jniFunctions.i2cWriteRegisterByte(I2C_DRV2605L_ADDRESS, (byte) 0x02, (byte) 0x00);
        LOGGER.info("Set RTP register to zero.");

        LOGGER.info("Started");
        LOGGER.info("Running...");

        printAvailableCommands();

        byte rtpRegisterValue = 0;
        boolean rtpRegisterValueSet = false;
        String inputLine;
        while ((inputLine = inputStreamReader.readLine()) != null) {
            if (inputLine.equals("a")) {
                LOGGER.info("Enter RTP value between -128 and +127 and press return:");
                if ((inputLine = inputStreamReader.readLine()) != null) {
                    try {
                        rtpRegisterValue = Byte.parseByte(inputLine);
                    } catch (Exception exception) {
                        LOGGER.info("Invalid input.");
                        continue;
                    }
                    if (rtpRegisterValueSet) {
                        jniFunctions.i2cWriteRegisterByte(I2C_DRV2605L_ADDRESS, (byte) 0x02, rtpRegisterValue);
                    }
                    LOGGER.info("Set RTP register to: {}", rtpRegisterValue);
                }
            } else if (inputLine.equals("d")) {
                LOGGER.info("Enter duration to play RTP mode in milliseconds:");
                if ((inputLine = inputStreamReader.readLine()) != null) {
                    int milliseconds;
                    try {
                        milliseconds = Integer.parseInt(inputLine);
                    } catch (Exception exception) {
                        LOGGER.info("Invalid input.");
                        continue;
                    }
                    jniFunctions.i2cWriteRegisterByte(I2C_DRV2605L_ADDRESS, (byte) 0x02, rtpRegisterValue);
                    LOGGER.info("Set RTP register to: {}", rtpRegisterValue);
                    Thread.sleep(milliseconds);
                    jniFunctions.i2cWriteRegisterByte(I2C_DRV2605L_ADDRESS, (byte) 0x02, (byte) 0x00);
                    LOGGER.info("Set RTP register to: 0");
                    rtpRegisterValueSet = false;
                }
            } else if (inputLine.equals(" ")) {
                if (rtpRegisterValueSet) {
                    jniFunctions.i2cWriteRegisterByte(I2C_DRV2605L_ADDRESS, (byte) 0x02, (byte) 0x00);
                    LOGGER.info("Toggled RTP mode off.");
                    rtpRegisterValueSet = false;
                } else {
                    jniFunctions.i2cWriteRegisterByte(I2C_DRV2605L_ADDRESS, (byte) 0x02, rtpRegisterValue);
                    LOGGER.info("Toggled RTP mode on.");
                    rtpRegisterValueSet = true;
                }
            } else {
                printAvailableCommands();
            }
        }
    }

    private void printAvailableCommands() {
        LOGGER.info("Available commands:");
        LOGGER.info("  Type 'a' to set amplitude.");
        LOGGER.info("  Type 'd' to set RTP register for a certain duration.");
        LOGGER.info("  Press the space bar to toggle RTP register value to on/off.");
    }

    /**
     * Stops {@link LRAHapticsJavaTest}.
     */
    public void stop() {
        if (shutdownGracefully) {
            return;
        }

        LOGGER.info("Stopping...");

        try {
            jniFunctions.i2cWriteRegisterByte(I2C_DRV2605L_ADDRESS, (byte) 0x02, (byte) 0x00);
            LOGGER.info("Set RTP register to zero.");
            jniFunctions.i2cStop();
            LOGGER.info("Stopped I2C interface.");
        } catch (Exception exception) {
            LOGGER.error("Could not stop successfully!", exception);
        }

        LOGGER.info("Stopped.");
        shutdownGracefully = true;
    }

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     */
    public static void main(String[] args) throws Exception {
        new LRAHapticsJavaTest().start();
    }
}
