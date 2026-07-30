package Sequential;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;


public class InstructionGenerator {

    static final int NUM_BOTS = 10;
    static final int SHELF_COLS = 5;
    static final int SHELF_ROWS = 5;
    static final int TASKS_PER_BOT = 5;
    static final long RANDOM_SEED = 2026L;
    static final String OUT_FILE =
            System.getProperty("user.dir")
                    + File.separator
                    + "instructions.txt";

    public static void main(String[] args) throws IOException {
        int totalTasks = Math.multiplyExact(NUM_BOTS, TASKS_PER_BOT);
        int slotCapacity = Math.multiplyExact(Math.multiplyExact(SHELF_COLS, SHELF_ROWS), 8);

        if (NUM_BOTS <= 0 || SHELF_COLS <= 0 || SHELF_ROWS <= 0 || TASKS_PER_BOT < 0) {
            throw new IllegalArgumentException("Bots and grid dimensions must be positive; tasks cannot be negative.");
        }
        if (totalTasks > slotCapacity) {
            throw new IllegalArgumentException(
                    "Requested " + totalTasks + " tasks, but a " + SHELF_COLS + " x " + SHELF_ROWS
                            + " warehouse has only " + slotCapacity
                            + " unique pickup slots and " + slotCapacity + " unique drop slots.");
        }

        List<SlotAddress> pickupSlots = new ArrayList<>(slotCapacity);
        List<SlotAddress> dropSlots = new ArrayList<>(slotCapacity);
        for (int sy = 0; sy < SHELF_ROWS; sy++) {
            for (int sx = 0; sx < SHELF_COLS; sx++) {
                for (int slot = 0; slot < 8; slot++) pickupSlots.add(new SlotAddress(sx, sy, slot));
                for (int slot = 8; slot < 16; slot++) dropSlots.add(new SlotAddress(sx, sy, slot));
            }
        }

        Random random = new Random(RANDOM_SEED);
        Collections.shuffle(pickupSlots, random);
        Collections.shuffle(dropSlots, random);

        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(OUT_FILE)))) {
            out.println(NUM_BOTS);
            out.println(SHELF_COLS + " " + SHELF_ROWS);

            int addressIndex = 0;
            for (int bot = 1; bot <= NUM_BOTS; bot++) {
                for (int task = 0; task < TASKS_PER_BOT; task++) {
                    SlotAddress from = pickupSlots.get(addressIndex);
                    SlotAddress to = dropSlots.get(addressIndex);
                    addressIndex++;

                    out.printf("B%d|(from)%d-%d-%x|(to)%d-%d-%x%n",
                            bot, from.sx, from.sy, from.slot, to.sx, to.sy, to.slot);
                }
            }
        }

        System.out.println("Generated " + new File(OUT_FILE).getAbsolutePath());
        System.out.println("Bots = " + NUM_BOTS);
        System.out.println("Warehouse grid = " + SHELF_COLS + " x " + SHELF_ROWS);
        System.out.println("Tasks = " + totalTasks);
    }

    private record SlotAddress(int sx, int sy, int slot) {}
}
