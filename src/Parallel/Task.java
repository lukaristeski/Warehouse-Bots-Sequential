package Parallel;

public class Task {

    private final int pickupShelfX, pickupShelfY, pickupSlot;
    private final int dropShelfX, dropShelfY, dropSlot;

    public Task(int pickupShelfX, int pickupShelfY, int pickupSlot,
                int dropShelfX, int dropShelfY, int dropSlot) {
        this.pickupShelfX = pickupShelfX;
        this.pickupShelfY = pickupShelfY;
        this.pickupSlot = pickupSlot;
        this.dropShelfX = dropShelfX;
        this.dropShelfY = dropShelfY;
        this.dropSlot = dropSlot;
    }

    public int getPickupX() { return pickupShelfX; }
    public int getPickupY() { return pickupShelfY; }
    public int getPickupSlot() { return pickupSlot; }

    public int getDropX() { return dropShelfX; }
    public int getDropY() { return dropShelfY; }
    public int getDropSlot() { return dropSlot; }

}
