package Parallel;

import java.util.*;
import java.util.concurrent.*;

public class GameState {

    public static final int SHELF_COLS = 4;
    public static final int SHELF_ROWS = 4;

    public static final int SHELF_TILE_W = 5;
    public static final int SHELF_TILE_H = 2;
    public static final int AISLE = 1;

    public static final int WIDTH =
            SHELF_COLS * SHELF_TILE_W + (SHELF_COLS + 1) * AISLE;
    public static final int HEIGHT =
            SHELF_ROWS * SHELF_TILE_H + (SHELF_ROWS + 1) * AISLE;

    public static final boolean[][] shelfMask = new boolean[WIDTH][HEIGHT];
    public static final Shelf[][] shelves = new Shelf[SHELF_COLS][SHELF_ROWS];

    public static Bot[] bots;


    private static final ExecutorService POOL =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public static long tick = 0;

    public static void initFixedWarehouse(int numBots) {
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

        bots = new Bot[numBots];

        int spawned = 0;
        for (int x = 0; x < WIDTH && spawned < numBots; x++) {
            if (isAisle(x, 0)) {
                bots[spawned] = new Bot(spawned, new Location(x, 0));
                spawned++;
            }
        }
        for (int y = 0; y < HEIGHT && spawned < numBots; y++) {
            if (isAisle(0, y)) {
                bots[spawned] = new Bot(spawned, new Location(0, y));
                spawned++;
            }
        }
        if (spawned < numBots) {
            throw new IllegalStateException("Too many bots for spawn area.");
        }
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
    public static boolean occupiedByOtherBot(int x, int y, int botId) {
        for (Bot b : bots) {
            if (b.getId() == botId) continue;
            Location p = b.getLocation();
            if (p.getX() == x && p.getY() == y) return true;
        }
        return false;
    }
    public static Shelf getShelf(int shelfX, int shelfY) {
        if (shelfX < 0 || shelfX >= SHELF_COLS ||
                shelfY < 0 || shelfY >= SHELF_ROWS)
            return null;
        return shelves[shelfX][shelfY];
    }

    public static List<Location> interactionTiles(int shelfX, int shelfY) {
        Shelf s = getShelf(shelfX, shelfY);
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

    public static Location bestInteractionTile(int shelfX, int shelfY, Location botPos) {
        List<Location> adj = interactionTiles(shelfX, shelfY);
        if (adj.isEmpty()) return null;

        Location best = null;
        int bestDist = Integer.MAX_VALUE;
        for (Location l : adj) {
            int d = Math.abs(l.getX() - botPos.getX()) +
                    Math.abs(l.getY() - botPos.getY());
            if (d < bestDist) {
                bestDist = d;
                best = l;
            }
        }
        return best;
    }

    public static Location bfsNextStep(int botId, Location start, Location goal) {
        if (start.equals(goal)) return start;

        ArrayDeque<Location> q = new ArrayDeque<>();
        boolean[][] vis = new boolean[WIDTH][HEIGHT];
        Location[][] parent = new Location[WIDTH][HEIGHT];

        q.add(start);
        vis[start.getX()][start.getY()] = true;
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};

        while (!q.isEmpty()) {
            Location cur = q.poll();
            if (cur.equals(goal)) break;

            for (int[] d : dirs) {
                int nx = cur.getX() + d[0];
                int ny = cur.getY() + d[1];

                if (!isAisle(nx, ny)) continue;
                if (occupiedByOtherBot(nx, ny, botId) &&
                        !(nx == goal.getX() && ny == goal.getY())) continue;
                if (vis[nx][ny]) continue;

                vis[nx][ny] = true;
                parent[nx][ny] = cur;
                q.add(new Location(nx, ny));
            }
        }

        if (!vis[goal.getX()][goal.getY()]) return null;

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

        Bot.Intent[] intents = new Bot.Intent[n];
        boolean[] actFlags = new boolean[n];

        CountDownLatch decideLatch = new CountDownLatch(n);
        for (int i = 0; i < n; i++) {
            final int idx = i;
            POOL.submit(() -> {
                Bot.Intent it = bots[idx].decideIntent();
                intents[idx] = it;
                actFlags[idx] = it.act;
                decideLatch.countDown();
            });
        }
        try { decideLatch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        Location[] desired = new Location[n];
        for (int i = 0; i < n; i++) {
            Bot.Intent it = intents[i];
            desired[i] = (it != null && it.nextPos != null) ? it.nextPos : bots[i].getLocation();
        }

        Map<Location, List<Integer>> who = new HashMap<>();
        for (int i = 0; i < n; i++) {
            who.computeIfAbsent(desired[i], k -> new ArrayList<>()).add(i);
        }
        for (var e : who.entrySet()) {
            if (e.getValue().size() > 1) {          // more than one bot wants the same tile
                for (int idx : e.getValue()) {
                    desired[idx] = bots[idx].getLocation(); // stay where you are
                }
            }
        }

        CountDownLatch moveLatch = new CountDownLatch(n);
        for (int i = 0; i < n; i++) {
            final int idx = i;
            POOL.submit(() -> {
                bots[idx].setLocation(desired[idx]);
                moveLatch.countDown();
            });
        }
        try { moveLatch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        CountDownLatch actLatch = new CountDownLatch(n);
        for (int i = 0; i < n; i++) {
            final int idx = i;
            POOL.submit(() -> {
                if (actFlags[idx]) {
                    bots[idx].performActionIfPossible();
                }
                actLatch.countDown();
            });
        }
        try { actLatch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        tick++;
    }

    public static boolean allBotsDone() {
        if (bots == null) return true;
        for (Bot b : bots)
            if (b != null && !b.isDone()) return false;
        return true;
    }

    public static void shutdown() {
        POOL.shutdownNow();
    }
}
