package io.clearsoutions.android.logback.cloudwatch.appender;

public class LogEventDTO {

    private final String message;
    private final long timestamp;

    public LogEventDTO(String message, long timestamp) {
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
