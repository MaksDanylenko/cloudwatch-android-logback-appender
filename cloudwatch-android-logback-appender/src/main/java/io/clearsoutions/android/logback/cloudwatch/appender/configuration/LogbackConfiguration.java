package io.clearsoutions.android.logback.cloudwatch.appender.configuration;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.encoder.Encoder;

public class LogbackConfiguration {

    private final Layout<ILoggingEvent> layout;
    private final Encoder<ILoggingEvent> encoder;

    public LogbackConfiguration(Layout<ILoggingEvent> layout, Encoder<ILoggingEvent> encoder) {
        this.layout = layout;
        this.encoder = encoder;
    }

    public Layout<ILoggingEvent> getLayout() {
        return layout;
    }

    public Encoder<ILoggingEvent> getEncoder() {
        return encoder;
    }
}
