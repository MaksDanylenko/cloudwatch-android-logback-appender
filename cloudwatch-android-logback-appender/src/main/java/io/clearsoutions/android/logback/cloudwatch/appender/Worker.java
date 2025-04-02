package io.clearsoutions.android.logback.cloudwatch.appender;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import ch.qos.logback.classic.spi.ILoggingEvent;
import io.clearsoutions.android.logback.cloudwatch.appender.configuration.LogbackConfiguration;

public class Worker implements Runnable {

    private final BlockingQueue<ILoggingEvent> logs;
    private final CloudWatchLogWriter cloudWatchLogWriter;
    private final LogbackConfiguration configuration;

    public Worker(BlockingQueue<ILoggingEvent> logs, CloudWatchLogWriter cloudWatchLogWriter,
                  LogbackConfiguration configuration) {
        this.logs = logs;
        this.cloudWatchLogWriter = cloudWatchLogWriter;
        this.configuration = configuration;
    }

    @Override
    public void run() {
        try {
            long lastDrainTime = System.currentTimeMillis();
            while (!Thread.currentThread().isInterrupted()) {
                // 1 seconds in milliseconds
                long thresholdTime = 1000;
                // Minimum number of log events to process at once
                int maxSize = 100;
                if ((System.currentTimeMillis() - lastDrainTime >= thresholdTime
                    || logs.size() >= maxSize)
                    && ! logs.isEmpty()) {

                    List<ILoggingEvent> buffer = new ArrayList<>();
                    logs.drainTo(buffer);
                    processBuffer(buffer);
                    lastDrainTime = System.currentTimeMillis();
                } else {
                    // Sleep for a short duration to avoid tight looping, adjust as necessary
                    Thread.sleep(100);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Queue processing thread was interrupted.");
        }
    }

    public void processBuffer(List<ILoggingEvent> buffer) {
        List<LogEventDTO> collect = new ArrayList<>();
        for (ILoggingEvent event : buffer) {
            collect.add(createLogEventDTO(event));
        }

        Collections.sort(collect, new Comparator<LogEventDTO>() {
            @Override
            public int compare(LogEventDTO o1, LogEventDTO o2) {
                return Long.compare(o1.getTimestamp(), o2.getTimestamp());
            }
        });
        cloudWatchLogWriter.write(collect);
    }

    private LogEventDTO createLogEventDTO(ILoggingEvent log) {
        String message;
        if (configuration.getEncoder() != null) {
            message = new String(configuration.getEncoder().encode(log), StandardCharsets.UTF_8);
        } else {
            message = configuration.getLayout().doLayout(log);
        }
        return new LogEventDTO(message, log.getTimeStamp());
    }
}
