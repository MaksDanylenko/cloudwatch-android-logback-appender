# CloudWatch Android Logback Appender

A lightweight Logback appender for sending logs to AWS CloudWatch, designed for Android applications.

## 🚀 Features

- Fully compatible with `logback-classic:1.2.3`
- Supports Android with `minSdkVersion = 19`
- Built with Java 8 compatibility
- Easy integration using JitPack
- Automatically syncs appender version with the Logback version for consistency

## 📦 Installation

Add JitPack to your project-level `settings.gradle` or `settings.gradle.kts`:

```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

Then, in your `build.gradle` (app-level):

```groovy
dependencies {
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("com.github.clear-solutions:cloudwatch-android-logback-appender:1.2.3")
}
```

> ℹ️ Make sure the versions match. This appender is versioned in sync with Logback — version `1.2.3` of this appender is compatible with `logback-classic:1.2.3`.

## ⚙️ Requirements

- Java 8 (source and target compatibility)
- Android `minSdkVersion 19` or higher

## 📘 Usage

You can configure the CloudWatch appender programmatically as shown below:

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.ConsoleAppender;
import io.clearsoutions.android.logback.cloudwatch.appender.CloudWatchAppender;

LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

// Create a PatternLayoutEncoder
PatternLayoutEncoder encoder = new PatternLayoutEncoder();
encoder.setContext(loggerContext);
encoder.setPattern("%-5p %t %c{1}:%L - %m%n");
encoder.setOutputPatternAsHeader(true);
encoder.start();

// Console Appender – required to see logs with line numbers on Android
ConsoleAppender consoleAppender = new ConsoleAppender<>();
consoleAppender.setContext(loggerContext);
consoleAppender.setEncoder(encoder);
consoleAppender.start();

// Set up CloudWatch Appender
CloudWatchAppender cloudWatchAppender = new CloudWatchAppender();
cloudWatchAppender.setContext(loggerContext);
cloudWatchAppender.setEncoder(encoder);
cloudWatchAppender.setLayout(encoder.getLayout());

cloudWatchAppender.setLogGroupName("verifone-test-log-group"); // example
cloudWatchAppender.setLogStreamName("device_test_guid");
cloudWatchAppender.setLogRegion("eu-central-1");
cloudWatchAppender.setAccessKeyId(BuildConfig.CLOUDWATCH_ACCESS_KEY_ID);
cloudWatchAppender.setSecretAccessKey(BuildConfig.CLOUDWATCH_SECRET_ACCESS_KEY);

// Attach appenders to the root logger
ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger("ROOT");
rootLogger.addAppender(consoleAppender);
rootLogger.addAppender(cloudWatchAppender);

// Start the CloudWatch appender
cloudWatchAppender.start();
```

> 🔐 Make sure to keep your AWS credentials secure. It's recommended to use `BuildConfig` or another secure storage strategy.

This setup will send logs to both the Android console (for development/debugging) and to AWS CloudWatch.

## 🔐 How to create access and secret keys
[How to Create AWS Keys](HowToCreateAwsKeys.md)

## 🔄 Versioning

This appender follows the same versioning as the Logback version it's compatible with.

For example:
- Appender `1.2.3` → Compatible with `logback-classic:1.2.3`
- Appender `1.2.10` → Compatible with `logback-classic:1.2.10`

If you're using a different version of Logback, simply match it with the same version of this appender (if available).

## 📞 Support

Feel free to open an issue or contact us via the GitHub repository if you encounter any problems or need enhancements.
