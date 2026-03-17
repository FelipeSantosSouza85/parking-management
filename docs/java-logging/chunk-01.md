# java-logging

> Source: 126-java-logging.md
> Chunk: 1/4
> Included sections: intro | Role | Goal | Constraints | Examples | Examples - Table of contents | Examples - Example 1: Choose an Appropriate Logging Framework | Examples - Example 2: Understand and Use Logging Levels Correctly

---
author: Juan Antonio Breña Moral
version: 0.12.0-SNAPSHOT
---
# Java Logging Best Practices

## Role

You are a Senior software engineer with extensive experience in Java software development

## Goal

Effective Java logging involves selecting a standard framework (SLF4J with Logback/Log4j2), using appropriate log levels (ERROR, WARN, INFO, DEBUG, TRACE),
and adhering to core practices like parameterized logging, proper exception handling, and avoiding sensitive data exposure.
Configuration should be environment-specific with clear output formats.
Security is paramount: mask sensitive data, control log access, and ensure secure transmission.
Implement centralized log aggregation, monitoring, and alerting for proactive issue detection.
Finally, logging behavior and its impact should be validated through comprehensive testing.

### Implementing These Principles

These guidelines are built upon the following core principles:

1. **Standardized Framework Selection**: Utilize a widely accepted logging facade (preferably SLF4J) and a robust underlying implementation (Logback or Log4j2). This promotes consistency, flexibility, and access to advanced logging features.
2. **Meaningful and Consistent Log Levels**: Employ logging levels (ERROR, WARN, INFO, DEBUG, TRACE) deliberately and consistently to categorize the severity and importance of messages. This allows for effective filtering, monitoring, and targeted issue diagnosis.
3. **Adherence to Core Logging Practices**: Follow fundamental best practices such as using parameterized logging (avoiding string concatenation for performance and clarity), always logging exceptions with their stack traces, never logging sensitive data directly (PII, credentials).
4. **Thoughtful and Flexible Configuration**: Manage logging configuration externally (e.g., `logback.xml`, `log4j2.xml`). Tailor configurations for different environments (dev, test, prod) with appropriate log levels for various packages, clear and informative output formats (including timestamps, levels, logger names, thread info), and robust log rotation and retention policies.
5. **Security-Conscious Logging**: Prioritize security in all logging activities. Actively mask or filter sensitive information, control access to log files and log management systems, use secure protocols for transmitting logs, and ensure compliance with relevant data protection regulations (e.g., GDPR, HIPAA).
6. **Proactive Log Monitoring and Alerting**: Implement centralized log aggregation systems (e.g., ELK Stack, Splunk, Grafana Loki). Establish automated alerts based on log patterns, error rates, or specific critical events to enable proactive issue detection and rapid response.
7. **Comprehensive Logging Validation Through Testing**: Integrate logging into the testing strategy. Assert that critical log messages (especially errors and warnings) are generated as expected under specific conditions, verify log formats, test log level filtering, and assess any performance impact of logging.

Remember, good logging in Java is about operational excellence - making your application's behavior transparent and debuggable while maintaining security and performance.

## Constraints

Before applying any recommendations, ensure the project is in a valid state by running Maven compilation. Compilation failure is a BLOCKING condition that prevents any further processing.

- **MANDATORY**: Run `./mvnw compile` or `mvn compile` before applying any change
- **PREREQUISITE**: Project must compile successfully and pass basic validation checks before any optimization
- **CRITICAL SAFETY**: If compilation fails, IMMEDIATELY STOP and DO NOT CONTINUE with any recommendations
- **BLOCKING CONDITION**: Compilation errors must be resolved by the user before proceeding with any object-oriented design improvements
- **NO EXCEPTIONS**: Under no circumstances should design recommendations be applied to a project that fails to compile

## Examples

### Table of contents

- Example 1: Choose an Appropriate Logging Framework
- Example 2: Understand and Use Logging Levels Correctly
- Example 3: Adhere to Core Logging Practices
- Example 4: Follow Configuration Best Practices
- Example 5: Implement Secure Logging Practices
- Example 6: Establish Effective Log Monitoring and Alerting
- Example 7: Incorporate Logging in Testing

### Example 1: Choose an Appropriate Logging Framework

Title: Select a Standard Logging Facade and Implementation
Description: Using a standard logging facade like SLF4J allows for flexibility in choosing and switching an underlying logging implementation. The primary recommendation is SLF4J with Logback for its robustness and feature-richness.

**Good example:**

```java
// GOOD: Using SLF4J facade with proper logger declaration
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Objects;

public class UserService {
    // Logger declared using SLF4J - static final per class
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    public void performAction(String input) {
        // SLF4J parameterized logging - efficient and readable
        logger.info("Performing action with input: {}", input);

        if (Objects.isNull(input) || input.isEmpty()) {
            logger.warn("Input is null or empty, using default behavior");
            input = "default";
        }

        try {
            processInput(input);
            logger.debug("Action completed successfully for input: {}", input);
        } catch (ProcessingException e) {
            logger.error("Failed to process input: {}", input, e);
            throw e;
        }
    }

    public void performComplexOperation(String userId, String operation) {
        // Using MDC (Mapped Diagnostic Context) for contextual logging
        try (var mdcCloseable = org.slf4j.MDC.putCloseable("userId", userId)) {
            logger.info("Starting operation: {}", operation);

            // All log statements in this block will include userId context
            performInternalSteps(operation);

            logger.info("Operation completed successfully");
        } catch (Exception e) {
            logger.error("Operation failed", e);
            throw e;
        }
    }

    private void processInput(String input) throws ProcessingException {
        // Simulate processing that might fail
        if ("error".equals(input)) {
            throw new ProcessingException("Invalid input: " + input);
        }
        logger.trace("Processing step completed for: {}", input);
    }

    private void performInternalSteps(String operation) {
        logger.debug("Executing internal step 1 for operation: {}", operation);
        // ... business logic
        logger.debug("Executing internal step 2 for operation: {}", operation);
        // ... more business logic
    }

    private static class ProcessingException extends Exception {
        public ProcessingException(String message) {
            super(message);
        }
    }
}
```

**Bad example:**

```java
// AVOID: Using System.out.println or direct logging implementation
public class BadLoggingService {

    public void performAction(String input) {
        // BAD: Using System.out.println - no control, no levels, no formatting
        System.out.println("Starting action with: " + input);

        if (input == null || input.isEmpty()) {
            // BAD: Using System.err - not integrated with logging framework
            System.err.println("Warning: Input is null or empty!");
        }

        try {
            processInput(input);
            System.out.println("Action completed for: " + input);
        } catch (Exception e) {
            // BAD: Just printing stack trace to stderr
            e.printStackTrace();
            // No structured logging, no log levels, no context
        }
    }

    // BAD: Directly using concrete logging implementation
    private java.util.logging.Logger julLogger =
        java.util.logging.Logger.getLogger(BadLoggingService.class.getName());

    public void anotherMethod() {
        // BAD: Tied to specific implementation, less flexible
        julLogger.info("Using JUL directly");
    }

    // BAD: Multiple logging frameworks in same class
    private org.apache.logging.log4j.Logger log4jLogger =
        org.apache.logging.log4j.LogManager.getLogger(BadLoggingService.class);

    public void confusedLogging() {
        julLogger.info("Using JUL");
        log4jLogger.info("Using Log4j");
        System.out.println("Using System.out");
        // Inconsistent and confusing!
    }
}
```

### Example 2: Understand and Use Logging Levels Correctly

Title: Apply Appropriate Logging Levels for Messages
Description: Use logging levels consistently to categorize the severity and importance of log messages. ERROR for critical issues, WARN for potentially harmful situations, INFO for important business events, DEBUG for detailed information, and TRACE for fine-grained debugging.

**Good example:**

```java
// GOOD: Proper usage of logging levels
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Objects;

public class OrderProcessor {
    private static final Logger logger = LoggerFactory.getLogger(OrderProcessor.class);

    public void processOrder(String orderId, String customerId) {
        logger.trace("Entering processOrder with orderId: {}, customerId: {}", orderId, customerId);

        // INFO: Important business events
        logger.info("Processing order {} for customer {}", orderId, customerId);

        try {
            // Validate inputs
            if (Objects.isNull(orderId) || orderId.trim().isEmpty()) {
                // WARN: Potentially harmful situation that can be recovered
                logger.warn("Order ID is null or empty for customer {}. Using generated ID.", customerId);
                orderId = generateOrderId();
            }

            // DEBUG: Detailed information for development/troubleshooting
            logger.debug("Validating order {} inventory", orderId);
            validateInventory(orderId);

            logger.debug("Calculating pricing for order {}", orderId);
            calculatePricing(orderId);

            logger.debug("Processing payment for order {}", orderId);
            processPayment(orderId);

            // INFO: Successful completion of important business operation
            logger.info("Order {} processed successfully for customer {}", orderId, customerId);

        } catch (InventoryException e) {
            // WARN: Expected business exception that can be handled
            logger.warn("Insufficient inventory for order {}. Will backorder.", orderId, e);
            handleBackorder(orderId);

        } catch (PaymentException e) {
            // ERROR: Critical failure requiring immediate attention
            logger.error("Payment processing failed for order {}. Customer: {}",
                        orderId, customerId, e);
            throw new OrderProcessingException("Payment failed", e);

        } catch (Exception e) {
            // ERROR: Unexpected critical error
            logger.error("Unexpected error processing order {} for customer {}",
                        orderId, customerId, e);
            throw new OrderProcessingException("Unexpected error", e);
        }

        logger.trace("Exiting processOrder for orderId: {}", orderId);
    }

    public void performHealthCheck() {
        logger.debug("Starting health check");

        try {
            checkDatabaseConnection();
            checkExternalServices();

            // INFO: System status information
            logger.info("Health check passed - all systems operational");

        } catch (HealthCheckException e) {
            // ERROR: System health issue requiring immediate attention
            logger.error("Health check failed - system may be degraded", e);
        }
    }

    // Example methods
    private String generateOrderId() { return "ORD-" + System.currentTimeMillis(); }
    private void validateInventory(String orderId) throws InventoryException { /* ... */ }
    private void calculatePricing(String orderId) { /* ... */ }
    private void processPayment(String orderId) throws PaymentException { /* ... */ }
    private void handleBackorder(String orderId) { /* ... */ }
    private void checkDatabaseConnection() throws HealthCheckException { /* ... */ }
    private void checkExternalServices() throws HealthCheckException { /* ... */ }

    // Exception classes
    private static class InventoryException extends Exception {
        public InventoryException(String message) { super(message); }
    }
    private static class PaymentException extends Exception {
        public PaymentException(String message) { super(message); }
    }
    private static class OrderProcessingException extends RuntimeException {
        public OrderProcessingException(String message, Throwable cause) { super(message, cause); }
    }
    private static class HealthCheckException extends Exception {
        public HealthCheckException(String message) { super(message); }
    }
}
```

**Bad example:**

```java
// AVOID: Misusing logging levels
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BadLevelUsage {
    private static final Logger logger = LoggerFactory.getLogger(BadLevelUsage.class);

    public void processUser(String userId) {
        // BAD: Using INFO for debug-level details
        logger.info("Method entry: processUser with parameter: " + userId);
        logger.info("Creating new StringBuilder object");
        logger.info("Checking if userId is null");

        if (userId == null) {
            // BAD: Using ERROR for a normal business condition
            logger.error("User ID is null!"); // This might be expected/handled
            return;
        }

        // BAD: Using WARN for normal flow information
        logger.warn("User ID is not null, proceeding with processing");

        try {
            String result = processUserData(userId);

            // BAD: Using ERROR for successful operations
            logger.error("Successfully processed user: " + userId + " with result: " + result);

        } catch (Exception e) {
            // BAD: Using INFO for critical errors
            logger.info("An error occurred: " + e.getMessage());
            // Should be ERROR or WARN depending on severity
        }

        // BAD: Overuse of TRACE for everything
        logger.trace("About to return from method");
        logger.trace("Setting return value to void");
        logger.trace("Method execution completed");
        // Too much noise - not useful
    }

    public void anotherBadExample(String data) {
        // BAD: String concatenation instead of parameterized logging
        logger.info("Processing data: " + data + " at time: " + System.currentTimeMillis());

        // BAD: Using DEBUG for important business events
        logger.debug("Payment of $1000 processed for customer ABC123");
        // This should be INFO - it's an important business event

        // BAD: Using same level for different severity issues
        logger.warn("Configuration file not found, using defaults");  // This is OK for WARN
        logger.warn("Database connection lost");  // This should be ERROR
        logger.warn("User entered invalid email format");  // This might be INFO or DEBUG
    }

    private String processUserData(String userId) {
        return "processed-" + userId;
    }
}
```
