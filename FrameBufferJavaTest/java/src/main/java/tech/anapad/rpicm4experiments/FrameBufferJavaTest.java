package tech.anapad.rpicm4experiments;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.anapad.rpicm4experiments.jni.JNIFunctions;

/**
 * {@link FrameBufferJavaTest} is the main class for testing the LRA haptics via the DRV5605L using the RPi CM4 with
 * Java.
 */
public class FrameBufferJavaTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(FrameBufferJavaTest.class);

    /**
     * The I2C address of the GT9110 chip.
     */
    public static final short I2C_GT9110_ADDRESS = 0x5D;

    private final JNIFunctions jniFunctions;
    private boolean shutdownGracefully;

    /**
     * Instantiates a new {@link FrameBufferJavaTest}.
     */
    public FrameBufferJavaTest() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
        jniFunctions = new JNIFunctions();
        shutdownGracefully = false;
    }

    /**
     * Starts {@link FrameBufferJavaTest}.
     */
    public void start() throws Exception {
        LOGGER.info("Starting...");

        jniFunctions.i2cStart();
        LOGGER.info("Started I2C interface.");

        LOGGER.info("Started");
        LOGGER.info("Running...");

        // TODO
    }

    /**
     * Stops {@link FrameBufferJavaTest}.
     */
    public void stop() {
        if (shutdownGracefully) {
            return;
        }

        LOGGER.info("Stopping...");

        try {
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
        new FrameBufferJavaTest().start();
    }
}
