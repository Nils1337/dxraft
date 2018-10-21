package de.hhu.bsinfo.dxraft.timer;

import de.hhu.bsinfo.dxraft.server.ServerConfig;
import de.hhu.bsinfo.dxraft.state.ServerState;

import java.util.concurrent.*;

public class RaftTimer {

    private ServerConfig m_context;
    private TimeoutHandler m_timeoutHandler;
    private final ScheduledExecutorService m_scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture m_timeoutFuture;

    public RaftTimer(ServerConfig p_context) {
        m_context = p_context;
    }

    public void cancel() {
        if (m_timeoutFuture != null && !m_timeoutFuture.isDone()) {
            // Timer thread might already be running.
            // It can be interrupted if it's waiting for lock on the RaftServer instance
            m_timeoutFuture.cancel(false);
        }
    }

    public void reset(ServerState.State p_newState) {
        cancel();

        switch (p_newState) {
            case FOLLOWER:
                schedule(m_context.getFollowerTimeoutDuration(), m_context.getFollowerRandomizationAmount());
                break;
            case CANDIDATE:
                schedule(m_context.getElectionTimeoutDuration(), m_context.getElectionRandomizationAmount());
                break;
            case LEADER:
                schedule(m_context.getHeartbeatTimeoutDuration(), m_context.getHeartbeatRandomizationAmount());
        }
    }

    public void schedule(int p_timeoutInMilliseconds, int p_randomizationAmountInMilliseconds) {
        if (m_timeoutHandler == null) {
            throw new RuntimeException("Timeout handler was not set!");
        }

        int randomizedTimeout = p_timeoutInMilliseconds;

        // randomize timeout
        if (p_randomizationAmountInMilliseconds > 0) {
            randomizedTimeout += ThreadLocalRandom.current().nextInt(0, p_randomizationAmountInMilliseconds);
        }

        m_timeoutFuture = m_scheduler.schedule(() -> {
            if (m_timeoutHandler != null) {
                m_timeoutHandler.processTimeout();
            }
        }, randomizedTimeout, TimeUnit.MILLISECONDS);
    }

    public void setTimeoutHandler(TimeoutHandler p_timeoutHandler) {
        m_timeoutHandler = p_timeoutHandler;
    }
}
