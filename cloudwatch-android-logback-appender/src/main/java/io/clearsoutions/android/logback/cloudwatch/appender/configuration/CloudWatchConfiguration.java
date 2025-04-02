package io.clearsoutions.android.logback.cloudwatch.appender.configuration;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class CloudWatchConfiguration {

    private final String logGroupName;
    private final String logStreamName;
    private final String logRegion;
    private final String accessKeyId;
    private final String secretAccessKey;
    private final int retentionTimeInDays;

    public CloudWatchConfiguration(String logGroupName,
                                   String logStreamName,
                                   String logRegion,
                                   String accessKeyId,
                                   String secretAccessKey,
                                   int retentionTimeInDays) {

        this.logGroupName = removeUndefinedValue(createLogGroupName(logGroupName));
        this.logStreamName = removeUndefinedValue(createLogStreamName(logStreamName));
        this.logRegion = removeUndefinedValue(logRegion);
        this.accessKeyId = removeUndefinedValue(accessKeyId);
        this.secretAccessKey = removeUndefinedValue(secretAccessKey);
        this.retentionTimeInDays = retentionTimeInDays;
    }

    private String removeUndefinedValue(String value) {
        if (isNull(value) || value.contains("UNDEFINED")) {
            return null;
        }
        return value;
    }

    public boolean isConfigured() {
        return nonNull(logGroupName)
               && nonNull(logStreamName)
               && nonNull(logRegion)
               && nonNull(accessKeyId)
               && nonNull(secretAccessKey);
    }

    String createLogGroupName(String logGroupName) {
        if (isNull(logGroupName)) {
            return "logback";
        }

        return logGroupName;
    }

    String createLogStreamName(String logStreamName) {
        return logStreamName;
    }

    public String getLogGroupName() {
        return logGroupName;
    }

    public String getLogStreamName() {
        return logStreamName;
    }

    public String getLogRegion() {
        return logRegion;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public String getSecretAccessKey() {
        return secretAccessKey;
    }

    public int getRetentionTimeInDays() {
        return retentionTimeInDays;
    }
}
