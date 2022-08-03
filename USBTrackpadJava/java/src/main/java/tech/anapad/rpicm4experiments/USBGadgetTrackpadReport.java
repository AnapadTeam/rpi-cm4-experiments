package tech.anapad.rpicm4experiments;

public class USBGadgetTrackpadReport {

    private byte buttons;
    private byte x;
    private byte y;
    private byte wheel;

    public byte getButtons() {
        return buttons;
    }

    public void setButtons(byte buttons) {
        this.buttons = buttons;
    }

    public byte getX() {
        return x;
    }

    public void setX(byte x) {
        this.x = x;
    }

    public byte getY() {
        return y;
    }

    public void setY(byte y) {
        this.y = y;
    }

    public byte getWheel() {
        return wheel;
    }

    public void setWheel(byte wheel) {
        this.wheel = wheel;
    }
}
