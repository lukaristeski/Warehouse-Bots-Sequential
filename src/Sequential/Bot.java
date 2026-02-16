package Sequential;

import java.util.ArrayDeque;
import java.util.Deque;

public class Bot {

    public enum Phase { PICKUP, DROPOFF, DONE}
    private enum State { GO_PICKUP, WAIT_PICKUP, GO_DROPOFF, WAIT_DROPOFF, IDLE, DONE }

    private final int id;
    private Location location;

    private final Deque<Task> tasks = new ArrayDeque<>();

    private State state = State.IDLE;
    private boolean carrying = false;

    private boolean shouldActThisTick = false;

    public Bot(int id, Location start) {
        this.id = id;
        this.location = start;
    }

    public int getId() { return id; }
    public Location getLocation() { return location; }
    public void setLocation(Location l) { this.location = l; }

    public boolean hasTasks() { return !tasks.isEmpty(); }
    public int taskCount() { return tasks.size(); }
    public Task currentTask() { return tasks.peekFirst(); }

    public void addTask(Task t) {
        tasks.addLast(t);
        if (state == State.IDLE) {
            state = State.GO_PICKUP;
        }
    }

    public Phase getPhase() {
        if (state == State.DONE) {
            return Phase.DONE;
        }
        if (state == State.GO_DROPOFF || state == State.WAIT_DROPOFF) {
            return Phase.DROPOFF;
        }
        return Phase.PICKUP;
    }

    public static class Intent {
        public final Location nextPos;
        public final boolean act;
        public Intent(Location nextPos, boolean act) {
            this.nextPos = nextPos;
            this.act = act;
        }
    }

    public Intent decideIntent() {
        shouldActThisTick = false;

        if (state == State.DONE) {
            return new Intent(location, false);
        }

        Task t = currentTask();
        if (t == null) {
            state = State.IDLE;
            return new Intent(location, false);
        }

        if (state == State.WAIT_PICKUP || state == State.WAIT_DROPOFF) {
            shouldActThisTick = true;
            return new Intent(location, true);
        }

        int sx, sy;
        if (state == State.GO_PICKUP) {
            sx = t.getPickupX(); sy = t.getPickupY();
        } else {
            sx = t.getDropX(); sy = t.getDropY();
        }

        Location goal = GameState.bestInteractionTile(sx, sy, location);
        if (goal == null) return new Intent(location, false);

        if (goal.equals(location)) {
            state = (state == State.GO_PICKUP) ? State.WAIT_PICKUP : State.WAIT_DROPOFF;
            shouldActThisTick = false;
            return new Intent(location, false);
        }

        Location step = GameState.bfsNextStep(id, location, goal);
        if (step == null) return new Intent(location, false);
        return new Intent(step, false);
    }

    public void performActionIfPossible() {
        if (!shouldActThisTick) return;

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
                state = hasTasks() ? State.GO_PICKUP : State.DONE;
            }
        }
    }

    public boolean isDone(){
        return state == State.DONE;
    }
}
