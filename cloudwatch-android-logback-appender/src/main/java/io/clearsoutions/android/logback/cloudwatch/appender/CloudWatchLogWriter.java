package io.clearsoutions.android.logback.cloudwatch.appender;


import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.services.logs.AmazonCloudWatchLogsClient;
import com.amazonaws.services.logs.model.CreateLogGroupRequest;
import com.amazonaws.services.logs.model.CreateLogStreamRequest;
import com.amazonaws.services.logs.model.InputLogEvent;
import com.amazonaws.services.logs.model.InvalidSequenceTokenException;
import com.amazonaws.services.logs.model.PutLogEventsRequest;
import com.amazonaws.services.logs.model.PutLogEventsResult;
import com.amazonaws.services.logs.model.PutRetentionPolicyRequest;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import io.clearsoutions.android.logback.cloudwatch.appender.configuration.CloudWatchConfiguration;


public class CloudWatchLogWriter {

    private static final int MAX_MESSAGE_SIZE = 256 * 1024;
    private static final String THREE_DOTS = "...";
    private static final int PADDING = 42;//because this is answer to everything
    private static final int MAX_RETRIES = 3;

    private final CloudWatchConfiguration configuration;
    private final AmazonCloudWatchLogsClient cloudWatchLogsClient;
    private String sequenceToken;

    public CloudWatchLogWriter(CloudWatchConfiguration cloudWatchConfiguration) {
        this.configuration = cloudWatchConfiguration;
        this.cloudWatchLogsClient = build();
        initCloudWatchLogGroup();
    }

    public void write(List<LogEventDTO> logs) {
        writeWithRetry(logs, MAX_RETRIES);
    }
    private void writeWithRetry(List<LogEventDTO> logs, int retriesLeft) {
        try {
            PutLogEventsRequest request = putLogEventsRequest(logs);
            PutLogEventsResult putLogEventsResponse = cloudWatchLogsClient.putLogEvents(request);
            sequenceToken = putLogEventsResponse.getNextSequenceToken();
        } catch (InvalidSequenceTokenException ex) {
            if (retriesLeft > 0) {
                System.out.println("Invalid sequence token, retrying... Retries left: " + (retriesLeft - 1));
                sequenceToken = ex.getExpectedSequenceToken();
                writeWithRetry(logs, retriesLeft - 1);
            } else {
                System.err.println("Failed to write logs after " + MAX_RETRIES + " retries.");
            }
        } catch (Throwable t) {
            System.out.println("Failed to write logs");
        }
    }

    private PutLogEventsRequest putLogEventsRequest(List<LogEventDTO> logs) {
        List<InputLogEvent> logstream = new ArrayList<>();
        for (LogEventDTO log : logs) {
            String message = ensureNotLargerThan256KB(log.getMessage());
            InputLogEvent event = new InputLogEvent()
                    .withMessage(message)
                    .withTimestamp(log.getTimestamp());
            logstream.add(event);
        }

        PutLogEventsRequest request = new PutLogEventsRequest(
                configuration.getLogGroupName(),
                configuration.getLogStreamName(),
                logstream
        );
        // Add the sequenceToken if it is not null
        if (sequenceToken != null) {
            request = request.withSequenceToken(sequenceToken);
        }
        return request;
    }

    /**
     * Checks if a string is larger than 256 KB in UTF-8 encoding and cuts it to fit within this size if necessary.
     *
     * @param input The input string to check and possibly cut.
     * @return A string that is guaranteed to be less than or equal to 256 KB in UTF-8 byte size.
     * @see <a href="https://docs.aws.amazon.com/AmazonCloudWatchLogs/latest/APIReference/API_InputLogEvent.html">InputLogEvent</a>
     */
    public String ensureNotLargerThan256KB(String input) {
        byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);

        if (inputBytes.length <= MAX_MESSAGE_SIZE) {
            return input;
        }

        int cutSize = inputBytes.length - PADDING - THREE_DOTS.getBytes(StandardCharsets.UTF_8).length;

        String unsafeTrimmed = new String(inputBytes, 0, cutSize + 1, StandardCharsets.UTF_8);
        String cutString = unsafeTrimmed.substring(0, unsafeTrimmed.length() - 1);
        return cutString + THREE_DOTS;
    }

    private AmazonCloudWatchLogsClient build() {
        AWSCredentials awsCredentials = new BasicAWSCredentials(configuration.getAccessKeyId(), configuration.getSecretAccessKey());
        AWSCredentialsProvider awsCredentialsProvider = new StaticCredentialsProvider(awsCredentials);
        AmazonCloudWatchLogsClient client = new AmazonCloudWatchLogsClient(awsCredentialsProvider);
        client.setRegion(Region.getRegion(configuration.getLogRegion()));
        System.out.println("Creating  Client");
        return client;
    }

    private void initCloudWatchLogGroup() {
        try {
            CreateLogGroupRequest createLogGroupRequest = new CreateLogGroupRequest()
                    .withLogGroupName(configuration.getLogGroupName());
            cloudWatchLogsClient.createLogGroup(createLogGroupRequest);
            if (configuration.getRetentionTimeInDays() > 0) {
                PutRetentionPolicyRequest putRetentionPolicyRequest = new PutRetentionPolicyRequest()
                        .withLogGroupName(configuration.getLogGroupName())
                        .withRetentionInDays(configuration.getRetentionTimeInDays());
                cloudWatchLogsClient.putRetentionPolicy(putRetentionPolicyRequest);
            }
        } catch (Throwable e) {
            //TODO is it better check if the log group exists?
        }
        try {
            CreateLogStreamRequest createLogStreamRequest = new CreateLogStreamRequest()
                    .withLogGroupName(configuration.getLogGroupName())
                    .withLogStreamName(configuration.getLogStreamName());
            cloudWatchLogsClient.createLogStream(createLogStreamRequest);
        } catch (Throwable e) {
            //TODO is it better check if the log group exists?
        }
    }
}
