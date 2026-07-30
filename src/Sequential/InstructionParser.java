package Sequential;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class InstructionParser {

    public static void parse(String filename) throws IOException {
        System.out.println("Reading instructions from: " + new File(filename).getAbsolutePath());

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String botsLine = nextNonEmpty(reader);
            if (botsLine == null) throw new IllegalArgumentException("Missing num_of_bots");
            int numBots = parsePositiveInt(botsLine, "num_of_bots");

            String gridLine = nextNonEmpty(reader);
            if (gridLine == null) throw new IllegalArgumentException("Missing warehouse grid size A B");
            String[] grid = gridLine.trim().split("\\s+");
            if (grid.length != 2) {
                throw new IllegalArgumentException("Warehouse size must contain exactly two integers: A B");
            }
            int shelfCols = parsePositiveInt(grid[0], "warehouse width A");
            int shelfRows = parsePositiveInt(grid[1], "warehouse height B");

            GameState.initWarehouse(numBots, shelfCols, shelfRows);

            int loaded = 0;
            int lineNumber = 2;
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String raw = line;
                line = line.trim();
                if (line.isEmpty()) continue;

                try {
                    String[] parts = line.split("\\|", -1);
                    if (parts.length != 3) {
                        throw new IllegalArgumentException("Expected bot|from|to");
                    }

                    int botIndex = parseBotIndex(parts[0].trim(), numBots);
                    FromTo from = parseEndpoint(parts[1].trim(), true, shelfCols, shelfRows);
                    FromTo to = parseEndpoint(parts[2].trim(), false, shelfCols, shelfRows);

                    GameState.bots[botIndex].addTask(
                            new Task(from.x, from.y, from.slot, to.x, to.y, to.slot));
                    loaded++;
                } catch (RuntimeException e) {
                    throw new IllegalArgumentException(
                            "Invalid instruction at line " + lineNumber + ": " + raw + " (" + e.getMessage() + ")", e);
                }
            }

            System.out.println("Warehouse grid = " + shelfCols + " x " + shelfRows);
            System.out.println("Loaded tasks = " + loaded);
            for (Bot bot : GameState.bots) {
                System.out.println("B" + (bot.getId() + 1) + " tasks = " + bot.taskCount());
            }
        }
    }

    private static String nextNonEmpty(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (!line.isEmpty()) return line;
        }
        return null;
    }

    private static int parsePositiveInt(String text, String field) {
        try {
            int value = Integer.parseInt(text.trim());
            if (value <= 0) throw new IllegalArgumentException(field + " must be greater than zero");
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(field + " must be an integer", e);
        }
    }

    private static int parseBotIndex(String token, int numBots) {
        if (!token.matches("B\\d+")) throw new IllegalArgumentException("Invalid bot token: " + token);
        int index = Integer.parseInt(token.substring(1)) - 1;
        if (index < 0 || index >= numBots) throw new IllegalArgumentException("Bot out of range: " + token);
        return index;
    }

    private static FromTo parseEndpoint(String token, boolean from,
                                        int shelfCols, int shelfRows) {
        String compact = token.replace(" ", "");
        String tag = from ? "(from)" : "(to)";

        if (compact.startsWith(tag)) compact = compact.substring(tag.length());
        else if (compact.startsWith("(")) throw new IllegalArgumentException("Expected " + tag);

        String[] values = compact.split("-", -1);
        if (values.length != 3) throw new IllegalArgumentException("Bad " + tag + " payload");

        int sx;
        int sy;
        try {
            sx = Integer.parseInt(values[0]);
            sy = Integer.parseInt(values[1]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Shelf coordinates must be decimal integers", e);
        }

        if (sx < 0 || sx >= shelfCols || sy < 0 || sy >= shelfRows) {
            throw new IllegalArgumentException(
                    "Shelf coordinates out of bounds: " + sx + "-" + sy);
        }

        if (!values[2].matches("[0-9a-fA-F]")) {
            throw new IllegalArgumentException("Shelf slot must be one hexadecimal digit 0-f");
        }
        int slot = Integer.parseInt(values[2], 16);
        return new FromTo(sx, sy, slot);
    }

    private record FromTo(int x, int y, int slot) {}
}
