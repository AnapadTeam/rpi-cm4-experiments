package tech.anapad.rpicm4experiments;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.anapad.rpicm4experiments.jni.JNIFunctions;

import java.util.Arrays;

/**
 * {@link JavaExperiments} is the main class for testing the LRA haptics via the DRV5605L using the RPi CM4 with Java.
 */
public class JavaExperiments extends Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaExperiments.class);

    private static final short I2C_GT9110_ADDRESS_SLAVE = 0x5D;
    private static final short I2C_GT9110_ADDRESS_REGISTER_RESOLUTION = (short) 0x8146;
    private static final short I2C_GT9110_ADDRESS_REGISTER_STATUS = (short) 0x814E;
    private static final short I2C_GT9110_ADDRESS_REGISTER_TOUCH_DATA = (short) 0x814F;

    public static final short I2C_DRV2605L_ADDRESS = 0x5A;

    private JNIFunctions jniFunctions;
    private boolean runLoop;
    private Canvas canvas;
    private GraphicsContext graphics;
    private int touchscreenXResolution;
    private int touchscreenYResolution;

    @Override
    public void init() {
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

        LOGGER.info("Creating UI...");
        canvas = new Canvas(1920, 515);
        graphics = canvas.getGraphicsContext2D();
        Scene scene = new Scene(new StackPane(canvas), 1920, 515);
        scene.setFill(Color.WHITE);
        scene.setCursor(Cursor.NONE);
        primaryStage.setScene(scene);
        primaryStage.show();
        LOGGER.info("Created UI.");

        LOGGER.info("Starting I2C interface...");
        jniFunctions.i2cStart();
        LOGGER.info("Started I2C interface.");

        LOGGER.info("Setting up experiment...");
        setupExperimentDRV2605();
        LOGGER.info("Set up experiment.");

        LOGGER.info("Started");

        new Thread(() -> {
            try {
                LOGGER.info("Running...");
                runLoop = true;
                trackpadControlLoop();
            } catch (Exception exception) {
                LOGGER.error("Error while running!", exception);
                try {
                    stop();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    private void trackpadControlLoop() throws Exception {
        LOGGER.info("Reading screen resolution...");
        byte[] touchscreenResolutionData = jniFunctions.i2cReadRegisterBytes(I2C_GT9110_ADDRESS_SLAVE,
                I2C_GT9110_ADDRESS_REGISTER_RESOLUTION, 4, false);
        touchscreenXResolution = (touchscreenResolutionData[1] << 8) | touchscreenResolutionData[0];
        touchscreenYResolution = (touchscreenResolutionData[3] << 8) | touchscreenResolutionData[2];
        LOGGER.info("Screen resolution: {}x{}", touchscreenXResolution, touchscreenYResolution);

        while (runLoop) {
            // Wait until touchscreen data is ready to be read
            byte coordinateStatusRegister;
            boolean bufferReady;
            do {
                coordinateStatusRegister = jniFunctions.i2cReadRegisterByte(I2C_GT9110_ADDRESS_SLAVE,
                        I2C_GT9110_ADDRESS_REGISTER_STATUS, false);
                bufferReady = ((coordinateStatusRegister & 0xFF) >> 7) == 1;

                if (!runLoop) {
                    return;
                }
            } while (!bufferReady);

            // Reset buffer status to trigger another touchscreen sample
            jniFunctions.i2cWriteRegisterByte(I2C_GT9110_ADDRESS_SLAVE, I2C_GT9110_ADDRESS_REGISTER_STATUS, (byte) 0,
                    false);

            // Read touch data
            int numberOfTouches = coordinateStatusRegister & 0x0F;
            byte[] touchscreenCoordinateData = jniFunctions.i2cReadRegisterBytes(I2C_GT9110_ADDRESS_SLAVE,
                    I2C_GT9110_ADDRESS_REGISTER_TOUCH_DATA, 8 * 10 /* 10 8-byte touch data */, false);
            TouchscreenTouch[] touchscreenTouches = new TouchscreenTouch[numberOfTouches];
            for (int touchIndex = 0; touchIndex < touchscreenTouches.length; touchIndex++) {
                int dataIndex = touchIndex * 8;
                int id = (touchscreenCoordinateData[dataIndex] & 0xFF);
                int x = ((touchscreenCoordinateData[dataIndex + 2] & 0xFF) << 8) |
                        (touchscreenCoordinateData[dataIndex + 1] & 0xFF);
                int y = ((touchscreenCoordinateData[dataIndex + 4] & 0xFF) << 8) |
                        (touchscreenCoordinateData[dataIndex + 3] & 0xFF);
                int size = ((touchscreenCoordinateData[dataIndex + 6] & 0xFF) << 8) |
                        (touchscreenCoordinateData[dataIndex + 5] & 0xFF);
                touchscreenTouches[touchIndex] = new TouchscreenTouch(id, x, y, size);
            }

            processTouchesForExperimentDRV2605(touchscreenTouches);
        }
    }

    //
    // BEGIN DRV2605 experiment
    //

    private void setupExperimentDRV2605() throws Exception {
        jniFunctions.i2cWriteRegisterByte(I2C_DRV2605L_ADDRESS, (byte) 0x01, (byte) 0x05, true);
        LOGGER.info("Set DRV2605L to RTP mode.");

        jniFunctions.i2cWriteRegisterByte(I2C_DRV2605L_ADDRESS, (byte) 0x02, (byte) 0x00, true);
        LOGGER.info("Set RTP register to zero.");

        jniFunctions.i2cWriteRegisterByte(I2C_DRV2605L_ADDRESS, (byte) 0x17, (byte) 0xFF, true);
        LOGGER.info("Set DRV2605L overdrive voltage-clamp to max value.");

        byte feedbackControlRegister = jniFunctions.i2cReadRegisterByte(I2C_DRV2605L_ADDRESS, (byte) 0x1A, true);
        feedbackControlRegister |= (1 << 7); // Set LRA mode
        jniFunctions.i2cWriteRegisterByte(I2C_DRV2605L_ADDRESS, (byte) 0x1A, feedbackControlRegister, true);
        LOGGER.info("Set DRV2605L into LRA mode.");

        byte control3Register = jniFunctions.i2cReadRegisterByte(I2C_DRV2605L_ADDRESS, (byte) 0x1D, true);
        control3Register |= 1; // Set LRA open-loop mode
        jniFunctions.i2cWriteRegisterByte(I2C_DRV2605L_ADDRESS, (byte) 0x1D, control3Register, true);
        LOGGER.info("Set DRV2605L into LRA open-loop mode.");
    }

    boolean sawZero;

    private void processTouchesForExperimentDRV2605(TouchscreenTouch[] touchscreenTouches) throws Exception {
        Platform.runLater(() -> {
            graphics.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
            graphics.setTextAlign(TextAlignment.LEFT);
            graphics.setTextBaseline(VPos.BOTTOM);
            graphics.fillText(Arrays.toString(touchscreenTouches), 0, canvas.getHeight() / 2);
        });

        if (touchscreenTouches.length > 0) {
            if (sawZero) {
                Thread.sleep(25);
                jniFunctions.i2cWriteRegisterByte(I2C_DRV2605L_ADDRESS, (byte) 0x02, (byte) 127, true);
                Thread.sleep(15);
                jniFunctions.i2cWriteRegisterByte(I2C_DRV2605L_ADDRESS, (byte) 0x02, (byte) 0, true);
                sawZero = false;
            }
        } else {
            jniFunctions.i2cWriteRegisterByte(I2C_DRV2605L_ADDRESS, (byte) 0x02, (byte) 0, true);
            sawZero = true;
        }
    }

    //
    // END DRV2605 experiment
    //

    @Override
    public void stop() throws Exception {
        LOGGER.info("Stopping...");

        runLoop = false;
        try {
            LOGGER.info("Stopping I2C interface...");
            try {
                jniFunctions.i2cStop();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            LOGGER.info("Stopped I2C interface.");
        } catch (Exception exception) {
            LOGGER.error("Could not stop successfully!", exception);
        }

        LOGGER.info("Stopped.");
        Platform.exit();
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
