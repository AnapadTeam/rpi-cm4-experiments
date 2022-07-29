package tech.anapad.rpicm4experiments;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.anapad.rpicm4experiments.jni.JNIFunctions;

import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * {@link USBTrackpadJava} is the main class for testing the LRA haptics via the DRV5605L using the RPi CM4 with Java.
 */
public class USBTrackpadJava extends Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(USBTrackpadJava.class);

    private static final short I2C_GT9110_ADDRESS_SLAVE = 0x5D;
    private static final short I2C_GT9110_ADDRESS_REGISTER_RESOLUTION = (short) 0x8146;
    private static final short I2C_GT9110_ADDRESS_REGISTER_STATUS = (short) 0x814E;
    private static final short I2C_GT9110_ADDRESS_REGISTER_TOUCH_DATA = (short) 0x814F;

    private JNIFunctions jniFunctions;
    private Canvas canvas;
    private GraphicsContext graphics;
    private boolean runLoop;

    @Override
    public void init() throws Exception {
        jniFunctions = new JNIFunctions();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        LOGGER.info("Starting...");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                stop();
            } catch (Exception exception) {
                LOGGER.error("Error while stopping!", exception);
            }
        }));

        canvas = new Canvas(1920, 515);
        graphics = canvas.getGraphicsContext2D();
        graphics.setFill(Color.AQUA);
        Scene scene = new Scene(new Pane(canvas), 1920, 515);
        scene.setFill(Color.BLACK);
        primaryStage.setScene(scene);
        primaryStage.show();

        jniFunctions.i2cStart();
        LOGGER.info("Started I2C interface.");

        LOGGER.info("Started");

        new Thread(() -> {
            try {
                LOGGER.info("Running...");
                runLoop = true;
                trackpadControlLoop();
            } catch (Exception exception) {
                LOGGER.error("Error while running!", exception);
            }
        }).start();
    }

    private final WritablePixelFormat<IntBuffer> pixelFormat =
            PixelFormat.getIntArgbPreInstance();

    private void trackpadControlLoop() throws Exception {
        LOGGER.info("Reading screen resolution...");
        byte[] touchscreenResolutionData = jniFunctions.i2cReadRegisterBytes(I2C_GT9110_ADDRESS_SLAVE,
                I2C_GT9110_ADDRESS_REGISTER_RESOLUTION, 4);
        int xResolution = (touchscreenResolutionData[1] << 8) | touchscreenResolutionData[0];
        int yResolution = (touchscreenResolutionData[3] << 8) | touchscreenResolutionData[2];
        LOGGER.info("Screen resolution: {}x{}", xResolution, yResolution);

        final int registerTouchDataLength = 8 * 10; // 10 8-byte touch data
        while (runLoop) {
            // Wait until touchscreen data is ready to be read
            byte coordinateStatusRegister;
            boolean bufferReady;
            do {
                coordinateStatusRegister = jniFunctions.i2cReadRegisterByte(I2C_GT9110_ADDRESS_SLAVE,
                        I2C_GT9110_ADDRESS_REGISTER_STATUS);
                bufferReady = ((coordinateStatusRegister & 0xFF) >> 7) == 1;

                if (!runLoop) {
                    return;
                }
            } while (!bufferReady);

            // Reset buffer status to trigger another touchscreen sample
            jniFunctions.i2cWriteRegisterByte(I2C_GT9110_ADDRESS_SLAVE, I2C_GT9110_ADDRESS_REGISTER_STATUS, (byte) 0);

            int numberOfTouches = (coordinateStatusRegister & 0xF0) >> 4;
            byte[] touchscreenCoordinateData = jniFunctions.i2cReadRegisterBytes(I2C_GT9110_ADDRESS_SLAVE,
                    I2C_GT9110_ADDRESS_REGISTER_TOUCH_DATA, registerTouchDataLength);

            if (numberOfTouches > 0) {
                int x = ((touchscreenCoordinateData[2] & 0xFF) << 8) | (touchscreenCoordinateData[1] & 0xFF);
                int y = ((touchscreenCoordinateData[4] & 0xFF) << 8) | (touchscreenCoordinateData[3] & 0xFF);
                LOGGER.info("Touchscreen touch 0: x={} y={}", x, y);
                Platform.runLater(() -> {
                    double xRatio = canvas.getWidth() / xResolution;
                    double yRatio = canvas.getHeight() / yResolution;
                    graphics.fillOval(x * xRatio - 15, y * yRatio - 15, 30, 30);
                });
            }
        }
    }

    @Override
    public void stop() throws Exception {
        LOGGER.info("Stopping...");

        runLoop = false;
        try {
            jniFunctions.i2cStop();
            LOGGER.info("Stopped I2C interface.");
            Files.write(Paths.get("/home/pi/test.txt"), new byte[]{});
        } catch (Exception exception) {
            LOGGER.error("Could not stop successfully!", exception);
        }

        LOGGER.info("Stopped.");
    }

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     */
    public static void main(String[] args) throws Exception {
        launch(args);
    }
}
