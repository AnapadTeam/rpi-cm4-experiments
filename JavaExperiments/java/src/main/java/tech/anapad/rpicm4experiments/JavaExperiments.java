package tech.anapad.rpicm4experiments;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.anapad.rpicm4experiments.util.BitUtil;

import static tech.anapad.rpicm4experiments.jni.JNIFunctions.i2cReadByte;
import static tech.anapad.rpicm4experiments.jni.JNIFunctions.i2cReadRegisterByte;
import static tech.anapad.rpicm4experiments.jni.JNIFunctions.i2cReadRegisterBytes;
import static tech.anapad.rpicm4experiments.jni.JNIFunctions.i2cRegisterBitGet;
import static tech.anapad.rpicm4experiments.jni.JNIFunctions.i2cRegisterBitReset;
import static tech.anapad.rpicm4experiments.jni.JNIFunctions.i2cRegisterBitSet;
import static tech.anapad.rpicm4experiments.jni.JNIFunctions.i2cRegisterBitsSet;
import static tech.anapad.rpicm4experiments.jni.JNIFunctions.i2cStart;
import static tech.anapad.rpicm4experiments.jni.JNIFunctions.i2cStop;
import static tech.anapad.rpicm4experiments.jni.JNIFunctions.i2cWriteByte;
import static tech.anapad.rpicm4experiments.jni.JNIFunctions.i2cWriteRegisterByte;
import static tech.anapad.rpicm4experiments.util.BitUtil.setBit;

/**
 * {@link JavaExperiments} is the main class for testing the LRA haptics via the DRV5605L using the RPi CM4 with Java.
 */
public class JavaExperiments extends Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaExperiments.class);

    private static final short I2C_GT9110_ADDRESS_SLAVE = 0x5D;
    private static final short I2C_GT9110_ADDRESS_REGISTER_RESOLUTION = (short) 0x8146;
    private static final short I2C_GT9110_ADDRESS_REGISTER_STATUS = (short) 0x814E;
    private static final short I2C_GT9110_ADDRESS_REGISTER_TOUCH_DATA = (short) 0x814F;

    private static final short I2C_DRV2605L_ADDRESS = 0x5A;

    private static final short I2C_NAU7802_ADDRESS = 0x2A;

    private static final short I2C_TCA9548A_ADDRESS = 0x70;

    private boolean runLoop;
    private Canvas canvas;
    private GraphicsContext graphics;
    private int touchscreenXResolution;
    private int touchscreenYResolution;

    @Override
    public void init() {}

    XYChart.Series<Number, Number> series;

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

        final NumberAxis xAxis = new NumberAxis();
        final NumberAxis yAxis = new NumberAxis();
        series = new XYChart.Series<>();
        final LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.getData().add(series);
        lineChart.setTitle("ADC Values");

        Scene scene = new Scene(new StackPane(lineChart), 1920, 515);
        scene.setFill(Color.WHITE);
        scene.setCursor(Cursor.NONE);
        primaryStage.setScene(scene);
        primaryStage.show();
        LOGGER.info("Created UI.");

        LOGGER.info("Starting I2C interface...");
        i2cStart();
        LOGGER.info("Started I2C interface.");

        LOGGER.info("Setting up experiment...");
        setupExperimentDRV2605();
        // setupExperimentAnalog();
        // setupExperimentAnalogMultiplexer();
        LOGGER.info("Set up experiment.");

        LOGGER.info("Started");

        new Thread(() -> {
            try {
                LOGGER.info("Running...");
                runLoop = true;
                // touchscreenReadLoop();
                // analogExperimentLoop();
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

    private void touchscreenReadLoop() throws Exception {
        LOGGER.info("Reading screen resolution...");
        byte[] touchscreenResolutionData = i2cReadRegisterBytes(I2C_GT9110_ADDRESS_SLAVE,
                I2C_GT9110_ADDRESS_REGISTER_RESOLUTION, 4, false);
        touchscreenXResolution = (touchscreenResolutionData[1] << 8) | touchscreenResolutionData[0];
        touchscreenYResolution = (touchscreenResolutionData[3] << 8) | touchscreenResolutionData[2];
        LOGGER.info("Screen resolution: {}x{}", touchscreenXResolution, touchscreenYResolution);

        while (runLoop) {
            // Wait until touchscreen data is ready to be read
            byte coordinateStatusRegister;
            boolean bufferReady;
            do {
                coordinateStatusRegister = i2cReadRegisterByte(I2C_GT9110_ADDRESS_SLAVE,
                        I2C_GT9110_ADDRESS_REGISTER_STATUS, false);
                bufferReady = ((coordinateStatusRegister & 0xFF) >> 7) == 1;

                if (!runLoop) {
                    return;
                }
            } while (!bufferReady);

            // Reset buffer status to trigger another touchscreen sample
            i2cWriteRegisterByte(I2C_GT9110_ADDRESS_SLAVE, I2C_GT9110_ADDRESS_REGISTER_STATUS, (byte) 0,
                    false);

            // Read touch data
            int numberOfTouches = coordinateStatusRegister & 0x0F;
            byte[] touchscreenCoordinateData = i2cReadRegisterBytes(I2C_GT9110_ADDRESS_SLAVE,
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
        i2cWriteRegisterByte(I2C_DRV2605L_ADDRESS, (byte) 0x01, (byte) 0x05, true);
        LOGGER.info("Set DRV2605L to RTP mode.");

        i2cWriteRegisterByte(I2C_DRV2605L_ADDRESS, (byte) 0x02, (byte) 0x00, true);
        LOGGER.info("Set RTP register to zero.");

        i2cWriteRegisterByte(I2C_DRV2605L_ADDRESS, (byte) 0x17, (byte) 0xFF, true);
        LOGGER.info("Set DRV2605L overdrive voltage-clamp to max value.");

        byte feedbackControlRegister = i2cReadRegisterByte(I2C_DRV2605L_ADDRESS, (byte) 0x1A, true);
        feedbackControlRegister |= (1 << 7); // Set LRA mode
        i2cWriteRegisterByte(I2C_DRV2605L_ADDRESS, (byte) 0x1A, feedbackControlRegister, true);
        LOGGER.info("Set DRV2605L into LRA mode.");

        byte control3Register = i2cReadRegisterByte(I2C_DRV2605L_ADDRESS, (byte) 0x1D, true);
        control3Register |= 1; // Set LRA open-loop mode
        // control3Register = (byte) setBit(control3Register, 0, 0);
        i2cWriteRegisterByte(I2C_DRV2605L_ADDRESS, (byte) 0x1D, control3Register, true);
        LOGGER.info("Set DRV2605L into LRA open-loop mode.");

        i2cWriteRegisterByte((short) 0x23, (byte) 0x03, (byte) 0x00, true);
        i2cWriteRegisterByte((short) 0x23, (byte) 0x01, (byte) 0b0010_0000, true);
        LOGGER.info("VALUE: {}", i2cReadRegisterByte((short) 0x23, (byte) 0x00, true));

        i2cWriteRegisterByte(I2C_DRV2605L_ADDRESS, (byte) 0x02, (byte) 127, true);
        Thread.sleep(1000);
        i2cWriteRegisterByte(I2C_DRV2605L_ADDRESS, (byte) 0x02, (byte) 0, true);

        i2cWriteRegisterByte((short) 0x23, (byte) 0x03, (byte) 0x00, true);
        i2cWriteRegisterByte((short) 0x23, (byte) 0x01, (byte) 0b0000_0000, true);
        LOGGER.info("VALUE: {}", i2cReadRegisterByte((short) 0x23, (byte) 0x00, true));

        i2cWriteRegisterByte((short) 0x21, (byte) 0x03, (byte) 0x00, true);
        i2cWriteRegisterByte((short) 0x21, (byte) 0x01, (byte) 0b0010_0000, true);
        LOGGER.info("VALUE: {}", i2cReadRegisterByte((short) 0x21, (byte) 0x00, true));

        i2cWriteRegisterByte(I2C_DRV2605L_ADDRESS, (byte) 0x02, (byte) 127, true);
        Thread.sleep(1000);
        i2cWriteRegisterByte(I2C_DRV2605L_ADDRESS, (byte) 0x02, (byte) 0, true);
    }

    boolean sawZero;

    private void processTouchesForExperimentDRV2605(TouchscreenTouch[] touchscreenTouches) throws Exception {
        Platform.runLater(() -> {
            Platform.runLater(() -> {
                graphics.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
                graphics.setTextAlign(TextAlignment.LEFT);
                graphics.setTextBaseline(VPos.BOTTOM);
                int startY = 0;
                for (TouchscreenTouch touchscreenTouch : touchscreenTouches) {
                    graphics.fillText(touchscreenTouch.toString(), 0, startY += 20);
                }
            });
        });

        if (touchscreenTouches.length > 0) {
            if (sawZero) {
                Thread.sleep(25);
                i2cWriteRegisterByte(I2C_DRV2605L_ADDRESS, (byte) 0x02, (byte) 127, true);
                Thread.sleep(100);
                i2cWriteRegisterByte(I2C_DRV2605L_ADDRESS, (byte) 0x02, (byte) 0, true);
                sawZero = false;
            }
        } else {
            i2cWriteRegisterByte(I2C_DRV2605L_ADDRESS, (byte) 0x02, (byte) 0, true);
            sawZero = true;
        }
    }

    //
    // END DRV2605 experiment
    //

    //
    // BEGIN Load cell experiment
    //

    private void setupExperimentAnalog() throws Exception {
        LOGGER.info("Setting up NAU7802.");

        // Reset chip
        i2cRegisterBitSet(I2C_NAU7802_ADDRESS, (byte) 0x00, true, 0); // RR

        // Put chip back into normal mode
        i2cRegisterBitReset(I2C_NAU7802_ADDRESS, (byte) 0x00, true, 0); // RR
        Thread.sleep(1);
        i2cRegisterBitSet(I2C_NAU7802_ADDRESS, (byte) 0x00, true, 1); // PUD
        i2cRegisterBitSet(I2C_NAU7802_ADDRESS, (byte) 0x00, true, 2); // PUA

        // Wait for power up (PUR)
        while (!i2cRegisterBitGet(I2C_NAU7802_ADDRESS, (byte) 0x00, true, 3)) {}

        // Configure chip
        i2cRegisterBitsSet(I2C_NAU7802_ADDRESS, (byte) 0x01, true, 0b111, 2, 0); // Gain = x128
        i2cRegisterBitsSet(I2C_NAU7802_ADDRESS, (byte) 0x01, true, 0b101, 5, 3); // LDO = 3.0V
        i2cRegisterBitSet(I2C_NAU7802_ADDRESS, (byte) 0x00, true, 7); // AVDDS
        i2cRegisterBitsSet(I2C_NAU7802_ADDRESS, (byte) 0x02, true, 0b111, 6, 4); // Sample rate = 320 sps

        // Calibrate chip
        i2cRegisterBitSet(I2C_NAU7802_ADDRESS, (byte) 0x02, true, 2);
        int iteration = 0;
        while (i2cRegisterBitGet(I2C_NAU7802_ADDRESS, (byte) 0x02, true, 2)) {
            iteration++;
            if (iteration > 10000) {
                LOGGER.info("NAU7802 calibration failed.");
                Platform.exit();
                return;
            }
        }

        LOGGER.info("Set up NAU7802.");
    }

    int x = 0;

    private void analogExperimentLoop() throws Exception {
        while (runLoop) {
            final int samples = 40;
            double sum = 0;
            for (int i = 0; i < samples; i++) {
                while (!i2cRegisterBitGet(I2C_NAU7802_ADDRESS, (byte) 0x00, true, 5)) {}
                byte[] adcData = i2cReadRegisterBytes(I2C_NAU7802_ADDRESS, (byte) 0x12, 3, true);
                int adcValue = (adcData[0] & 0xFF) << 16 | (adcData[1] & 0xFF) << 8 | (adcData[2] & 0xFF);
                adcValue = adcValue << 8;
                adcValue = adcValue >> 8;
                sum += adcValue;
            }
            final double sumFinal = sum;
            Platform.runLater(() -> {
                series.getData().add(new XYChart.Data<>(x++, sumFinal / samples));
                // graphics.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
                // graphics.setTextAlign(TextAlignment.LEFT);
                // graphics.setTextBaseline(VPos.BOTTOM);
                // graphics.fillText(String.valueOf(sumFinal / samples), 0, canvas.getHeight() / 2);
            });
        }
    }

    //
    // END Load cell experiment
    //

    //
    // BEGIN Analog multiplexer experiment
    //

    private void setupExperimentAnalogMultiplexer() throws Exception {
        i2cWriteByte(I2C_TCA9548A_ADDRESS, (byte) 0b0100_0000);

        // setupExperimentDRV2605();

        i2cWriteRegisterByte(I2C_DRV2605L_ADDRESS, (byte) 0x02, (byte) 127, true);
        Thread.sleep(5000);
        i2cWriteRegisterByte(I2C_DRV2605L_ADDRESS, (byte) 0x02, (byte) 0, true);

        // i2cWriteByte(I2C_TCA9548A_ADDRESS, (byte) 0b1000_0000);
        // LOGGER.info("Register: {}", i2cReadRegisterByte(I2C_DRV2605L_ADDRESS, (short) 0x02, true));
        //
        // i2cWriteByte(I2C_TCA9548A_ADDRESS, (byte) 0b0100_0000);
        // LOGGER.info("Register: {}", i2cReadRegisterByte(I2C_DRV2605L_ADDRESS, (short) 0x02, true));
    }

    //
    // END Analog multiplexer experiment
    //

    @Override
    public void stop() throws Exception {
        LOGGER.info("Stopping...");

        runLoop = false;
        try {
            LOGGER.info("Stopping I2C interface...");
            try {
                i2cStop();
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
