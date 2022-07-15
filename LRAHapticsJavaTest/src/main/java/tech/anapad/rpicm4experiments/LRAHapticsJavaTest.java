package tech.anapad.rpicm4experiments;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.IOType;
import com.pi4j.io.i2c.I2C;
import com.pi4j.io.i2c.I2CConfig;
import com.pi4j.io.i2c.I2CProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private final Context pi4j;
    private boolean shutdownGracefully;

    /**
     * Instantiates a new {@link LRAHapticsJavaTest}.
     */
    public LRAHapticsJavaTest() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

        pi4j = Pi4J.newAutoContext();
        shutdownGracefully = false;
    }

    /**
     * Starts {@link LRAHapticsJavaTest}.
     */
    public void start() {
        LOGGER.info("Starting...");

        final I2CProvider i2CProvider = pi4j.provider("linuxfs-i2c");
        final I2CConfig i2cConfig = I2C.newConfigBuilder(pi4j)
                .id("I2C_DRV2605L")
                .bus(1)
                .device(I2C_DRV2605L_ADDRESS)
                .build();

        try (I2C drv2605lDevice = i2CProvider.create(i2cConfig)) {
            int reg = drv2605lDevice.readRegister(0x00);
            LOGGER.info("{}", reg);
            if (reg < 0) {
                LOGGER.error("Could not read I2C register!");
            }
        } catch (Exception exception) {
            LOGGER.error("Could not read I2C register!", exception);
        }

        stop();
    }

    /**
     * Stops {@link LRAHapticsJavaTest}.
     */
    public void stop() {
        if (shutdownGracefully) {
            return;
        }

        LOGGER.info("Stopping...");

        if (pi4j != null) {
            pi4j.shutdown();
        }

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
