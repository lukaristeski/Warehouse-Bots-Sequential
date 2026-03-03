package Parallel;

import java.util.ArrayDeque;
import java.util.Deque;

public class Bot {

    public enum Phase { PICKUP, DROPOFF, DONE}
    private enum State { GO_PICKUP, WAIT_PICKUP, GO_DROPOFF, WAIT_DROPOFF, IDLE, DONE }

    private final int id;
    private Location location;

    private final Deque<Task> tasks = new ArrayDeque<>();

    private Parallel.Bot.State state = Parallel.Bot.State.IDLE;
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
        if (state == Parallel.Bot.State.IDLE) {
            state = Parallel.Bot.State.GO_PICKUP;
        }
    }

    public Parallel.Bot.Phase getPhase() {
        if (state == Parallel.Bot.State.DONE) {
            return Parallel.Bot.Phase.DONE;
        }
        if (state == Parallel.Bot.State.GO_DROPOFF || state == Parallel.Bot.State.WAIT_DROPOFF) {
            return Parallel.Bot.Phase.DROPOFF;
        }
        return Parallel.Bot.Phase.PICKUP;
    }

    public static class Intent {
        public final Location nextPos;
        public final boolean act;
        public Intent(Location nextPos, boolean act) {
            this.nextPos = nextPos;
            this.act = act;
        }
    }

    public Parallel.Bot.Intent decideIntent() {
        shouldActThisTick = false;

        if (state == Parallel.Bot.State.DONE) {
            return new Parallel.Bot.Intent(location, false);
        }

        Task t = currentTask();
        if (t == null) {
            state = Parallel.Bot.State.IDLE;
            return new Parallel.Bot.Intent(location, false);
        }

        if (state == Parallel.Bot.State.WAIT_PICKUP || state == Parallel.Bot.State.WAIT_DROPOFF) {
            shouldActThisTick = true;
            return new Parallel.Bot.Intent(location, true);
        }

        int sx, sy;
        if (state == Parallel.Bot.State.GO_PICKUP) {
            sx = t.getPickupX(); sy = t.getPickupY();
        } else {
            sx = t.getDropX(); sy = t.getDropY();
        }

        Location goal = Parallel.GameState.bestInteractionTile(sx, sy, location);
        if (goal == null) return new Parallel.Bot.Intent(location, false);

        if (goal.equals(location)) {
            state = (state == Parallel.Bot.State.GO_PICKUP) ? Parallel.Bot.State.WAIT_PICKUP : Parallel.Bot.State.WAIT_DROPOFF;
            shouldActThisTick = false;
            return new Parallel.Bot.Intent(location, false);
        }

        Location step = Parallel.GameState.bfsNextStep(id, location, goal);
        if (step == null) return new Parallel.Bot.Intent(location, false);
        return new Parallel.Bot.Intent(step, false);
    }

    public void performActionIfPossible() {
        if (!shouldActThisTick) return;

        Task t = currentTask();
        if (t == null) return;

        if (state == Parallel.Bot.State.WAIT_PICKUP) {
            Parallel.Shelf s = Parallel.GameState.getShelf(t.getPickupX(), t.getPickupY());
            if (!carrying && s.pick(t.getPickupSlot())) {
                carrying = true;
                state = Parallel.Bot.State.GO_DROPOFF;
            }
        } else if (state == Parallel.Bot.State.WAIT_DROPOFF) {
            Shelf s = GameState.getShelf(t.getDropX(), t.getDropY());
            if (carrying && s.drop(t.getDropSlot())) {
                carrying = false;
                tasks.pollFirst();
                state = hasTasks() ? Parallel.Bot.State.GO_PICKUP : Parallel.Bot.State.DONE;
            }
        }
    }

    public boolean isDone(){
        return state == Parallel.Bot.State.DONE;
    }
}
