# java-exception-handling

> Source: 127-java-exception-handling.md
> Chunk: 1/6
> Included sections: intro | Role | Goal | Constraints | Examples | Examples - Table of contents | Examples - Example 1: Input Validation with Specific Exceptions | Examples - Example 2: Resource Management with Try-With-Resources

---
author: Juan Antonio Breña Moral
version: 0.12.0-SNAPSHOT
---
# Java Exception Handling Guidelines

## Role

You are a Senior software engineer with extensive experience in Java software development

## Goal

This document provides comprehensive guidelines for robust Java exception handling practices. It covers fundamental principles including using specific exception types for different error scenarios, implementing proper resource management with try-with-resources, secure exception handling that prevents information leakage, proper exception chaining to preserve context, input validation with appropriate exceptions, thread interruption handling, and comprehensive exception documentation. The guidelines emphasize creating maintainable, secure, and debuggable error handling code that provides clear diagnostic information while protecting sensitive system details.

### Implementing These Principles

These guidelines are built upon the following core principles:

1. **Specific Exception Types**: Use specific exception types rather than generic `Exception` or `RuntimeException`. Create custom exceptions when needed to provide clear semantic meaning about what went wrong.
2. **Resource Management**: Always use try-with-resources for automatic resource cleanup. Ensure resources are properly closed even in exception scenarios.
3. **Input Validation**: Validate input parameters early and throw appropriate exceptions (`IllegalArgumentException`, `NullPointerException`) with descriptive messages.
4. **Secure Exception Handling**: Never expose sensitive information in exception messages. Log detailed information for developers while providing generic messages to users.
5. **Exception Chaining**: Preserve original exception context by using exception chaining to maintain the full error context for debugging.
6. **Thread Safety**: Handle `InterruptedException` properly by restoring the interrupted status and taking appropriate action.
7. **Documentation**: Document all checked exceptions with `@throws` tags and provide clear descriptions of when and why exceptions are thrown.
8. **Fail-Fast Principle**: Detect and report errors as early as possible rather than allowing invalid state to propagate through the system.
9. **Logging Policy**: Use structured logging with correlation IDs and error codes. Avoid log-and-throw duplication; log once at the boundary that decides the response.
10. **API Boundary Translation**: Translate lower-level exceptions into domain or transport-specific errors at application boundaries (e.g., controllers, message handlers) using centralized mappers/handlers.
11. **Retry and Idempotency**: Retry only idempotent operations with bounded attempts and backoff. Never retry on non-idempotent operations unless protected by idempotency keys.
12. **Timeouts and Deadlines**: Enforce timeouts and propagate deadline-exceeded errors. Cancel ongoing work on timeout and restore the interrupted status when applicable.
13. **Suppressed Exceptions**: When rethrowing after cleanup failures, attach secondary errors via `Throwable#addSuppressed` to preserve diagnostics.
14. **Do Not Catch Throwable/Error**: Catch specific exceptions. Do not catch `Throwable` or `Error` types; let fatal JVM errors propagate.
15. **Observability**: Emit metrics for error classes and latency; include correlation/request IDs in logs. Ensure error categorization supports SLO/SLA monitoring.
16. **Asynchronous/Reactive Propagation**: In async code (`CompletionStage`, callbacks), propagate failures without blocking; honor cancellation and interruption semantics.

## Constraints

Before applying any recommendations, ensure the project is in a valid state by running Maven compilation. Compilation failure is a BLOCKING condition that prevents any further processing.

- **MANDATORY**: Run `./mvnw compile` or `mvn compile` before applying any change
- **PREREQUISITE**: Project must compile successfully and pass basic validation checks before any optimization
- **CRITICAL SAFETY**: If compilation fails, IMMEDIATELY STOP and DO NOT CONTINUE with any recommendations
- **BLOCKING CONDITION**: Compilation errors must be resolved by the user before proceeding with any exception handling improvements
- **NO EXCEPTIONS**: Under no circumstances should design recommendations be applied to a project that fails to compile

## Examples

### Table of contents

- Example 1: Input Validation with Specific Exceptions
- Example 2: Resource Management with Try-With-Resources
- Example 3: Secure Exception Handling
- Example 4: Exception Chaining and Context Preservation
- Example 5: Thread Interruption and Concurrent Exception Handling
- Example 6: Custom Exception Design and Documentation
- Example 7: Use Exceptions Only for Exceptional Conditions
- Example 8: Use Checked Exceptions for Recoverable Conditions and Runtime Exceptions for Programming Errors
- Example 9: Favor the Use of Standard Exceptions
- Example 10: Include Failure-Capture Information in Detail Messages
- Example 11: Don't Ignore Exceptions
- Example 12: Testing Exception Scenarios Effectively
- Example 13: Logging Policy: Structured, De-duplicated Logging
- Example 14: Exception Translation at API Boundaries
- Example 15: Retry with Backoff Only for Idempotent Operations
- Example 16: Timeouts, Deadlines and Cancellation
- Example 17: Preserve Secondary Failures with Suppressed Exceptions
- Example 18: Do Not Catch Throwable or Error
- Example 19: Exception Handling in Lambda Expressions and Streams
- Example 20: Exception Handling in Constructors

### Example 1: Input Validation with Specific Exceptions

Title: Validate Early and Fail Fast with Descriptive Exceptions
Description: Validate input parameters at method entry points and throw specific, descriptive exceptions immediately when validation fails. This prevents invalid data from propagating through the system and makes debugging easier.

**Good example:**

```java
// GOOD: Specific input validation with descriptive exceptions
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class UserService {

    /**
     * Creates a new user with validated input.
     *
     * @param username the username, must not be null or empty
     * @param email the email address, must be valid format
     * @param age the user's age, must be between 13 and 120
     * @return the created user
     * @throws IllegalArgumentException if any parameter is invalid
     * @throws NullPointerException if username or email is null
     */
    public User createUser(String username, String email, int age) {
        // Null checks first
        Objects.requireNonNull(username, "Username cannot be null");
        Objects.requireNonNull(email, "Email cannot be null");

        // Specific validation with descriptive messages
        if (username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty or whitespace only");
        }

        if (username.length() < 3 || username.length() > 50) {
            throw new IllegalArgumentException("Username must be between 3 and 50 characters, was: " + username.length());
        }

        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }

        if (age < 13 || age > 120) {
            throw new IllegalArgumentException("Age must be between 13 and 120, was: " + age);
        }

        return new User(username.trim(), email.toLowerCase(), age);
    }

    /**
     * Processes a list of items, validates the list is not empty.
     *
     * @param items the list to process, must not be null or empty
     * @return processed results
     * @throws IllegalArgumentException if list is null or empty
     */
    public <T> List<T> processItems(List<T> items) {
        if (items == null) {
            throw new IllegalArgumentException("Items list cannot be null");
        }

        if (items.isEmpty()) {
            throw new IllegalArgumentException("Items list cannot be empty");
        }

        // Process items...
        return items.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private boolean isValidEmail(String email) {
        return email.contains("@") && email.contains(".");
    }
}
```

**Bad example:**

```java
// AVOID: Poor input validation and generic exceptions
public class BadUserService {

    // BAD: No validation, allows invalid state
    public User createUser(String username, String email, int age) {
        return new User(username, email, age); // Could create invalid user
    }

    // BAD: Generic exception without descriptive message
    public User createUserBad(String username, String email, int age) throws Exception {
        if (username == null || email == null) {
            throw new Exception("Invalid input"); // Too generic
        }
        return new User(username, email, age);
    }

    // BAD: Catching validation errors too late
    public User createUserWorse(String username, String email, int age) {
        try {
            // Process without validation
            User user = new User(username, email, age);
            user.save(); // Might fail with cryptic database error
            return user;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create user", e); // Lost context
        }
    }

    // BAD: No parameter validation
    public <T> List<T> processItems(List<T> items) {
        return items.stream() // NullPointerException if items is null
            .collect(Collectors.toList());
    }
}
```

### Example 2: Resource Management with Try-With-Resources

Title: Ensure Proper Resource Cleanup in All Scenarios
Description: Use try-with-resources for automatic resource management. This ensures resources are properly closed even when exceptions occur, preventing resource leaks and improving application reliability.

**Good example:**

```java
// GOOD: Proper resource management with try-with-resources
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceManager {
    private static final Logger logger = LoggerFactory.getLogger(ResourceManager.class);

    /**
     * Reads file content safely with automatic resource cleanup.
     *
     * @param filePath the path to read from
     * @return file content as string
     * @throws FileProcessingException if file cannot be read
     */
    public String readFileContent(Path filePath) throws FileProcessingException {
        Objects.requireNonNull(filePath, "File path cannot be null");

        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            return reader.lines()
                .collect(Collectors.joining(System.lineSeparator()));

        } catch (NoSuchFileException e) {
            logger.warn("File not found: {}", filePath);
            throw new FileProcessingException("File not found: " + filePath.getFileName(), e);

        } catch (AccessDeniedException e) {
            logger.error("Access denied reading file: {}", filePath);
            throw new FileProcessingException("Access denied to file", e);

        } catch (IOException e) {
            logger.error("IO error reading file: {}", filePath, e);
            throw new FileProcessingException("Failed to read file", e);
        }
    }

    /**
     * Executes database query with proper resource management.
     *
     * @param query the SQL query to execute
     * @param parameters query parameters
     * @return query results
     * @throws DatabaseException if query execution fails
     */
    public List<Map<String, Object>> executeQuery(String query, Object... parameters)
            throws DatabaseException {
        Objects.requireNonNull(query, "Query cannot be null");

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            // Set parameters safely
            for (int i = 0; i < parameters.length; i++) {
                statement.setObject(i + 1, parameters[i]);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                return extractResults(resultSet);
            }

        } catch (SQLException e) {
            logger.error("Database query failed: {}", query, e);
            throw new DatabaseException("Query execution failed", e);
        }
    }

    /**
     * Writes properties to file with proper resource management.
     *
     * @param properties the properties to write
     * @param filePath the target file path
     * @throws ConfigurationException if writing fails
     */
    public void writeProperties(Properties properties, Path filePath)
            throws ConfigurationException {
        Objects.requireNonNull(properties, "Properties cannot be null");
        Objects.requireNonNull(filePath, "File path cannot be null");

        try (OutputStream output = Files.newOutputStream(filePath);
             BufferedOutputStream buffered = new BufferedOutputStream(output)) {

            properties.store(buffered, "Generated configuration");

        } catch (IOException e) {
            logger.error("Failed to write properties to: {}", filePath, e);
            throw new ConfigurationException("Failed to write configuration", e);
        }
    }
}
```

**Bad example:**

```java
// AVOID: Poor resource management
public class BadResourceManager {

    // BAD: Manual resource management prone to leaks
    public String readFileContent(String filePath) throws Exception {
        FileInputStream fis = null;
        BufferedReader reader = null;

        try {
            fis = new FileInputStream(filePath);
            reader = new BufferedReader(new InputStreamReader(fis));

            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
            return content.toString();

        } catch (Exception e) {
            throw new Exception("File read failed", e);
        } finally {
            // BAD: Resource cleanup can fail silently
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // Swallowed exception - bad practice
                }
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    // Another swallowed exception
                }
            }
        }
    }

    // BAD: No resource management at all
    public void executeQuery(String query) throws Exception {
        Connection connection = getConnection();
        PreparedStatement statement = connection.prepareStatement(query);
        ResultSet resultSet = statement.executeQuery();

        // Process results...

        // TERRIBLE: Resources never closed - guaranteed leak
    }

    // BAD: Inconsistent resource management
    public void writeProperties(Properties props, String filePath) throws Exception {
        FileOutputStream fos = new FileOutputStream(filePath);
        props.store(fos, "Config");
        fos.close(); // Only closed on success path
    }
}
```
