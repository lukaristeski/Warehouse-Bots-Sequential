package Sequential;

import java.util.ArrayDeque;
import java.util.Deque;

public class Bot {

    public enum Phase {PICKUP, DROPOFF, DONE}

    private enum State {GO_PICKUP, WAIT_PICKUP, GO_DROPOFF, WAIT_DROPOFF, IDLE, DONE}

    private final int id;
    private final long spawnTick;
    private Location location;

    private final Deque<Task> tasks = new ArrayDeque<>();

    private State state = State.IDLE;
    private boolean carrying = false;
    private boolean active = false;

    private long doneTick = -1;


    public Bot(int id, Location start, long spawnTick) {
        this.id = id;
        this.location = start;
        this.spawnTick = spawnTick;
    }

    public int getId() {
        return id;
    }

    public long getSpawnTick() {
        return spawnTick;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location l) {
        this.location = l;
    }

    public boolean isActive() {
        return active;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
        this.tasks.clear();
    }

    public boolean hasTasks() {
        return !tasks.isEmpty();
    }

    public int taskCount() {
        return tasks.size();
    }

    public Task currentTask() {
        return tasks.peekFirst();
    }

    public void addTask(Task t) {
        tasks.addLast(t);
        if (state == State.IDLE) state = State.GO_PICKUP;
    }

    public Phase getPhase() {
        if (state == State.DONE) return Phase.DONE;
        if (state == State.GO_DROPOFF ||
                state == State.WAIT_DROPOFF) return Phase.DROPOFF;
        return Phase.PICKUP;
    }

    public boolean isDone() {
        return state == State.DONE;
    }

    private void markDoneTick() {
        this.doneTick = GameState.tick;
    }

    public boolean readyToDisappear() {
        return active && state == State.DONE &&
                (GameState.tick - doneTick) >= GameState.DISAPPEAR_TICKS;
    }

    public Intent decideIntent() {

        if (!active)
            return new Intent(location, false);

        if (state == State.DONE)
            return new Intent(location, false);

        Task t = currentTask();
        if (t == null) {
            state = State.IDLE;
            return new Intent(location, false);
        }

        if (state == State.WAIT_PICKUP || state == State.WAIT_DROPOFF)
            return new Intent(location, true);

        int sx, sy;
        if (state == State.GO_PICKUP) {
            sx = t.getPickupX();
            sy = t.getPickupY();
        } else {
            sx = t.getDropX();
            sy = t.getDropY();
        }


        Location goal = GameState.bestInteractionTile(sx, sy, location);
        if (goal == null) {
            return new Intent(location, false);
        }

        if (goal.equals(location)) {
            state = (state == State.GO_PICKUP) ? State.WAIT_PICKUP : State.WAIT_DROPOFF;
            return new Intent(location, false);
        }

        Location step = GameState.bfsNextStep(location, goal);
        if (step == null)
            return new Intent(location, false);
        return new Intent(step, false);
    }

    public void performActionIfPossible() {
        Task t = currentTask();
        if (t == null) return;

        if (state == State.WAIT_PICKUP) {
            Shelf s = GameState.getShelf(t.getPickupX(), t.getPickupY());
            if (!carrying && s.pick(t.getPickupSlot())) {
                carrying = true;
                state = State.GO_DROPOFF;
            }
        } else if (state == State.WAIT_DROPOFF) {
            Shelf s = GameState.getShelf(t.getDropX(), t.getDropY());
            if (carrying && s.drop(t.getDropSlot())) {
                carrying = false;
                tasks.pollFirst();
                if (hasTasks())
                    state = State.GO_PICKUP;
                else {
                    state = State.DONE;
                    markDoneTick();
                }
            }
        }
    }

    public static class Intent {
        public final Location nextPos;
        public final boolean act;
        public Intent(Location nextPos, boolean act) {
            this.nextPos = nextPos;
            this.act = act;
        }
    }
}
