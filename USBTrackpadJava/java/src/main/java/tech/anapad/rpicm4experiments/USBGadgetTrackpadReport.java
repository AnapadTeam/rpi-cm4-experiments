package tech.anapad.rpicm4experiments;

public class USBGadgetTrackpadReport {

    private byte buttons;
    private byte x;
    private byte y;
    private byte wheel;
    private byte modifier;
    private byte reserved;
    private byte keycode1;
    private byte keycode2;
    private byte keycode3;
    private byte keycode4;
    private byte keycode5;
    private byte keycode6;

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

    public byte getModifier() {
        return modifier;
    }

    public void setModifier(byte modifier) {
        this.modifier = modifier;
    }

    public byte getReserved() {
        return reserved;
    }

    public void setReserved(byte reserved) {
        this.reserved = reserved;
    }

    public byte getKeycode1() {
        return keycode1;
    }

    public void setKeycode1(byte keycode1) {
        this.keycode1 = keycode1;
    }

    public byte getKeycode2() {
        return keycode2;
    }

    public void setKeycode2(byte keycode2) {
        this.keycode2 = keycode2;
    }

    public byte getKeycode3() {
        return keycode3;
    }

    public void setKeycode3(byte keycode3) {
        this.keycode3 = keycode3;
    }

    public byte getKeycode4() {
        return keycode4;
    }

    public void setKeycode4(byte keycode4) {
        this.keycode4 = keycode4;
    }

    public byte getKeycode5() {
        return keycode5;
    }

    public void setKeycode5(byte keycode5) {
        this.keycode5 = keycode5;
    }

    public byte getKeycode6() {
        return keycode6;
    }

    public void setKeycode6(byte keycode6) {
        this.keycode6 = keycode6;
    }
}
