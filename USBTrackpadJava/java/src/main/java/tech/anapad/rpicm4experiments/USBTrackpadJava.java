package tech.anapad.rpicm4experiments;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link USBTrackpadJava} is the main class for testing the LRA haptics via the DRV5605L using the RPi CM4 with Java.
 */
public class USBTrackpadJava extends Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(USBTrackpadJava.class);

    /**
     * The I2C address of the GT9110 chip.
     */
    public static final short I2C_GT9110_ADDRESS = 0x5D;

    // private final JNIFunctions jniFunctions;
    // private boolean shutdownGracefully;

    @Override
    public void init() throws Exception {

    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Canvas canvas = new Canvas(1920, 515);
        GraphicsContext graphicsContext = canvas.getGraphicsContext2D();
        String javaVersion = System.getProperty("java.version");
        String javafxVersion = System.getProperty("javafx.version");
        // graphicsContext.strokeText("Hello, JavaFX " + javafxVersion + ", running on Java " + javaVersion + ".", 0,0);

        // Media media = new Media(getClass().getClassLoader().getResource("Untitled.mp4").toExternalForm());
        // javafx.scene.media.MediaPlayer player = new javafx.scene.media.MediaPlayer(media);
        // MediaView viewer = new MediaView(player);
        // player.play();

        Scene scene = new Scene(new Pane(canvas), 1920, 515);
        primaryStage.setScene(scene);
        primaryStage.show();

        new Thread(() -> {
            // Lissajous curve
            final double centerX = canvas.getWidth() / 2;
            final double centerY = canvas.getHeight() / 2;
            final double r = 200;
            double x = 0;
            double y = 0;
            while (!Thread.currentThread().isInterrupted()) {
                x += 0.01 * 5;
                y += 0.01 * 7;

                double xCopy = x;
                double yCopy = y;
                Platform.runLater(() ->
                        graphicsContext.fillOval(centerX + Math.cos(xCopy) * r, centerY + Math.sin(yCopy) * r, 10, 10));
                try {
                    Thread.sleep(17);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            // Show some colors!
            // int x = 0;
            // int y = 0;
            // while (!Thread.currentThread().isInterrupted()) {
            //     if (x >= 1920) {
            //         x = 0;
            //         y += 30;
            //     }
            //     x += 15;
            //     int xCopy = x;
            //     int yCopy = y;
            //     Platform.runLater(() -> {
            //         for (int i = 0; i < 100; i++) {
            //             graphicsContext.setFill(Color.rgb((int) (Math.random() * 255),
            //                     (int) (Math.random() * 255), (int) (Math.random() * 255)));
            //             graphicsContext.fillOval(xCopy, yCopy, 30, 30);
            //         }
            //     });
            //     try {
            //         Thread.sleep(17);
            //     } catch (InterruptedException e) {
            //         throw new RuntimeException(e);
            //     }
            // }
        }).start();
    }

    @Override
    public void stop() throws Exception {

    }

    // /**
    //  * Instantiates a new {@link USBTrackpadJava}.
    //  */
    // public USBTrackpadJava() {
    //     Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    //     jniFunctions = new JNIFunctions();
    //     shutdownGracefully = false;
    // }
    //
    // /**
    //  * Starts {@link USBTrackpadJava}.
    //  */
    // public void start() throws Exception {
    //     LOGGER.info("Starting...");
    //
    //     jniFunctions.i2cStart();
    //     LOGGER.info("Started I2C interface.");
    //
    //     LOGGER.info("Started");
    //     LOGGER.info("Running...");
    //
    //     // TODO
    // }
    //
    // /**
    //  * Stops {@link USBTrackpadJava}.
    //  */
    // public void stop() {
    //     if (shutdownGracefully) {
    //         return;
    //     }
    //
    //     LOGGER.info("Stopping...");
    //
    //     try {
    //         jniFunctions.i2cStop();
    //         LOGGER.info("Stopped I2C interface.");
    //     } catch (Exception exception) {
    //         LOGGER.error("Could not stop successfully!", exception);
    //     }
    //
    //     LOGGER.info("Stopped.");
    //     shutdownGracefully = true;
    // }

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     */
    public static void main(String[] args) throws Exception {
        launch(args);
    }
}
