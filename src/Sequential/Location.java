package Sequential;

import java.util.Objects;

public class Location {
    private final int x;
    private final int y;

    public Location(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() { return x; }
    public int getY() { return y; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Location)) return false;
        Location that = (Location) o;
        return x == that.x && y == that.y;
    }

    @Override public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override public String toString() {
        return "(" + x + "," + y + ")";
    }
}
