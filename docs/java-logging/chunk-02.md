# java-logging

> Source: 126-java-logging.md
> Chunk: 2/4
> Included sections: Examples - Example 3: Adhere to Core Logging Practices | Examples - Example 4: Follow Configuration Best Practices

### Example 3: Adhere to Core Logging Practices

Title: Implement Fundamental Best Practices
Description: Follow core practices including using parameterized logging, proper exception handling, avoiding sensitive data exposure, and implementing performance considerations for logging operations.

**Good example:**

```java
// GOOD: Core logging best practices
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import java.util.Objects;

public class SecureTransactionService {
    private static final Logger logger = LoggerFactory.getLogger(SecureTransactionService.class);

    public void processPayment(String userId, String amount, String cardNumber) {
        // Set correlation ID for request tracing
        String correlationId = generateCorrelationId();
        MDC.put("correlationId", correlationId);

        try {
            logger.info("Processing payment for user: {}, amount: {}", userId, amount);

            // GOOD: Mask sensitive information before logging
            String maskedCard = maskCreditCard(cardNumber);
            logger.debug("Processing payment with card: {}", maskedCard);

            validatePaymentRequest(userId, amount, cardNumber);

            if (logger.isDebugEnabled()) {
                // GOOD: Guard clause for expensive operations
                String debugInfo = buildComplexDebugInfo(userId, amount);
                logger.debug("Payment validation details: {}", debugInfo);
            }

            processPaymentInternal(userId, amount, cardNumber);

            logger.info("Payment processed successfully for user: {}, correlation: {}",
                       userId, correlationId);

        } catch (ValidationException e) {
            // GOOD: Log exception with context but don't expose sensitive data
            logger.warn("Payment validation failed for user: {}, reason: {}, correlation: {}",
                       userId, e.getValidationError(), correlationId, e);
            throw e;

        } catch (PaymentProcessingException e) {
            // GOOD: Log critical error with full context
            logger.error("Payment processing failed for user: {}, amount: {}, correlation: {}",
                        userId, amount, correlationId, e);
            throw e;

        } catch (Exception e) {
            // GOOD: Catch unexpected exceptions and log with context
            logger.error("Unexpected error during payment processing for user: {}, correlation: {}",
                        userId, correlationId, e);
            throw new PaymentProcessingException("Unexpected error", e);

        } finally {
            // GOOD: Clean up MDC to prevent memory leaks
            MDC.remove("correlationId");
        }
    }

    public void processLargeDataSet(List<String> dataItems) {
        logger.info("Processing {} data items", dataItems.size());

        for (int i = 0; i < dataItems.size(); i++) {
            String item = dataItems.get(i);

            try {
                processDataItem(item);

                // GOOD: Log progress periodically, not for every item
                if (i % 1000 == 0) {
                    logger.debug("Processed {} of {} items", i, dataItems.size());
                }

            } catch (Exception e) {
                // GOOD: Log error but continue processing
                logger.warn("Failed to process item at index {}: {}", i, item, e);
            }
        }

        logger.info("Completed processing {} data items", dataItems.size());
    }

    // Utility methods
    private String maskCreditCard(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 8) {
            return "****";
        }
        return cardNumber.substring(0, 4) + "****" + cardNumber.substring(cardNumber.length() - 4);
    }

    private String generateCorrelationId() {
        return "TXN-" + System.currentTimeMillis() + "-" + Thread.currentThread().getId();
    }

    private String buildComplexDebugInfo(String userId, String amount) {
        // Simulate expensive debug information building
        return String.format("User: %s, Amount: %s, Timestamp: %d",
                           userId, amount, System.currentTimeMillis());
    }

    private void validatePaymentRequest(String userId, String amount, String cardNumber)
            throws ValidationException {
        if (Objects.isNull(userId) || userId.trim().isEmpty()) {
            throw new ValidationException("INVALID_USER_ID");
        }
        // More validation...
    }

    private void processPaymentInternal(String userId, String amount, String cardNumber)
            throws PaymentProcessingException {
        // Simulate payment processing
        if ("fail".equals(userId)) {
            throw new PaymentProcessingException("Payment gateway error");
        }
    }

    private void processDataItem(String item) {
        // Simulate data processing
        if ("error".equals(item)) {
            throw new RuntimeException("Processing failed for item: " + item);
        }
    }

    // Exception classes
    private static class ValidationException extends Exception {
        private final String validationError;

        public ValidationException(String validationError) {
            super("Validation failed: " + validationError);
            this.validationError = validationError;
        }

        public String getValidationError() {
            return validationError;
        }
    }

    private static class PaymentProcessingException extends RuntimeException {
        public PaymentProcessingException(String message) {
            super(message);
        }

        public PaymentProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
```

**Bad example:**

```java
// AVOID: Poor logging practices
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PoorLoggingPractices {
    private static final Logger logger = LoggerFactory.getLogger(PoorLoggingPractices.class);

    public void processPayment(String userId, String amount, String cardNumber, String ssn) {
        // BAD: String concatenation instead of parameterized logging
        logger.info("Processing payment for user: " + userId + " amount: " + amount);

        // BAD: Logging sensitive information directly
        logger.debug("Credit card: " + cardNumber + ", SSN: " + ssn);

        try {
            validatePayment(userId, amount);

            // BAD: Expensive operation without guard clause
            logger.debug("Payment details: " + buildExpensiveDebugString(userId, amount, cardNumber));

            processPaymentTransaction(userId, amount, cardNumber);

        } catch (Exception e) {
            // BAD: Swallowing exception without proper logging
            logger.info("Payment failed: " + e.getMessage());
            // Lost the stack trace and context!

            // BAD: Logging sensitive data in error message
            logger.error("Payment failed for card: " + cardNumber + " and SSN: " + ssn);
        }
    }

    public void processLargeDataSet(List<String> items) {
        // BAD: Logging every single item in large dataset
        for (String item : items) {
            logger.debug("Processing item: " + item); // Will flood logs
            processItem(item);
            logger.debug("Completed item: " + item); // Even more noise
        }
    }

    public void handleUserLogin(String username, String password) {
        // BAD: Logging passwords - NEVER do this!
        logger.debug("User login attempt: username=" + username + ", password=" + password);

        try {
            authenticateUser(username, password);
            // BAD: Inconsistent logging format
            logger.info("User " + username + " logged in successfully");
        } catch (AuthenticationException e) {
            // BAD: Using wrong log level and exposing sensitive info
            logger.error("Login failed for " + username + " with password " + password);
        }
    }

    public void performDatabaseOperation(String query, String connectionString) {
        // BAD: Logging database connection strings (may contain credentials)
        logger.debug("Executing query: " + query + " on connection: " + connectionString);

        try {
            executeQuery(query);
        } catch (SQLException e) {
            // BAD: Not using parameterized logging with exception
            logger.error("SQL error: " + e.getMessage() + " for query: " + query);
            // Stack trace is lost!
        }
    }

    // BAD: No try-with-resources or proper cleanup for MDC
    public void badMDCUsage(String userId) {
        org.slf4j.MDC.put("userId", userId);
        logger.info("Processing for user");

        // ... some processing ...

        // BAD: Forgot to clear MDC - memory leak!
        // MDC.clear() or MDC.remove("userId") is missing
    }

    // Helper methods with poor exception handling
    private String buildExpensiveDebugString(String userId, String amount, String cardNumber) {
        // Simulate expensive operation
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            sb.append("Debug info for ").append(userId).append(" ");
        }
        return sb.toString();
    }

    private void validatePayment(String userId, String amount) throws ValidationException {
        if (userId == null) throw new ValidationException("Invalid user");
    }

    private void processPaymentTransaction(String userId, String amount, String cardNumber) {
        // Simulate processing
    }

    private void processItem(String item) {
        // Simulate processing
    }

    private void authenticateUser(String username, String password) throws AuthenticationException {
        if ("baduser".equals(username)) {
            throw new AuthenticationException("Invalid credentials");
        }
    }

    private void executeQuery(String query) throws SQLException {
        if (query.contains("DROP")) {
            throw new SQLException("Invalid query");
        }
    }

    // Exception classes
    private static class ValidationException extends Exception {
        public ValidationException(String message) { super(message); }
    }
    private static class AuthenticationException extends Exception {
        public AuthenticationException(String message) { super(message); }
    }
    private static class SQLException extends Exception {
        public SQLException(String message) { super(message); }
    }
}
```

### Example 4: Follow Configuration Best Practices

Title: Configure Your Logging Framework Thoughtfully
Description: Proper configuration is key to effective logging. Use separate configurations per environment, implement different log levels for different packages, and include comprehensive output formats with timestamps, levels, logger names, and thread information.

**Good example:**

```java
// Configuration shown through usage - actual config would be in logback.xml or log4j2.xml
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class ConfiguredLoggerExample {
    private static final Logger appLogger = LoggerFactory.getLogger(ConfiguredLoggerExample.class);
    private static final Logger performanceLogger = LoggerFactory.getLogger("performance." + ConfiguredLoggerExample.class.getName());

    public void performBusinessOperation(String operationId, String userId) {
        // Using MDC for contextual information
        MDC.put("operationId", operationId);
        MDC.put("userId", userId);

        try {
            long startTime = System.currentTimeMillis();
            appLogger.info("Starting business operation");

            // Simulate work
            Thread.sleep(100);

            long duration = System.currentTimeMillis() - startTime;
            performanceLogger.info("Operation completed in {} ms", duration);

            appLogger.info("Business operation completed successfully");

        } catch (InterruptedException e) {
            appLogger.warn("Operation interrupted", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            appLogger.error("Business operation failed", e);
            throw new RuntimeException("Operation failed", e);
        } finally {
            // Always clear MDC
            MDC.clear();
        }
    }

    public void demonstrateLogLevels() {
        // These will be filtered based on configuration
        appLogger.trace("This is trace level - very detailed");
        appLogger.debug("This is debug level - detailed for development");
        appLogger.info("This is info level - important business events");
        appLogger.warn("This is warn level - potentially harmful situations");
        appLogger.error("This is error level - critical issues");
    }

    public static void main(String[] args) {
        ConfiguredLoggerExample example = new ConfiguredLoggerExample();
        example.performBusinessOperation("OP123", "user456");
        example.demonstrateLogLevels();
    }
}
```

**Bad example:**

```java
// Poor configuration consequences
public class BadConfigConsequences {
    private static final Logger logger = LoggerFactory.getLogger(BadConfigConsequences.class);

    public void performOperation() {
        // If configuration is bad (everything at TRACE), this floods logs
        logger.trace("Entering method");
        logger.trace("Creating variables");
        logger.trace("About to check condition");

        logger.info("User logged in.");
        logger.error("Failed to connect to DB.", new RuntimeException("DB connection timeout"));

        // With bad pattern configuration, output might be:
        // User logged in.
        // Failed to connect to DB.
        // Problem: No timestamp, no level, no logger name, no thread info
    }

    public static void main(String[] args) {
        BadConfigConsequences example = new BadConfigConsequences();
        example.performOperation();
    }
}
```
