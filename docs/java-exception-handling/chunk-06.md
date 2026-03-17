# java-exception-handling

> Source: 127-java-exception-handling.md
> Chunk: 6/6
> Included sections: Examples - Example 13: Logging Policy: Structured, De-duplicated Logging | Examples - Example 14: Exception Translation at API Boundaries | Examples - Example 15: Retry with Backoff Only for Idempotent Operations | Examples - Example 16: Timeouts, Deadlines and Cancellation | Examples - Example 17: Preserve Secondary Failures with Suppressed Exceptions | Examples - Example 18: Do Not Catch Throwable or Error | Examples - Example 19: Exception Handling in Lambda Expressions and Streams | Examples - Example 20: Exception Handling in Constructors | Output Format | Safeguards

### Example 13: Logging Policy: Structured, De-duplicated Logging

Title: Log once at boundaries with correlation IDs and error codes
Description: Centralize logging at application boundaries (e.g., controllers, message consumers, batch drivers). Lower layers should attach context and rethrow without logging to avoid duplicate log entries. Use structured logging fields like correlationId, errorCode, and userId.

**Good example:**

```java
// GOOD: Log once at the boundary with structured context
public class OrdersController {
    private static final Logger logger = LoggerFactory.getLogger(OrdersController.class);

    public Response createOrder(CreateOrderRequest request, String correlationId) {
        try {
            Order order = orderService.create(request, correlationId);
            return Response.created(order.getId());
        } catch (OrderValidationException e) {
            logger.info("order.create.validation_failed correlationId={} errorCode={}", correlationId, e.getErrorCode(), e);
            return Response.badRequest(problemFrom(e, correlationId));
        } catch (OrderSystemException e) {
            logger.error("order.create.system_error correlationId={} errorCode={}", correlationId, e.getErrorCode(), e);
            return Response.serverError(problemFrom(e, correlationId));
        }
    }
}

// Service layer: enrich and rethrow without logging
public class OrderService {
    public Order create(CreateOrderRequest request, String correlationId) throws OrderValidationException, OrderSystemException {
        try {
            validate(request);
            return repository.save(toOrder(request));
        } catch (DataIntegrityViolationException e) {
            throw new OrderValidationException("Duplicate order", ErrorCode.DUPLICATE, e).withCorrelationId(correlationId);
        } catch (SQLException e) {
            throw new OrderSystemException("Database failure", ErrorCode.SYSTEM_ERROR, e).withCorrelationId(correlationId);
        }
    }
}
```

**Bad example:**

```java
// AVOID: Duplicate logging across layers (log-and-throw)
public class NoisyService {
    private static final Logger logger = LoggerFactory.getLogger(NoisyService.class);

    public void doWork() {
        try {
            repository.save(...);
        } catch (Exception e) {
            logger.error("save failed", e); // First log
            throw new ServiceException("save failed", e);
        }
    }
}

public class NoisyController {
    private static final Logger logger = LoggerFactory.getLogger(NoisyController.class);
    public Response handle() {
        try {
            service.doWork();
            return Response.ok();
        } catch (ServiceException e) {
            logger.error("save failed", e); // Second log - duplicates
            return Response.serverError();
        }
    }
}
```

### Example 14: Exception Translation at API Boundaries

Title: Use centralized handlers to map domain errors to responses
Description: Translate internal exceptions into transport-appropriate error responses using centralized handlers. Keep domain exceptions expressive and map them to HTTP/problem-details (or messaging error contracts) at the boundary.

**Good example:**

```java
// GOOD: Centralized translation to ProblemDetails
public final class ProblemDetails {
    public final String type; // URI identifying error type
    public final String title;
    public final int status;
    public final String detail;
    public final String correlationId;
    public ProblemDetails(String type, String title, int status, String detail, String correlationId) {
        this.type = type; this.title = title; this.status = status; this.detail = detail; this.correlationId = correlationId;
    }
}

public class ApiErrorHandler {
    public Response toResponse(Throwable t, String correlationId) {
        if (t instanceof UserNotFoundException e) {
            return Response.status(404).body(new ProblemDetails(
                "urn:problem:user-not-found", "User not found", 404, e.getMessage(), correlationId));
        }
        if (t instanceof UserValidationException e) {
            return Response.status(400).body(new ProblemDetails(
                "urn:problem:validation", "Validation failed", 400, e.getMessage(), correlationId));
        }
        return Response.status(500).body(new ProblemDetails(
            "urn:problem:internal", "Internal Server Error", 500, "Unexpected error", correlationId));
    }
}
```

**Bad example:**

```java
// AVOID: Ad-hoc translation scattered across controllers
public class UsersController {
    public Response get(String id) {
        try {
            return Response.ok(service.get(id));
        } catch (Exception e) { // Too generic and repeated everywhere
            return Response.status(500).body(e.getMessage()); // Leaks internals
        }
    }
}
```

### Example 15: Retry with Backoff Only for Idempotent Operations

Title: Bounded retries, interruption-aware, transient-only
Description: Apply retries only to operations that are safe to repeat. Use bounded attempts, backoff, and transient error filtering. Preserve interruption and stop on non-transient errors.

**Good example:**

```java
// GOOD: Interruption-aware retry with backoff for idempotent fetch
public class RetryingClient {
    private static final Logger logger = LoggerFactory.getLogger(RetryingClient.class);

    public Response fetch(String resourceId) throws ClientException, InterruptedException {
        int maxAttempts = 3;
        long backoffMs = 200;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return httpClient.get(resourceId); // Safe, idempotent
            } catch (IOException e) {
                if (attempt == maxAttempts || !isTransient(e)) {
                    throw new ClientException("Fetch failed after " + attempt + " attempts", e);
                }
                logger.warn("Transient failure (attempt {}), backing off {}ms", attempt, backoffMs);
                try { Thread.sleep(backoffMs); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); throw ie; }
                backoffMs *= 2;
            }
        }
        throw new ClientException("Unreachable code");
    }

    private boolean isTransient(IOException e) { return e instanceof java.net.SocketTimeoutException; }
}
```

**Bad example:**

```java
// AVOID: Blind retries on non-idempotent operations, swallowing interrupts
public class RiskyClient {
    public String createOrder(String body) {
        for (int i = 0; i < 10; i++) {
            try {
                return http.post("/orders", body); // Not idempotent
            } catch (Exception e) {
                // Swallow interruption and keep retrying
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            }
        }
        throw new RuntimeException("Failed");
    }
}
```

### Example 16: Timeouts, Deadlines and Cancellation

Title: Propagate deadline exceeded and cancel ongoing work
Description: Enforce time limits for blocking calls and asynchronous operations. On timeout, cancel the work, restore interrupted status if applicable, and translate to a domain-specific timeout error.

**Good example:**

```java
// GOOD: Enforce deadline with cancellation
public class DeadlineService {
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public Result computeWithDeadline(Callable<Result> task, Duration timeout) throws ProcessingException, InterruptedException {
        Future<Result> future = executor.submit(task);
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true); // interrupt if running
            throw new ProcessingException("Deadline exceeded", e);
        } catch (ExecutionException e) {
            throw new ProcessingException("Task failed", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        }
    }
}
```

**Bad example:**

```java
// AVOID: No timeout, no cancellation, blocks indefinitely
public class SlowService {
    public Result compute() {
        try {
            return longBlockingCall(); // Could hang forever
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```

### Example 17: Preserve Secondary Failures with Suppressed Exceptions

Title: Use addSuppressed when cleanup also fails
Description: When manual cleanup fails after a primary exception, attach the cleanup exception using `Throwable#addSuppressed` before rethrowing to retain both diagnostics. Note: try-with-resources does this automatically.

**Good example:**

```java
// GOOD: Keep both primary and cleanup failures
public void copy(Path src, Path dst) throws FileOperationException {
    FileChannel in = null, out = null;
    try {
        in = FileChannel.open(src, StandardOpenOption.READ);
        out = FileChannel.open(dst, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
        in.transferTo(0, in.size(), out);
    } catch (IOException primary) {
        IOException cleanupError = null;
        try { if (in != null) in.close(); } catch (IOException e) { cleanupError = e; }
        try { if (out != null) out.close(); } catch (IOException e) { if (cleanupError != null) primary.addSuppressed(cleanupError); cleanupError = e; }
        if (cleanupError != null) primary.addSuppressed(cleanupError);
        throw new FileOperationException("Copy failed", primary);
    }
}
```

**Bad example:**

```java
// AVOID: Losing cleanup failure
public void write(Path dst) throws Exception {
    OutputStream out = null;
    try {
        out = Files.newOutputStream(dst);
        out.write(new byte[]{1});
    } catch (IOException e) {
        try { if (out != null) out.close(); } catch (IOException ignored) {}
        throw e; // Cleanup error lost
    }
}
```

### Example 18: Do Not Catch Throwable or Error

Title: Catch specific exceptions; let fatal errors propagate
Description: Catching `Throwable` or `Error` can mask fatal conditions (e.g., `OutOfMemoryError`). Restrict catch blocks to expected, recoverable exceptions.

**Good example:**

```java
// GOOD: Catch only expected exceptions
try {
    persist(entity);
} catch (SQLException e) {
    throw new RepositoryException("Persist failed", e);
}
```

**Bad example:**

```java
// AVOID: Catching Throwable masks fatal errors
try {
    run();
} catch (Throwable t) { // Too broad
    log.error("failure", t);
}
```


### Example 19: Exception Handling in Lambda Expressions and Streams

Title: Properly manage exceptions in functional constructs
Description: In functional programming with lambdas and streams, wrap throwing operations in try-catch or use checked-exception wrappers to maintain proper error handling without losing functional style.

**Good example:**

```java
// GOOD: Proper exception handling in streams
import java.util.List;
import java.util.stream.Collectors;

public class LambdaProcessor {
    public List<Result> process(List<Input> inputs) {
        return inputs.stream()
            .map(input -> {
                try {
                    return processSingle(input);
                } catch (ProcessingException e) {
                    logger.warn("Failed to process input: {}", input, e);
                    return Result.failure(input, e.getMessage());
                }
            })
            .collect(Collectors.toList());
    }

    private Result processSingle(Input input) throws ProcessingException {
        // Processing that may throw
        if (input == null) {
            throw new ProcessingException("Null input");
        }
        return Result.success(input);
    }
}
```

**Bad example:**

```java
// AVOID: Unhandled exceptions in lambdas
public class BadLambdaProcessor {
    public List<Result> process(List<Input> inputs) {
        return inputs.stream()
            .map(this::processSingle) // Throws unchecked, breaks stream
            .collect(Collectors.toList());
    }

    private Result processSingle(Input input) {
        if (input == null) {
            throw new RuntimeException("Null input"); // Bad: unchecked in lambda
        }
        return Result.success(input);
    }
}
```

### Example 20: Exception Handling in Constructors

Title: Properly handle initialization failures
Description: In constructors, validate parameters and throw exceptions for invalid states to prevent creation of invalid objects. Use factory methods for complex initialization that may fail.

**Good example:**

```java
// GOOD: Validation in constructor with factory method
public class User {
    private final String name;
    private final int age;

    private User(String name, int age) {
        Objects.requireNonNull(name, "Name cannot be null");
        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        if (age < 0) {
            throw new IllegalArgumentException("Age cannot be negative");
        }
        this.name = name.trim();
        this.age = age;
    }

    public static User create(String name, int age) {
        return new User(name, age);
    }
}
```

**Bad example:**

```java
// AVOID: No validation in constructor, creates invalid objects
public class BadUser {
    private String name;
    private int age;

    public BadUser(String name, int age) {
        this.name = name; // Could be null or empty
        this.age = age;   // Could be negative
    }
}
```

## Output Format

- **ANALYZE** Java code to identify specific exception handling issues and categorize them by severity (CRITICAL, HIGH, MEDIUM, LOW) and exception handling area (validation, resource management, security, thread safety, documentation, exception design)
- **CATEGORIZE** exception handling improvements found: Input Validation Issues (missing validation vs comprehensive parameter checking, inadequate sanitization vs proper input validation), Resource Management Problems (missing try-with-resources vs automatic resource management, resource leaks vs proper cleanup), Security Vulnerabilities (information disclosure vs secure error messages, sensitive data exposure vs protected exception handling), Thread Safety Problems (improper InterruptedException handling vs correct interruption patterns, concurrent access issues vs thread-safe exception handling), Exception Design Flaws (generic exceptions vs specific exception types, poor hierarchy design vs well-structured exception hierarchies), and Documentation Deficiencies (missing @throws tags vs comprehensive documentation, unclear exception descriptions vs detailed error information)
- **APPLY** exception handling best practices directly by implementing the most appropriate improvements for each identified issue: Implement comprehensive input validation with early parameter checking and proper sanitization, establish automatic resource management using try-with-resources patterns, secure exception handling through generic error messages and protected logging, implement proper thread safety with correct InterruptedException handling, design specific exception hierarchies with meaningful exception types, and provide comprehensive exception documentation with detailed @throws annotations
- **IMPLEMENT** comprehensive exception handling refactoring using proven patterns: Establish robust input validation with null checks, range validation, and format validation, implement automatic resource management with try-with-resources for all closeable resources, apply security-focused exception handling with sanitized error messages and secure logging, design proper exception hierarchies with domain-specific exceptions, implement thread-safe exception handling with proper interruption support, and integrate comprehensive exception documentation with detailed error descriptions and recovery guidance
- **REFACTOR** code systematically following the exception handling improvement roadmap: First implement critical input validation to prevent invalid state and security vulnerabilities, then establish automatic resource management to prevent resource leaks, apply security measures to protect sensitive information in exceptions, implement proper thread safety in exception handling, design and apply well-structured exception hierarchies, and complete comprehensive exception documentation for maintainability and debugging support
- **EXPLAIN** the applied exception handling improvements and their benefits: Reliability enhancements through comprehensive input validation and resource management, security improvements via protected error messages and secure exception handling, maintainability gains from well-structured exception hierarchies and comprehensive documentation, thread safety improvements through proper interruption handling, and debugging capabilities enhancement through detailed exception information and recovery guidance
- **VALIDATE** that all applied exception handling refactoring compiles successfully, maintains existing functionality, improves error handling robustness, eliminates security vulnerabilities, and achieves the intended reliability and maintainability improvements through comprehensive testing and verification

## Safeguards

- **BLOCKING SAFETY CHECK**: ALWAYS run `./mvnw compile` before ANY exception handling recommendations to ensure project stability
- **CRITICAL VALIDATION**: Execute `./mvnw clean verify` to ensure all tests pass after each exception handling improvement
- **MANDATORY VERIFICATION**: Confirm all existing functionality remains intact through comprehensive test execution
- **ROLLBACK REQUIREMENT**: Ensure all exception handling changes can be easily reverted using version control checkpoints
- **INCREMENTAL SAFETY**: Apply exception handling improvements incrementally, validating after each modification to isolate potential issues
- **SECURITY VALIDATION**: Verify that exception handling improvements don't introduce information disclosure vulnerabilities or expose sensitive system details
- **THREAD SAFETY VERIFICATION**: Ensure that concurrent exception handling changes don't introduce race conditions or deadlocks
- **BACKWARD COMPATIBILITY**: Maintain API compatibility when changing exception types or adding new exception handling patterns
