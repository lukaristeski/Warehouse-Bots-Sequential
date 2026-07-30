package Sequential;

public class Shelf {
    private final Location topLeft;
    private final boolean[][] slots = new boolean[2][8];

    public Shelf(Location topLeft) {
        this.topLeft = topLeft;
        for (int slot = 0 ; slot < 16; slot++) {
            int r = slot / 8;
            int c = slot % 8;
            slots[r][c] = slot < 8;
        }
    }

    public Location getLocation() { return topLeft; }

    private int row(int slot) { return slot / 8; }
    private int col(int slot) { return slot % 8; }

    public boolean pick(int slot) {
        int r = row(slot), c = col(slot);
        if (!slots[r][c]) return false;
        slots[r][c] = false;
        return true;
    }

    public boolean drop(int slot) {
        int r = row(slot), c = col(slot);
        if (slots[r][c]) return false;
        slots[r][c] = true;
        return true;
    }
}
