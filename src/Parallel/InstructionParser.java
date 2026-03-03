package Parallel;

import java.io.*;

public class InstructionParser {

    public static void parse(String filename) throws IOException {
        System.out.println("Reading instructions from: " + new File(filename).getAbsolutePath());

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line = nextNonEmpty(reader);
            if (line == null) throw new IllegalArgumentException("Missing num_of_bots");
            int numBots = Integer.parseInt(line.trim());

            GameState.initFixedWarehouse(numBots);

            int loaded = 0;
            while ((line = reader.readLine()) != null) {
                String raw = line;
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] tp = line.split("\\|");
                if (tp.length != 3) {
                    throw new IllegalArgumentException("Invalid task line: " + raw);
                }

                int botIndex = parseBotIndex(tp[0].trim(), numBots);

                FromTo from = parseFromTo(tp[1].trim(), true, raw);
                FromTo to   = parseFromTo(tp[2].trim(), false, raw);

                Task task = new Task(from.x, from.y, from.slot, to.x, to.y, to.slot);
                GameState.bots[botIndex].addTask(task);
                loaded++;
            }

            System.out.println("Loaded tasks");
            for (Bot b : GameState.bots) {
                System.out.println("B" + (b.getId() + 1) + " tasks = " + b.taskCount());
            }
            System.out.println("Total loaded tasks = " + loaded);
        }
    }

    private static String nextNonEmpty(BufferedReader r) throws IOException {
        String s;
        while ((s = r.readLine()) != null) {
            s = s.trim();
            if (!s.isEmpty()) return s;
        }
        return null;
    }

    private static int parseBotIndex(String botToken, int numBots) {
        if (!botToken.matches("B\\d+")) throw new IllegalArgumentException("Invalid bot token: " + botToken);
        int idx = Integer.parseInt(botToken.substring(1)) - 1;
        if (idx < 0 || idx >= numBots) throw new IllegalArgumentException("Bot out of range: " + botToken);
        return idx;
    }

    private static class FromTo {
        int x, y, slot;
        FromTo(int x, int y, int slot) { this.x = x; this.y = y; this.slot = slot; }
    }

    private static FromTo parseFromTo(String raw, boolean isFrom, String wholeLine) {
        String tag = isFrom ? "(from)" : "(to)";
        raw = raw.replace(" ", "");
        if (!raw.startsWith(tag)) throw new IllegalArgumentException("Missing " + tag + " in: " + wholeLine);

        String payload = raw.substring(tag.length());
        String[] p = payload.split("-");
        if (p.length != 3) throw new IllegalArgumentException("Bad " + tag + " payload: " + wholeLine);

        int sx = Integer.parseInt(p[0]);
        int sy = Integer.parseInt(p[1]);
        if (sx < 0 || sx >= GameState.SHELF_COLS || sy < 0 || sy >= GameState.SHELF_ROWS) {
            throw new IllegalArgumentException("Shelf coords out of bounds in: " + wholeLine);
        }

        int slot = Integer.parseInt(p[2], 16);
        if (slot < 0 || slot > 15) throw new IllegalArgumentException("Slot must be 0..f in: " + wholeLine);

        return new FromTo(sx, sy, slot);
    }
}
