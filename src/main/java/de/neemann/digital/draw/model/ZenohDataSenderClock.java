package de.neemann.digital.draw.model;

import java.util.ArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.neemann.digital.core.Model;
import de.neemann.digital.core.ModelEvent;
import de.neemann.digital.core.ModelEventType;
import de.neemann.digital.core.ModelStateObserverTyped;
import de.neemann.digital.core.io.zenoh.ZenohDataSender;
import de.neemann.digital.draw.model.RealTimeClock.Runner;

public class ZenohDataSenderClock implements ModelStateObserverTyped {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZenohDataSenderClock.class);

    private final Model model;
    private final ScheduledThreadPoolExecutor executor;
    private final int frequency;
    private Runner runner;

    /**
     * Creates a new clock for sending zenoh updates
     *
     * @param model     the model
     * @param executor  the executor used to schedule the update
     * @param frequency the frequency for which to send data from zenoh
     *                  publishers/components
     */
    public ZenohDataSenderClock(Model model, ScheduledThreadPoolExecutor executor,
            int frequency) {
        this.model = model;
        this.executor = executor;
        int f = frequency;
        if (f < 1)
            f = 1;
        this.frequency = f;
        model.addObserver(this);
    }

    @Override
    public void handleEvent(ModelEvent event) {
        switch (event.getType()) {
            case STARTED:
                int delayMuS = 1000000 / frequency;
                runner = new RealTimeRunner(delayMuS);
                break;
            case CLOSED:
                if (runner != null)
                    runner.stop();
                break;
            default:
                break;
        }
    }

    @Override
    public ModelEventType[] getEvents() {
        return new ModelEventType[] { ModelEventType.STARTED, ModelEventType.CLOSED };
    }

    /**
     * runs with defined rate
     */
    private class RealTimeRunner implements Runner {

        private final ScheduledFuture<?> timer;

        RealTimeRunner(int delay) {
            timer = executor.scheduleAtFixedRate(() -> {

                ArrayList<ZenohDataSender> zenohDataSenders = model.getZenohSenders();

                for (ZenohDataSender sender : zenohDataSenders) {
                    sender.sendData();
                }
            }, delay, delay, TimeUnit.MICROSECONDS);
        }

        @Override
        public void stop() {
            if (timer != null)
                timer.cancel(false);
        }
    }
}
