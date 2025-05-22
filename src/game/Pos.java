package src.game;

public class Pos {
    private int x, y;

    public Pos(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setX(int value) {
        x = value;
    }

    public void setY(int value) {
        y = value;
    }

    public void incrementX(int amt) {
        x += amt;
    }

    public void incrementY(int amt) {
        y += amt;
    }

    public boolean equalTo(Pos other) {
        return (this.x == other.getX() && this.y == other.getY());
    }
}
