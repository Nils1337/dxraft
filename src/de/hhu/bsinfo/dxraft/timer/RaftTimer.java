package de.hhu.bsinfo.dxraft.timer;

import de.hhu.bsinfo.dxraft.context.RaftContext;
import de.hhu.bsinfo.dxraft.server.RaftServerContext;
import de.hhu.bsinfo.dxraft.server.ServerState;

import java.util.concurrent.*;

import static de.hhu.bsinfo.dxraft.server.ServerState.State.FOLLOWER;

public class RaftTimer {

    private RaftServerContext context;
    private TimeoutHandler timeoutHandler;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture timeoutFuture;

    public RaftTimer(RaftServerContext context) {
        this.context = context;
    }

    public void cancel() {
        if (timeoutFuture != null && !timeoutFuture.isDone()) {
            // Timer thread might already be running.
            // It can be interrupted if it's waiting for lock on the RaftServer instance
            timeoutFuture.cancel(true);
        }
    }

    public void reset(ServerState.State newState) {
        switch (newState) {
            case FOLLOWER:
                schedule(context.getFollowerTimeoutDuration(), context.getFollowerRandomizationAmount());
                break;
            case CANDIDATE:
                schedule(context.getElectionTimeoutDuration(), context.getElectionRandomizationAmount());
                break;
            case LEADER:
                schedule(context.getHeartbeatTimeoutDuration(), context.getHeartbeatRandomizationAmount());
        }
    }

    public void schedule(int timeoutInMilliseconds, int randomizationAmountInMilliseconds) {
        if (timeoutHandler == null) {
            throw new RuntimeException("Timeout handler was not set!");
        }

        int randomizedTimeout = timeoutInMilliseconds;

        // randomize timeout
        if (randomizationAmountInMilliseconds > 0) {
            randomizedTimeout += ThreadLocalRandom.current().nextInt(0, randomizationAmountInMilliseconds);
        }

        this.timeoutFuture = this.scheduler.schedule(() -> {
            if (timeoutHandler != null) {
                timeoutHandler.processTimeout();
            }
        }, randomizedTimeout, TimeUnit.MILLISECONDS);
    }

    public void setTimeoutHandler(TimeoutHandler timeoutHandler) {
        this.timeoutHandler = timeoutHandler;
    }
}
