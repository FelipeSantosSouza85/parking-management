# java-logging

> Source: 126-java-logging.md
> Chunk: 4/4
> Included sections: Examples - Example 6: Establish Effective Log Monitoring and Alerting | Examples - Example 7: Incorporate Logging in Testing | Output Format | Safeguards

### Example 6: Establish Effective Log Monitoring and Alerting

Title: Implement Log Aggregation, Monitoring, and Alerting
Description: Set up centralized log aggregation, implement alerts based on log patterns, use structured logging for querying, and regularly analyze logs to identify trends and issues.

**Good example:**

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import java.util.Objects;
import java.util.UUID;

public class MonitoredService {
    private static final Logger logger = LoggerFactory.getLogger(MonitoredService.class);

    public void handleApiRequest(String endpoint, String payload) {
        String requestId = UUID.randomUUID().toString();
        MDC.put("requestId", requestId);
        MDC.put("endpoint", endpoint);

        try {
            logger.info("API request received");

            if (payload.contains("<script>")) {
                // Log a security-sensitive event that monitoring tools can pick up for alerts.
                logger.warn("SECURITY_ALERT: Potential XSS attempt detected in payload for endpoint: {}", endpoint);
                // Handle error, e.g., return 400 Bad Request
                return;
            }

            // Simulate some processing
            if (endpoint.equals("/critical_op")) {
                if (Math.random() > 0.9) { // Simulate a sporadic critical failure
                    throw new RuntimeException("Critical operation failure detected!");
                }
            }

            logger.info("API request processed successfully");

            // Structured logging for metrics and monitoring
            logger.info("METRICS: endpoint={}, duration={}, status=success", endpoint, System.currentTimeMillis() % 1000);

        } catch (RuntimeException e) {
            // This ERROR log (especially with stack trace) would be a key candidate for alerting.
            logger.error("ALERT: Unhandled exception during API request processing", e);
            // Could trigger immediate alert in monitoring system
        } finally {
            MDC.clear();
        }
    }

    public void performHealthCheck() {
        try {
            // Simulate health checks
            checkDatabase();
            checkExternalService();

            logger.info("HEALTH_CHECK: All systems operational");

        } catch (Exception e) {
            logger.error("HEALTH_CHECK: System health check failed", e);
            // This would trigger alerts for operational teams
        }
    }

    private void checkDatabase() {
        // Simulate database check
        if (Math.random() > 0.95) {
            throw new RuntimeException("Database connection failed");
        }
    }

    private void checkExternalService() {
        // Simulate external service check
        if (Math.random() > 0.98) {
            throw new RuntimeException("External service unavailable");
        }
    }

    public static void main(String[] args) {
        MonitoredService service = new MonitoredService();

        // These logs would be searchable in monitoring systems by:
        // - requestId for request tracing
        // - endpoint for API-specific analysis
        // - SECURITY_ALERT for security incident detection
        // - HEALTH_CHECK for system monitoring
        // - METRICS for performance analysis

        service.handleApiRequest("/user_data", "{\"data\": \"normal\"}");
        service.handleApiRequest("/submit_form", "payload with <script>alert('XSS')</script>");

        for (int i = 0; i < 20; i++) {
            service.handleApiRequest("/critical_op", "some_data");
        }

        service.performHealthCheck();
    }
}
```

**Bad example:**

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Objects;

public class UnmonitoredService {
    private static final Logger logger = LoggerFactory.getLogger(UnmonitoredService.class);

    public void doWork() {
        try {
            logger.info("Work started.");
            // ... some logic ...
            if (Math.random() > 0.5) {
                throw new Exception("Something went wrong randomly!");
            }
            logger.info("Work finished.");
        } catch (Exception e) {
            // Logs an error, but if logs are only on local disk and not monitored,
            // this critical issue might go unnoticed for a long time.
            logger.error("Error during work", e);
        }
    }

    public static void main(String[] args) {
        UnmonitoredService service = new UnmonitoredService();
        for (int i = 0; i < 5; i++) {
            service.doWork();
        }
        // Problem: Logs are likely just going to console or a local file.
        // - No central aggregation: Difficult to search across instances or time.
        // - No alerts: Critical errors might be missed until users report them.
        // - No analysis: Trends or recurring non-fatal issues are hard to spot.
    }
}
```

### Example 7: Incorporate Logging in Testing

Title: Validate Logging Behavior and Impact During Testing
Description: Ensure logging works as expected and doesn't negatively impact the application. Assert that specific log messages are generated, verify log formats, test different logging levels, and check performance impact.

**Good example:**

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Objects;

// ---- System Under Test ----
class ImportantService {
    private static final Logger logger = LoggerFactory.getLogger(ImportantService.class);

    public void processImportantData(String dataId) {
        if (Objects.isNull(dataId)) {
            logger.error("Data ID is null, cannot process.");
            throw new IllegalArgumentException("Data ID cannot be null");
        }
        if (dataId.startsWith("invalid_")) {
            logger.warn("Received potentially invalid data ID: {}", dataId);
            // continue processing with caution
        }
        logger.info("Processing data for ID: {}", dataId);
        // ... actual processing ...
    }
}

// ---- Test Example (Conceptual - would use JUnit/TestNG) ----
public class LoggingTestExample {

    // This would be actual test code using testing frameworks
    public void demonstrateLoggingTests() {
        ImportantService service = new ImportantService();

        // Test 1: Verify ERROR log for null input
        try {
            service.processImportantData(null);
            // Should not reach here
        } catch (IllegalArgumentException e) {
            // In real test, would capture logs and assert:
            // - ERROR level log contains "Data ID is null"
            // - Only one ERROR log entry
            System.out.println("Test 1 passed: ERROR log generated for null input");
        }

        // Test 2: Verify WARN log for invalid input
        service.processImportantData("invalid_XYZ");
        // In real test, would assert:
        // - WARN level log contains "Received potentially invalid data ID: invalid_XYZ"
        // - INFO level log contains "Processing data for ID: invalid_XYZ"
        System.out.println("Test 2 passed: WARN and INFO logs generated for invalid input");

        // Test 3: Verify INFO log for valid input
        service.processImportantData("valid_123");
        // In real test, would assert:
        // - Only INFO level log contains "Processing data for ID: valid_123"
        // - No WARN or ERROR logs
        System.out.println("Test 3 passed: INFO log generated for valid input");
    }

    public void performanceTestExample() {
        ImportantService service = new ImportantService();

        // Performance test: measure logging overhead
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < 1000; i++) {
            service.processImportantData("test_" + i);
        }

        long duration = System.currentTimeMillis() - startTime;
        System.out.println("Performance test: 1000 operations completed in " + duration + "ms");

        // In real test, would assert that logging overhead is within acceptable limits
        // e.g., assert duration < 1000; // Less than 1ms per operation including logging
    }

    public static void main(String[] args) {
        LoggingTestExample test = new LoggingTestExample();
        test.demonstrateLoggingTests();
        test.performanceTestExample();

        System.out.println("In real implementation, use JUnit/TestNG with LogCapture utilities");
        System.out.println("Example dependencies: ch.qos.logback.classic.Logger with ListAppender");
    }
}
```

**Bad example:**

```java
// Code that is hard to test for logging behavior
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Objects;

public class UntestableLogging {
    private static final Logger logger = LoggerFactory.getLogger(UntestableLogging.class);

    public void complexOperation(String input) {
        // ... many lines of code ...
        if (input.equals("problem")) {
            // Log message is deeply embedded, possibly conditional, hard to trigger specifically
            // and verify without significant effort or refactoring.
            logger.warn("A specific problem occurred with input: {}", input);
        }
        // ... more code ...
        // No clear separation of concerns, making it hard to isolate and test logging.
    }

    public void methodWithSideEffects(String data) {
        // Expensive logging operation always executed
        logger.debug("Processing data: {}", buildExpensiveLogMessage(data));

        // No way to test if logging is impacting performance
        // No way to verify log content without running expensive operations
    }

    private String buildExpensiveLogMessage(String data) {
        // Simulate expensive operation that shouldn't run in production
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            sb.append("Debug info: ").append(data).append(" iteration: ").append(i);
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        UntestableLogging ul = new UntestableLogging();
        ul.complexOperation("test");
        ul.complexOperation("problem");
        ul.methodWithSideEffects("example");

        // Problem: Without proper test utilities or testable design,
        // verifying that the WARN message is logged correctly (and only when expected)
        // is difficult. Developers might skip testing logging, leading to unverified log output.
    }
}
```

## Output Format

- **ANALYZE** Java code to identify specific logging issues and categorize them by impact (CRITICAL, SECURITY, PERFORMANCE, MAINTAINABILITY, OBSERVABILITY) and logging area (framework selection, log levels, security, configuration, performance)
- **CATEGORIZE** logging improvements found: Framework Issues (inconsistent frameworks vs standardized SLF4J, outdated implementations vs modern logging), Log Level Problems (inappropriate levels vs proper level usage, missing contextual information vs structured logging), Security Issues (sensitive data exposure vs secure logging practices, plaintext credentials vs masked data), Configuration Problems (hardcoded settings vs environment-specific configuration, missing structured formats vs JSON/structured output), and Performance Issues (synchronous logging vs asynchronous patterns, excessive logging overhead vs optimized performance, string concatenation vs parameterized messages)
- **APPLY** logging best practices directly by implementing the most appropriate improvements for each identified issue: Standardize on SLF4J facade with appropriate implementation (Logback/Log4j2), implement proper log level usage with contextual information, secure sensitive data through masking and filtering, configure environment-specific settings with external configuration files, optimize performance through asynchronous logging and parameterized messages, and establish structured logging with JSON output for better parsing and analysis
- **IMPLEMENT** comprehensive logging refactoring using proven patterns: Migrate to SLF4J facade with consistent logger declarations, establish proper log level hierarchy (ERROR for failures, WARN for degraded service, INFO for business events, DEBUG for troubleshooting), implement secure logging practices with data masking and filtering, configure environment-specific logging with external properties, optimize performance through async appenders and lazy evaluation, and integrate structured logging with correlation IDs and contextual metadata
- **REFACTOR** code systematically following the logging improvement roadmap: First standardize logging framework usage through SLF4J adoption, then optimize log level usage and add proper contextual information, implement security measures for sensitive data protection, configure environment-specific logging settings with external configuration, optimize logging performance through asynchronous patterns and parameterized messages, and establish comprehensive monitoring and alerting integration
- **EXPLAIN** the applied logging improvements and their benefits: Framework standardization benefits through SLF4J facade adoption and consistent logger usage, observability enhancements via proper log levels and structured output, security improvements through sensitive data masking and secure logging practices, performance optimizations from asynchronous logging and parameterized messages, and operational benefits from environment-specific configuration and monitoring integration
- **VALIDATE** that all applied logging refactoring compiles successfully, maintains existing functionality, eliminates security vulnerabilities, follows logging best practices, and achieves the intended observability and performance improvements through comprehensive testing and verification

## Safeguards

- **BLOCKING SAFETY CHECK**: ALWAYS run `./mvnw compile` before ANY logging recommendations
- **CRITICAL VALIDATION**: Execute `./mvnw clean verify` to ensure all tests pass after logging changes
- **SECURITY VERIFICATION**: Validate that no sensitive data (passwords, PII, tokens) is logged directly
- **PERFORMANCE MONITORING**: Ensure logging configurations don't introduce significant performance overhead
- **CONFIGURATION VALIDATION**: Verify logging configuration files are syntactically correct and environment-appropriate
- **ROLLBACK READINESS**: Ensure all logging changes can be easily reverted without system disruption
- **INCREMENTAL SAFETY**: Apply logging improvements incrementally, validating after each modification
- **LOG LEVEL VERIFICATION**: Confirm log levels are appropriate for production environments (avoid DEBUG/TRACE in prod)
- **DEPENDENCY COMPATIBILITY**: Verify logging framework dependencies don't conflict with existing project dependencies
- **OPERATIONAL CONTINUITY**: Ensure logging changes don't break existing monitoring, alerting, or log aggregation systems
