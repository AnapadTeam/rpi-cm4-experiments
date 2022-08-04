package tech.anapad.rpicm4experiments;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.anapad.rpicm4experiments.jni.JNIFunctions;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;

import static java.nio.file.Files.createSymbolicLink;
import static tech.anapad.rpicm4experiments.util.FileUtil.createDirectoryPath;
import static tech.anapad.rpicm4experiments.util.FileUtil.fileWithByteContents;
import static tech.anapad.rpicm4experiments.util.FileUtil.fileWithStringContents;
import static tech.anapad.rpicm4experiments.util.FileUtil.removePath;
import static tech.anapad.rpicm4experiments.util.MathUtil.clamp;

/**
 * {@link USBTrackpadJava} is the main class for testing the LRA haptics via the DRV5605L using the RPi CM4 with Java.
 */
public class USBTrackpadJava extends Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(USBTrackpadJava.class);

    private static final short I2C_GT9110_ADDRESS_SLAVE = 0x5D;
    private static final short I2C_GT9110_ADDRESS_REGISTER_RESOLUTION = (short) 0x8146;
    private static final short I2C_GT9110_ADDRESS_REGISTER_STATUS = (short) 0x814E;
    private static final short I2C_GT9110_ADDRESS_REGISTER_TOUCH_DATA = (short) 0x814F;

    private static final String USB_GADGET_DEVICE_PATH = "/dev/hidg0";
    private static final String USB_GADGET_KERNEL_CONFIG_PATH = "/sys/kernel/config/usb_gadget/";
    private static final String USB_GADGET_NAME = "trackpad/";
    private static final String USB_GADGET_TRACKPAD_PATH = USB_GADGET_KERNEL_CONFIG_PATH + USB_GADGET_NAME;
    private static final String USB_GADGET_TRACKPAD_STRINGS_PATH = USB_GADGET_TRACKPAD_PATH + "strings/0x409/";
    private static final String USB_GADGET_TRACKPAD_CONFIGURATION_PATH = USB_GADGET_TRACKPAD_PATH + "configs/c.1/";
    private static final String USB_GADGET_TRACKPAD_CONFIGURATION_STRINGS_PATH =
            USB_GADGET_TRACKPAD_CONFIGURATION_PATH + "strings/0x409/";
    private static final String USB_GADGET_TRACKPAD_FUNCTIONS_NAME = "hid.0";
    private static final String USB_GADGET_TRACKPAD_FUNCTIONS_PATH =
            USB_GADGET_TRACKPAD_PATH + "functions/" + USB_GADGET_TRACKPAD_FUNCTIONS_NAME + "/";
    // This is the UDC specifically for the RPi CM4
    private static final String USB_DEVICE_CONTROLLER_NAME = "fe980000.usb";

    // Created using https://eleccelerator.com/usbdescreqparser/ and https://shorturl.at/hU034
    // (removed wake feature though)
    private static final int[] USB_GADGET_TRACKPAD_REPORT_DESCRIPTOR = {
            0x05, 0x01, // Usage Page (Generic Desktop)
            0x09, 0x02, // Usage (Mouse)
            0xa1, 0x01, // Collection (Application)
            0x09, 0x01, //   Usage (Pointer)
            0xa1, 0x00, //   Collection (Physical)
            0x05, 0x09, //     Usage Page (Button)
            0x19, 0x01, //     Usage Minimum (0x01)
            0x29, 0x03, //     Usage Maximum (0x03)
            0x15, 0x00, //     Logical Minimum (0)
            0x25, 0x01, //     Logical Maximum (1)
            0x95, 0x03, //     Report Count (3)
            0x75, 0x01, //     Report Size (1)
            0x81, 0x02, //     Input (Data,Var,Abs)
            0x95, 0x01, //     Report Count (1)
            0x75, 0x05, //     Report Size (5)
            0x81, 0x03, //     Input (Const,Array,Abs)
            0x05, 0x01, //     Usage Page (Generic Desktop)
            0x09, 0x30, //     Usage (X)
            0x09, 0x31, //     Usage (Y)
            0x09, 0x38, //     Usage (Wheel)
            0x15, 0x81, //     Logical Minimum (-127)
            0x25, 0x7f, //     Logical Maximum (127)
            0x75, 0x08, //     Report Size (8)
            0x95, 0x03, //     Report Count (3)
            0x81, 0x06, //     Input (Data,Var,Rel)
            0xc0, //         End Collection
            0xc0 //        End Collection
    };
    private static final byte[] USB_GADGET_TRACKPAD_REPORT_DESCRIPTOR_BYTES =
            new byte[USB_GADGET_TRACKPAD_REPORT_DESCRIPTOR.length];

    static {
        for (int index = 0; index < USB_GADGET_TRACKPAD_REPORT_DESCRIPTOR_BYTES.length; index++) {
            USB_GADGET_TRACKPAD_REPORT_DESCRIPTOR_BYTES[index] =
                    (byte) (USB_GADGET_TRACKPAD_REPORT_DESCRIPTOR[index] & 0xFF);
        }
    }

    private JNIFunctions jniFunctions;
    private Canvas canvas;
    private GraphicsContext graphics;
    private boolean runLoop;
    private FileOutputStream hidGadgetOutputStream;
    private USBGadgetTrackpadReport usbGadgetTrackpadReport;
    private int xResolution;
    private int yResolution;

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

        LOGGER.info("Creating UI...");
        canvas = new Canvas(1920, 515);
        graphics = canvas.getGraphicsContext2D();
        graphics.setFont(Font.font(32));
        Scene scene = new Scene(new Pane(canvas), 1920, 515);
        scene.setFill(Color.BLACK);
        scene.setCursor(Cursor.NONE);
        primaryStage.setScene(scene);
        primaryStage.show();
        LOGGER.info("Created UI.");

        LOGGER.info("Starting I2C interface...");
        jniFunctions.i2cStart();
        LOGGER.info("Started I2C interface.");

        LOGGER.info("Creating the USB HID mouse gadget...");
        createUSBTrackpad();
        LOGGER.info("Created the USB HID mouse gadget.");

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

    private void createUSBTrackpad() {
        createDirectoryPath(USB_GADGET_TRACKPAD_PATH);

        fileWithStringContents(USB_GADGET_TRACKPAD_PATH + "idVendor", "0x1d6b"); // Linux Foundation
        fileWithStringContents(USB_GADGET_TRACKPAD_PATH + "idProduct", "0x0104"); // Multifunction Composite Gadget
        fileWithStringContents(USB_GADGET_TRACKPAD_PATH + "bcdDevice", "0x0100"); // v1.0.0
        fileWithStringContents(USB_GADGET_TRACKPAD_PATH + "bcdUSB", "0x0200"); // USB2

        createDirectoryPath(USB_GADGET_TRACKPAD_STRINGS_PATH);
        fileWithStringContents(USB_GADGET_TRACKPAD_STRINGS_PATH + "serialnumber", "a1b2c3d4e5");
        fileWithStringContents(USB_GADGET_TRACKPAD_STRINGS_PATH + "manufacturer", "Anapad Team");
        fileWithStringContents(USB_GADGET_TRACKPAD_STRINGS_PATH + "product", "Anapad");

        createDirectoryPath(USB_GADGET_TRACKPAD_CONFIGURATION_STRINGS_PATH);
        fileWithStringContents(USB_GADGET_TRACKPAD_CONFIGURATION_STRINGS_PATH + "configuration", "Anapad Config");
        fileWithStringContents(USB_GADGET_TRACKPAD_CONFIGURATION_PATH + "MaxPower", "250"); // 250mA

        createDirectoryPath(USB_GADGET_TRACKPAD_FUNCTIONS_PATH);
        fileWithStringContents(USB_GADGET_TRACKPAD_FUNCTIONS_PATH + "protocol", "2"); // 1=keyboard, 2=mouse
        fileWithStringContents(USB_GADGET_TRACKPAD_FUNCTIONS_PATH + "subclass", "1"); // 0 = no boot, 1 = boot
        fileWithStringContents(USB_GADGET_TRACKPAD_FUNCTIONS_PATH + "report_length", "4");
        fileWithByteContents(USB_GADGET_TRACKPAD_FUNCTIONS_PATH + "report_desc",
                USB_GADGET_TRACKPAD_REPORT_DESCRIPTOR_BYTES);
        try {
            createSymbolicLink(Paths.get(USB_GADGET_TRACKPAD_CONFIGURATION_PATH + USB_GADGET_TRACKPAD_FUNCTIONS_NAME),
                    Paths.get(USB_GADGET_TRACKPAD_FUNCTIONS_PATH));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Enable the gadget
        fileWithStringContents(USB_GADGET_TRACKPAD_PATH + "UDC", USB_DEVICE_CONTROLLER_NAME);

        try {
            hidGadgetOutputStream = new FileOutputStream(USB_GADGET_DEVICE_PATH);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        usbGadgetTrackpadReport = new USBGadgetTrackpadReport();
    }

    private void writeUSBGadgetTrackpadReport() throws IOException {
        hidGadgetOutputStream.write(new byte[]{
                usbGadgetTrackpadReport.getButtons(),
                usbGadgetTrackpadReport.getX(),
                usbGadgetTrackpadReport.getY(),
                usbGadgetTrackpadReport.getWheel()
        });
        hidGadgetOutputStream.flush();
    }

    private void destroyUSBTrackpad() {
        try {
            if (hidGadgetOutputStream != null) {
                hidGadgetOutputStream.close();
            }

            fileWithStringContents(USB_GADGET_TRACKPAD_PATH + "UDC", "");
            removePath(USB_GADGET_TRACKPAD_CONFIGURATION_PATH + USB_GADGET_TRACKPAD_FUNCTIONS_NAME);
            removePath(USB_GADGET_TRACKPAD_CONFIGURATION_STRINGS_PATH);
            removePath(USB_GADGET_TRACKPAD_CONFIGURATION_PATH);
            removePath(USB_GADGET_TRACKPAD_FUNCTIONS_PATH);
            removePath(USB_GADGET_TRACKPAD_STRINGS_PATH);
            removePath(USB_GADGET_TRACKPAD_PATH);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private void trackpadControlLoop() throws Exception {
        LOGGER.info("Reading screen resolution...");
        byte[] touchscreenResolutionData = jniFunctions.i2cReadRegisterBytes(I2C_GT9110_ADDRESS_SLAVE,
                I2C_GT9110_ADDRESS_REGISTER_RESOLUTION, 4);
        xResolution = (touchscreenResolutionData[1] << 8) | touchscreenResolutionData[0];
        yResolution = (touchscreenResolutionData[3] << 8) | touchscreenResolutionData[2];
        LOGGER.info("Screen resolution: {}x{}", xResolution, yResolution);

        final int registerTouchDataLength = 8 * 10; // 10 8-byte touch data
        int touchscreenTouchLastX = -1;
        int touchscreenTouchLastY = -1;
        boolean touchscreenTouchDownDeltaNonZero = false;
        boolean touchscreenMultiTouchedDown = false;
        int touchscreenLastTouchCount = 0;
        int touchscreenWheelMoveCount = 0;
        final int wheelMoveCountIncrementThreshold = 110;
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

            // Read touch data
            int numberOfTouches = coordinateStatusRegister & 0x0F;
            byte[] touchscreenCoordinateData = jniFunctions.i2cReadRegisterBytes(I2C_GT9110_ADDRESS_SLAVE,
                    I2C_GT9110_ADDRESS_REGISTER_TOUCH_DATA, registerTouchDataLength);
            drawTouches(touchscreenCoordinateData, numberOfTouches);

            if (!touchscreenMultiTouchedDown) {
                touchscreenMultiTouchedDown = numberOfTouches >= 2;
            }

            if (numberOfTouches > 0) {
                int x = ((touchscreenCoordinateData[2] & 0xFF) << 8) | (touchscreenCoordinateData[1] & 0xFF);
                int y = ((touchscreenCoordinateData[4] & 0xFF) << 8) | (touchscreenCoordinateData[3] & 0xFF);
                LOGGER.info("Touchscreen touch 0: x={} y={}", x, y);

                if (touchscreenTouchLastX == -1 || touchscreenTouchLastY == -1) {
                    // Set last touch coordinates to current touch coordinates
                    touchscreenTouchLastX = x;
                    touchscreenTouchLastY = y;
                } else {
                    // Calculate deltas
                    int deltaX = x - touchscreenTouchLastX;
                    int deltaY = y - touchscreenTouchLastY;
                    int multipliedDeltaX = (int) Math.round((double) deltaX / 5);
                    int multipliedDeltaY = (int) Math.round((double) deltaY / 5);

                    // Clamp deltas
                    deltaX = clamp(deltaX, Byte.MIN_VALUE, Byte.MAX_VALUE);
                    deltaY = clamp(deltaY, Byte.MIN_VALUE, Byte.MAX_VALUE);
                    multipliedDeltaX = clamp(multipliedDeltaX, Byte.MIN_VALUE, Byte.MAX_VALUE);
                    multipliedDeltaY = clamp(multipliedDeltaY, Byte.MIN_VALUE, Byte.MAX_VALUE);

                    // Write "trackpad" report
                    usbGadgetTrackpadReport.setButtons((byte) 0);
                    if (touchscreenMultiTouchedDown) {
                        usbGadgetTrackpadReport.setX((byte) 0);
                        usbGadgetTrackpadReport.setY((byte) 0);

                        if ((touchscreenWheelMoveCount += Math.abs(deltaY)) >= wheelMoveCountIncrementThreshold) {
                            touchscreenWheelMoveCount = 0;
                            usbGadgetTrackpadReport.setWheel((byte) -clamp(deltaY, -1, 1));
                        } else {
                            usbGadgetTrackpadReport.setWheel((byte) 0);
                        }
                    } else {
                        usbGadgetTrackpadReport.setX((byte) multipliedDeltaX);
                        usbGadgetTrackpadReport.setY((byte) multipliedDeltaY);
                        usbGadgetTrackpadReport.setWheel((byte) 0);
                    }
                    writeUSBGadgetTrackpadReport();

                    // Set last touch coordinates to current touch coordinates
                    if (multipliedDeltaX != 0) {
                        touchscreenTouchLastX = x;
                    }
                    if (multipliedDeltaY != 0) {
                        touchscreenTouchLastY = y;
                    }

                    // Trigger non-zero delta touch as needed
                    if (!touchscreenTouchDownDeltaNonZero && deltaX != 0 && deltaY != 0) {
                        touchscreenTouchDownDeltaNonZero = true;
                    }

                    LOGGER.info("Touchpad touch data sent: buttons={} x={} y={} wheel={}\n",
                            usbGadgetTrackpadReport.getButtons(), usbGadgetTrackpadReport.getX(),
                            usbGadgetTrackpadReport.getY(), usbGadgetTrackpadReport.getWheel());
                }
            } else {
                // Reset report
                usbGadgetTrackpadReport.setX((byte) 0);
                usbGadgetTrackpadReport.setY((byte) 0);
                usbGadgetTrackpadReport.setWheel((byte) 0);

                // Handle non-zero delta touch down/up (aka button clicks)
                if (touchscreenTouchDownDeltaNonZero) {
                    touchscreenTouchDownDeltaNonZero = false;
                    usbGadgetTrackpadReport.setButtons((byte) 0);
                } else if (touchscreenLastTouchCount != 0) {
                    if (touchscreenMultiTouchedDown) {
                        usbGadgetTrackpadReport.setButtons((byte) (0x01 << 1));
                        LOGGER.info("Touchpad right-click sent.");
                    } else {
                        usbGadgetTrackpadReport.setButtons((byte) 0x01);
                        LOGGER.info("Touchpad left-click sent.");
                    }
                    writeUSBGadgetTrackpadReport();
                    usbGadgetTrackpadReport.setButtons((byte) 0);
                }

                // Reset variables
                touchscreenTouchLastX = -1;
                touchscreenTouchLastY = -1;
                touchscreenMultiTouchedDown = false;

                writeUSBGadgetTrackpadReport();
            }

            touchscreenLastTouchCount = numberOfTouches;
        }
    }

    private void drawTouches(byte[] touchscreenCoordinateData, int numberOfTouches) {
        Platform.runLater(() -> {
            graphics.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
            Stop[] stops = new Stop[]{
                    new Stop(0d * (1d / 3d), Color.rgb(237, 55, 58)),
                    new Stop(1d * (1d / 3d), Color.rgb(199, 89, 190)),
                    new Stop(2d * (1d / 3d), Color.rgb(54, 124, 224)),
                    new Stop(3d * (1d / 3d), Color.rgb(51, 221, 106))};
            graphics.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE, stops));
            graphics.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        });
        final int touchDataLength = 8;
        for (int index = 0; index < touchscreenCoordinateData.length; index += touchDataLength) {
            if (index / touchDataLength < numberOfTouches) {
                int id = (touchscreenCoordinateData[index] & 0xFF);
                int x = ((touchscreenCoordinateData[index + 2] & 0xFF) << 8) |
                        (touchscreenCoordinateData[index + 1] & 0xFF);
                int y = ((touchscreenCoordinateData[index + 4] & 0xFF) << 8) |
                        (touchscreenCoordinateData[index + 3] & 0xFF);
                int size = ((touchscreenCoordinateData[index + 6] & 0xFF) << 8) |
                        (touchscreenCoordinateData[index + 5] & 0xFF);
                Platform.runLater(() -> {
                    // Draw touchscreen touch oval
                    final double xRatio = canvas.getWidth() / xResolution;
                    final double yRatio = canvas.getHeight() / yResolution;
                    final double radius = size * 3;
                    final double drawX = x * xRatio - radius;
                    final double drawY = y * yRatio - radius;
                    graphics.setFill(Color.AQUA);
                    graphics.fillOval(drawX, drawY, radius * 2, radius * 2);

                    // Draw touchscreen number
                    graphics.setFill(Color.WHITE);
                    graphics.setTextAlign(TextAlignment.CENTER);
                    graphics.setTextBaseline(VPos.BOTTOM);
                    graphics.fillText(String.valueOf(id), drawX + radius, drawY - 10);
                });
            }
        }
    }

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

            LOGGER.info("Stopping the USB HID mouse gadget...");
            try {
                destroyUSBTrackpad();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            LOGGER.info("Stopped the USB HID mouse gadget.");
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
