package tech.anapad.rpicm4experiments;

public class TouchscreenTouch {

    private int id;
    private int x;
    private int y;
    private int size;

    public TouchscreenTouch(int id, int x, int y, int size) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.size = size;
    }

    public int getID() {
        return id;
    }

    public void setID(int id) {
        this.id = id;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    @Override
    public String toString() {
        return "TouchscreenTouch{" +
                "id=" + id +
                ", x=" + x +
                ", y=" + y +
                ", size=" + size +
                '}';
    }
}
