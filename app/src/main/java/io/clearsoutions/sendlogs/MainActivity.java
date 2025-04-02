package io.clearsoutions.sendlogs;

import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.ConsoleAppender;
import io.clearsoutions.android.logback.cloudwatch.appender.CloudWatchAppender;


public class MainActivity extends AppCompatActivity {

    private final static Logger log = LoggerFactory.getLogger(MainActivity.class);
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final CountDownLatch loggingLatch = new CountDownLatch(1);

    static {
        initializeLogging();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        TextView textView = findViewById(R.id.textView);
        textView.setText("Starting log configuration...");
        try {
            boolean await = loggingLatch.await(2, TimeUnit.SECONDS);
            log.info("Test Application started {}, {}", new Date(), await);
        } catch (Throwable ex) {
            System.out.println("Failed to log message: " + ex.getMessage());
        }

        textView.setText("Finished log configuration");
    }

    private static void initializeLogging() {
        // Start configuration in background thread
        executor.execute(() -> {
            try {
                configureLogback();
                // Mark as configured and release waiting threads
                loggingLatch.countDown();
            } catch (Throwable ex) {
                loggingLatch.countDown();
            }
        });
    }

    private static void configureLogback() {
        try {
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            // Create a PatternLayoutEncoder
            PatternLayoutEncoder encoder = new PatternLayoutEncoder();
            encoder.setContext(loggerContext);
            encoder.setPattern("%-5p %t %c{1}:%L - %m%n");
            encoder.setOutputPatternAsHeader(true);
            encoder.start();

            // Console Appender, need to use without it, android did not show the line numbers
            ConsoleAppender consoleAppender = new ConsoleAppender<>();
            consoleAppender.setContext(loggerContext);
            consoleAppender.setEncoder(encoder);
            consoleAppender.start();

            CloudWatchAppender cloudWatchAppender = new CloudWatchAppender();
            cloudWatchAppender.setContext(loggerContext);
            cloudWatchAppender.setEncoder(encoder);
            cloudWatchAppender.setLayout(encoder.getLayout());

            cloudWatchAppender.setLogGroupName("verifone-test-log-group");// example
            cloudWatchAppender.setLogStreamName("device_test_guid");
            cloudWatchAppender.setLogRegion("eu-central-1");
            cloudWatchAppender.setAccessKeyId(BuildConfig.CLOUDWATCH_ACCESS_KEY_ID);
            cloudWatchAppender.setSecretAccessKey(BuildConfig.CLOUDWATCH_SECRET_ACCESS_KEY);
            // Add the appender to the root logger
            ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger("ROOT");
            rootLogger.addAppender(consoleAppender);
            rootLogger.addAppender(cloudWatchAppender);
            cloudWatchAppender.start();

            log.info("Logback configured successfully in {}", 10);
        } catch (Throwable ex) {
            System.err.println("Failed to configure logback");
        }
    }
}