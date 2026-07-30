package Sequential;

import java.util.*;

public class GameState {

    public static int SHELF_COLS;
    public static int SHELF_ROWS;

    public static final int SHELF_TILE_W = 5;
    public static final int SHELF_TILE_H = 2;
    public static final int AISLE = 1;

    public static int WIDTH;
    public static int HEIGHT;

    public static boolean[][] shelfMask;
    public static Shelf[][] shelves;

    public static Bot[] bots;
    public static boolean[][] occupied;


    public static long tick = 0;

    static final int DISAPPEAR_TICKS = 8;
    static final int SPAWN_DELAY = 5;

    private static int SPAWN_X = -1;
    private static int SPAWN_Y = -1;

    private static final Deque<Bot> pendingSpawns = new ArrayDeque<>();


    public static void initWarehouse(int numBots, int shelfCols, int shelfRows) {
        if (numBots <= 0) {
            throw new IllegalArgumentException("Number of bots must be greater than zero.");
        }
        if (shelfCols <= 0 || shelfRows <= 0) {
            throw new IllegalArgumentException("Warehouse grid dimensions must be greater than zero.");
        }

        SHELF_COLS = shelfCols;
        SHELF_ROWS = shelfRows;
        WIDTH = SHELF_COLS * SHELF_TILE_W + (SHELF_COLS + 1) * AISLE;
        HEIGHT = SHELF_ROWS * SHELF_TILE_H + (SHELF_ROWS + 1) * AISLE;

        shelfMask = new boolean[WIDTH][HEIGHT];
        shelves = new Shelf[SHELF_COLS][SHELF_ROWS];
        occupied = new boolean[WIDTH][HEIGHT];

        for (int sy = 0; sy < SHELF_ROWS; sy++) {
            for (int sx = 0; sx < SHELF_COLS; sx++) {
                int x0 = AISLE + sx * (SHELF_TILE_W + AISLE);
                int y0 = AISLE + sy * (SHELF_TILE_H + AISLE);

                for (int dy = 0; dy < SHELF_TILE_H; dy++) {
                    for (int dx = 0; dx < SHELF_TILE_W; dx++) {
                        shelfMask[x0 + dx][y0 + dy] = true;
                    }
                }
                shelves[sx][sy] = new Shelf(new Location(x0, y0));
            }
        }

        SPAWN_X = -1;
        SPAWN_Y = -1;
        outer:
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                if (isAisle(x, y)) {
                    SPAWN_X = x;
                    SPAWN_Y = y;
                    break outer;
                }
            }
        }
        if (SPAWN_X == -1) {
            throw new IllegalStateException("No aisle tile found for spawning.");
        }

        bots = new Bot[numBots];
        for (int i = 0; i < numBots; i++) {
            long spawnTick = i * (long) SPAWN_DELAY;
            bots[i] = new Bot(i, new Location(SPAWN_X, SPAWN_Y), spawnTick);
        }

        pendingSpawns.clear();
        tick = 0;
    }

    public static boolean inBounds(int x, int y) {
        return x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT;
    }
    public static boolean isShelfTile(int x, int y) {
        return inBounds(x, y) && shelfMask[x][y];
    }
    public static boolean isAisle(int x, int y) {
        return inBounds(x, y) && !shelfMask[x][y];
    }
    public static Shelf getShelf(int sx, int sy) {
        if (sx < 0 || sx >= SHELF_COLS || sy < 0 || sy >= SHELF_ROWS) return null;
        return shelves[sx][sy];
    }

    public static List<Location> interactionTiles(int sx, int sy) {
        Shelf s = getShelf(sx, sy);
        if (s == null) return List.of();

        int x0 = s.getLocation().getX();
        int y0 = s.getLocation().getY();

        ArrayList<Location> out = new ArrayList<>();

        for (int dx = 0; dx < SHELF_TILE_W; dx++) {
            int ax = x0 + dx;
            if (isAisle(ax, y0 - 1)) out.add(new Location(ax, y0 - 1));
            if (isAisle(ax, y0 + SHELF_TILE_H)) out.add(new Location(ax, y0 + SHELF_TILE_H));
        }
        for (int dy = 0; dy < SHELF_TILE_H; dy++) {
            int ay = y0 + dy;
            if (isAisle(x0 - 1, ay)) out.add(new Location(x0 - 1, ay));
            if (isAisle(x0 + SHELF_TILE_W, ay)) out.add(new Location(x0 + SHELF_TILE_W, ay));
        }
        return out;
    }

    public static Location bestInteractionTile(int sx, int sy, Location botPos) {
        List<Location> adj = interactionTiles(sx, sy);
        if (adj.isEmpty()) return null;

        Location bestTile = null;
        int bestDist = Integer.MAX_VALUE;

        for (Location tile : adj) {

            Location step = bfsNextStep(botPos, tile);
            if (step == null) continue;

            int d = Math.abs(tile.getX() - botPos.getX()) +
                    Math.abs(tile.getY() - botPos.getY());

            if (d < bestDist) {
                bestDist = d;
                bestTile = tile;
            }
        }

        return bestTile;
    }

    public static Location bfsNextStep( Location start, Location goal) {
        if (start.equals(goal)) return start;

        ArrayDeque<Location> q = new ArrayDeque<>();
        boolean[][] visited = new boolean[WIDTH][HEIGHT];
        Location[][] parent = new Location[WIDTH][HEIGHT];

        q.add(start);
        visited[start.getX()][start.getY()] = true;

        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};

        while (!q.isEmpty()) {
            Location cur = q.poll();

            if (cur.equals(goal)) break;

            for (int[] d : dirs) {
                int nx = cur.getX() + d[0];
                int ny = cur.getY() + d[1];

                if (!inBounds(nx, ny)) continue;
                if (!isAisle(nx, ny)) continue;
                if (occupied[nx][ny]) continue;

                if (visited[nx][ny]) continue;
                visited[nx][ny] = true;
                parent[nx][ny] = cur;
                q.add(new Location(nx, ny));
            }
        }

        if (!visited[goal.getX()][goal.getY()]) return null;

        Location step = goal;
        Location prev = parent[step.getX()][step.getY()];
        while (prev != null && !prev.equals(start)) {
            step = prev;
            prev = parent[step.getX()][step.getY()];
        }
        return step;
    }

    public static void step() {

        int n = bots.length;
        while (!pendingSpawns.isEmpty() && !occupied[SPAWN_X][SPAWN_Y]) {
            Bot b = pendingSpawns.pollFirst();
            b.activate();
            b.setLocation(new Location(SPAWN_X, SPAWN_Y));
            occupied[SPAWN_X][SPAWN_Y] = true;
        }


        for (int i = 0; i < n; i++) {
            Bot b = bots[i];
            if (b != null && !b.isActive() && tick >= b.getSpawnTick()) {
                if (!occupied[SPAWN_X][SPAWN_Y]) {
                    b.activate();
                    b.setLocation(new Location(SPAWN_X, SPAWN_Y));
                    occupied[SPAWN_X][SPAWN_Y] = true;
                } else {
                    if (!pendingSpawns.contains(b)) {
                        pendingSpawns.addLast(b);
                    }
                }
            }
        }

        Bot.Intent[] intents = new Bot.Intent[n];
        boolean[] actFlags = new boolean[n];

        for (int i = 0; i < n; i++) {
            Bot b = bots[i];
            if (b != null && b.isActive()) {
                Bot.Intent intent = b.decideIntent();
                intents[i] = intent;
                actFlags[i] = intent.act;
            } else {
                intents[i] = new Bot.Intent(
                        b == null ? new Location(0, 0) : b.getLocation(),
                        false
                );
                actFlags[i] = false;
            }
        }

        Location[] desired = new Location[n];
        for (int i = 0; i < n; i++) {
            Bot b = bots[i];
            if (b != null && b.isActive()) {
                Bot.Intent it = intents[i];
                desired[i] = it.nextPos;
            } else {
                desired[i] = (b == null) ? new Location(0, 0) : b.getLocation();
            }
        }

        List<Integer> activeIdx = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Bot b = bots[i];
            if (b != null && b.isActive()) activeIdx.add(i);
        }

        activeIdx.sort(Comparator.comparingLong(i -> bots[i].getSpawnTick()));


        Location[] finalPos = new Location[n];
        for (int i = 0; i < n; i++) {
            Bot b = bots[i];
            finalPos[i] = (b == null) ? new Location(0, 0) : b.getLocation();
        }

        for (int idx : activeIdx) {
            Bot b = bots[idx];
            Location cur = b.getLocation();
            Location tgt = desired[idx];

            if (tgt.equals(cur)) {
                finalPos[idx] = cur;
                continue;
            }

            if (!occupied[tgt.getX()][tgt.getY()]) {
                finalPos[idx] = tgt;

                occupied[cur.getX()][cur.getY()] = false;
                occupied[tgt.getX()][tgt.getY()] = true;
            } else {
                finalPos[idx] = cur;
                actFlags[idx] = false;
            }
        }

        for (int x = 0; x < WIDTH; x++) {
            Arrays.fill(occupied[x], false);
        }

        for (int i = 0; i < n; i++) {
            Bot b = bots[i];
            if (b != null && b.isActive()) {
                b.setLocation(finalPos[i]);
                Location position = finalPos[i];
                occupied[position.getX()][position.getY()] = true;
            }
        }

        for (int i = 0; i < n; i++) {
            Bot b = bots[i];
            if (b != null && b.isActive() && actFlags[i]) {
                b.performActionIfPossible();
            }
        }

        for (Bot b : bots) {
            if (b != null && b.isActive() && b.readyToDisappear()) {
                b.deactivate();
            }
        }
        tick++;
    }
    public static boolean allBotsDone() {
        if (bots == null) return true;

        for (Bot b : bots) {
            if (b == null) continue;
            if (b.isActive() && !b.isDone()) return false;
            if (!b.isActive() && b.taskCount() > 0) return false;
        }
        return true;
    }
}
