package io.clearsoutions.android.logback.cloudwatch.appender;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.LayoutBase;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.status.WarnStatus;
import io.clearsoutions.android.logback.cloudwatch.appender.configuration.CloudWatchConfiguration;
import io.clearsoutions.android.logback.cloudwatch.appender.configuration.LogbackConfiguration;

public class CloudWatchAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    private LayoutBase<ILoggingEvent> layout;
    private Encoder<ILoggingEvent> encoder;

    private String logGroupName;
    private String logStreamName;
    private String logRegion;
    private String accessKeyId;
    private String secretAccessKey;

    private final BlockingQueue<ILoggingEvent> logs = new ArrayBlockingQueue<>(10000);
    private Thread worker;

    @Override
    protected void append(ILoggingEvent iLoggingEvent) {
        boolean offer = logs.offer(iLoggingEvent);
        if (! offer) {
            addWarn("Log queue is full, discarding log event: " + iLoggingEvent);
        }
    }

    @Override
    public void start() {
        super.start();
        try {
            int retentionTimeDays = 0;
            CloudWatchConfiguration configuration = new CloudWatchConfiguration(logGroupName,
                                                            logStreamName,
                                                            logRegion,
                                                            accessKeyId,
                                                            secretAccessKey,
                    retentionTimeDays);

            if (configuration.isConfigured()) {
                CloudWatchLogWriter cloudWatchLogWriter = new CloudWatchLogWriter(configuration);
                if (this.layout == null) {
                    this.layout = new EchoLayout<>();
                    this.addStatus(new WarnStatus("No layout, default to " + this.layout, this));
                }
                LogbackConfiguration logbackConfiguration = new LogbackConfiguration(layout, encoder);
                worker = new Thread(new Worker(logs, cloudWatchLogWriter, logbackConfiguration));
                worker.setDaemon(true);
                worker.setName("CloudWatchAppender-Worker");
                layout.start();
                worker.start();
            } else {
                super.stop();
                addWarn("Failed to start CloudWatchAppender, missing configuration");
            }
        } catch (Exception e) {
            super.stop();
            addWarn("Failed to start CloudWatchAppender", e);
        }
    }

    @Override
    public void stop() {
        super.stop();
        if (worker != null) {
            try {
                worker.join();
            } catch (InterruptedException e) {
                worker.interrupt();
            }
            worker = null;
        }
        encoder.stop();
        layout.stop();
        logs.clear();
    }

    public void setLayout(Layout<ILoggingEvent> layout) {
        LayoutWrappingEncoder<ILoggingEvent> lwe = new LayoutWrappingEncoder<>();
        lwe.setLayout(layout);
        lwe.setContext(context);
        this.encoder = lwe;
    }

    public void setEncoder(Encoder<ILoggingEvent> encoder) {
        this.encoder = encoder;
    }

    public void setLogGroupName(String logGroupName) {
        this.logGroupName = logGroupName;
    }

    public void setLogStreamName(String logStreamName) {
        this.logStreamName = logStreamName;
    }

    public void setLogRegion(String logRegion) {
        this.logRegion = logRegion;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public void setSecretAccessKey(String secretAccessKey) {
        this.secretAccessKey = secretAccessKey;
    }
}
