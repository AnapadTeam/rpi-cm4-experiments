module USBTrackpadJava {
    requires org.slf4j;
    requires javafx.base;
    requires javafx.graphics;
    requires javafx.controls;
    requires javafx.media;

    exports tech.anapad.rpicm4experiments to javafx.graphics;
}
