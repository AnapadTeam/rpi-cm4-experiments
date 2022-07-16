package tech.anapad.rpicm4experiments;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.anapad.rpicm4experiments.jni.JNIFunctions;

/**
 * {@link LRAHapticsJavaTest} is the main class for testing the LRA haptics via the DRV5605L using the RPi CM4 with
 * Java.
 */
public class LRAHapticsJavaTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(LRAHapticsJavaTest.class);

    /**
     * The I2C address of the DRV2605L chip.
     */
    public static final int I2C_DRV2605L_ADDRESS = 0x5A;

    private boolean shutdownGracefully;

    /**
     * Instantiates a new {@link LRAHapticsJavaTest}.
     */
    public LRAHapticsJavaTest() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

        shutdownGracefully = false;
    }

    /**
     * Starts {@link LRAHapticsJavaTest}.
     */
    public void start() {
        LOGGER.info("Starting...");

        LOGGER.info("Started");

        run();
        stop();
    }

    /**
     * Contains the run loop logic.
     */
    private void run() {
        LOGGER.info("Running...");

        JNIFunctions jniFunctions = new JNIFunctions();
        jniFunctions.i2cStart();

        // TODO

        jniFunctions.i2cStop();
    }

    /**
     * Stops {@link LRAHapticsJavaTest}.
     */
    public void stop() {
        if (shutdownGracefully) {
            return;
        }

        LOGGER.info("Stopping...");

        LOGGER.info("Stopped.");
        shutdownGracefully = true;
    }

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {
        new LRAHapticsJavaTest().start();
    }
}
