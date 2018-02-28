package de.hhu.bsinfo.dxraft.timer;

import de.hhu.bsinfo.dxraft.context.RaftContext;

import java.util.concurrent.*;

public class RaftTimer {

    private RaftContext context;
    private TimeoutHandler timeoutHandler;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture timeoutFuture;

    public RaftTimer(RaftContext context, TimeoutHandler timeoutHandler) {
        this.context = context;
        this.timeoutHandler = timeoutHandler;
    }

    public void cancel() {
        if (timeoutFuture != null && !timeoutFuture.isDone()) {
            // TODO interrupt?
            timeoutFuture.cancel(false);
        }
    }

    public void schedule(int timeoutInMilliseconds, int randomizationAmountInMilliseconds) {

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
}
