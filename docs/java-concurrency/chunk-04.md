# java-concurrency

> Source: 125-java-concurrency.md
> Chunk: 4/5
> Included sections: Examples - Example 9: Simplify Concurrent Code with Structured Concurrency | Examples - Example 10: Manage Thread-Shared Data with Scoped Values

### Example 9: Simplify Concurrent Code with Structured Concurrency

Title: Manage Related Tasks as a Single Unit
Description: Use `StructuredTaskScope` to simplify the management of multiple related concurrent tasks as a single unit of work, improving error handling, cancellation, and resource management. - Use the static `StructuredTaskScope.open()` factory method instead of constructors. - Use `StructuredTaskScope.ShutdownOnFailure()` for fail-fast behavior. - Use `StructuredTaskScope.ShutdownOnSuccess()` for racing tasks. - Implement `Joiner.onTimeout()` method for custom timeout handling. - Ensure proper resource cleanup with try-with-resources. - Handle task results and exceptions appropriately. - Combine structured concurrency with virtual threads for optimal performance.

**Good example:**

```java
// GOOD: Structured concurrency for managing related tasks
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.Future;
import java.util.List;

class StructuredConcurrencyExample {

    // Fetch user data from multiple sources
    public UserData fetchCompleteUserData(String userId) throws Exception {
        // Use static factory method instead of constructor
        try (var scope = StructuredTaskScope.open()) {

            // Fork multiple related tasks
            Future<String> profileFuture = scope.fork(() -> fetchUserProfile(userId));
            Future<String> settingsFuture = scope.fork(() -> fetchUserSettings(userId));
            Future<String> ordersFuture = scope.fork(() -> fetchUserOrders(userId));

            // Wait for all tasks to complete or fail
            scope.join();
            scope.throwIfFailed();

            // All tasks completed successfully
            return new UserData(
                profileFuture.resultNow(),
                settingsFuture.resultNow(),
                ordersFuture.resultNow()
            );
        }
    }

    // Race multiple service calls and return first success
    public String fetchDataFromAnySource(String query) throws Exception {
        // Use static factory method for ShutdownOnSuccess
        try (var scope = StructuredTaskScope.open()) {

            // Fork competing tasks
            scope.fork(() -> fetchFromPrimaryService(query));
            scope.fork(() -> fetchFromSecondaryService(query));
            scope.fork(() -> fetchFromCacheService(query));

            // Wait for first successful completion
            scope.join();
            return scope.result();
        }
    }

    // Batch processing with structured concurrency
    public List<String> processBatch(List<String> items) throws Exception {
        // Use static factory method instead of constructor
        try (var scope = StructuredTaskScope.open()) {

            List<Future<String>> futures = items.stream()
                .map(item -> scope.fork(() -> processItem(item)))
                .toList();

            scope.join();
            scope.throwIfFailed();

            return futures.stream()
                .map(Future::resultNow)
                .toList();
        }
    }

    private String fetchUserProfile(String userId) {
        // Simulate I/O operation
        try { Thread.sleep(100); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "Profile for " + userId;
    }

    private String fetchUserSettings(String userId) {
        try { Thread.sleep(150); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "Settings for " + userId;
    }

    private String fetchUserOrders(String userId) {
        try { Thread.sleep(200); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "Orders for " + userId;
    }

    private String fetchFromPrimaryService(String query) {
        try { Thread.sleep(300); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "Primary result for: " + query;
    }

    private String fetchFromSecondaryService(String query) {
        try { Thread.sleep(200); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "Secondary result for: " + query;
    }

    private String fetchFromCacheService(String query) {
        try { Thread.sleep(50); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "Cached result for: " + query;
    }

    private String processItem(String item) {
        try { Thread.sleep(100); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "Processed: " + item;
    }

    // Custom Joiner with timeout handling
    public String fetchWithTimeoutHandling(String query) throws Exception {
        try (var scope = StructuredTaskScope.open(new CustomTimeoutJoiner<String>())) {

            scope.fork(() -> {
                try { Thread.sleep(5000); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
                return "Slow result for: " + query;
            });

            scope.fork(() -> {
                try { Thread.sleep(2000); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
                return "Fast result for: " + query;
            });

            scope.join();
            return scope.result();
        }
    }

    // Custom Joiner implementation with timeout handling
    static class CustomTimeoutJoiner<T> implements StructuredTaskScope.Joiner<T, T> {
        private volatile T result;

        @Override
        public boolean onFork(StructuredTaskScope.Subtask<? extends T> subtask) {
            return true; // Continue with all subtasks
        }

        @Override
        public T onJoin() {
            return result;
        }

        @Override
        public T onTimeout() {
            // Provide default result when timeout occurs
            return (T) "Default result due to timeout";
        }

        @Override
        public boolean needsCompletion() {
            return result == null;
        }
    }

    record UserData(String profile, String settings, String orders) {}
}
```

**Bad example:**

```java
// AVOID: Manual task management without structured concurrency
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class BadStructuredConcurrency {

    // BAD: Manual task management with resource leaks
    public UserData fetchUserDataManually(String userId) {
        ExecutorService executor = Executors.newFixedThreadPool(3);

        try {
            Future<String> profileFuture = executor.submit(() -> fetchUserProfile(userId));
            Future<String> settingsFuture = executor.submit(() -> fetchUserSettings(userId));
            Future<String> ordersFuture = executor.submit(() -> fetchUserOrders(userId));

            // BAD: No proper error handling
            return new UserData(
                profileFuture.get(),
                settingsFuture.get(),
                ordersFuture.get()
            );
        } catch (Exception e) {
            // BAD: Poor error handling
            throw new RuntimeException(e);
        } finally {
            // BAD: Improper shutdown
            executor.shutdown();
        }
    }

    // BAD: No cancellation when one task fails
    public String fetchFromAnySourceBadly(String query) {
        ExecutorService executor = Executors.newFixedThreadPool(3);

        try {
            Future<String> primary = executor.submit(() -> fetchFromPrimaryService(query));
            Future<String> secondary = executor.submit(() -> fetchFromSecondaryService(query));
            Future<String> cache = executor.submit(() -> fetchFromCacheService(query));

            // BAD: All tasks continue even if one succeeds
            while (true) {
                if (primary.isDone() && !primary.isCancelled()) {
                    return primary.get();
                }
                if (secondary.isDone() && !secondary.isCancelled()) {
                    return secondary.get();
                }
                if (cache.isDone() && !cache.isCancelled()) {
                    return cache.get();
                }
                Thread.sleep(10);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            executor.shutdown();
        }
    }

    record UserData(String profile, String settings, String orders) {}
}
```

### Example 10: Manage Thread-Shared Data with Scoped Values

Title: Use Scoped Values for Thread-Safe Context Propagation
Description: Prefer `ScopedValue` over `ThreadLocal` for sharing immutable data robustly and efficiently across tasks within a dynamically bounded scope, especially when working with virtual threads. Scoped Values became stable in Java 25 (JEP 506). - Use `ScopedValue.newInstance()` to create scoped value instances. - Use `ScopedValue.where()` to establish scoped bindings with automatic cleanup. - Combine multiple scoped values using method chaining for complex contexts. - Prefer scoped values for immutable context data that needs to flow through call chains. - Avoid `ThreadLocal` with virtual threads due to potential memory issues and lack of structured cleanup. - Use scoped values for request-scoped data in web applications and distributed tracing. - Leverage automatic inheritance in structured concurrency and virtual threads.

**Good example:**

```java
// GOOD: Scoped values for thread-safe context propagation
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.Future;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class ScopedValueExample {

    // Define scoped values for different types of context
    private static final ScopedValue<String> USER_ID = ScopedValue.newInstance();
    private static final ScopedValue<String> REQUEST_ID = ScopedValue.newInstance();
    private static final ScopedValue<Map<String, String>> SECURITY_CONTEXT = ScopedValue.newInstance();

    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    // Web request processing with scoped context
    public String handleWebRequest(String userId, String requestId, Map<String, String> securityContext) {
        return ScopedValue.where(USER_ID, userId)
            .where(REQUEST_ID, requestId)
            .where(SECURITY_CONTEXT, securityContext)
            .call(() -> processRequestWithContext());
    }

    private String processRequestWithContext() {
        String userId = USER_ID.get();
        String requestId = REQUEST_ID.get();

        System.out.println("Processing request " + requestId + " for user " + userId);

        // Process in parallel while maintaining context
        return ScopedValue.where(USER_ID, userId)
            .where(REQUEST_ID, requestId)
            .where(SECURITY_CONTEXT, SECURITY_CONTEXT.get())
            .call(() -> {
                try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {

                    // All forked tasks inherit the scoped values
                    Future<String> businessLogic = scope.fork(this::executeBusinessLogic);
                    Future<String> auditLog = scope.fork(this::createAuditLog);
                    Future<String> notification = scope.fork(this::sendNotification);

                    scope.join();
                    scope.throwIfFailed();

                    return combineResults(
                        businessLogic.resultNow(),
                        auditLog.resultNow(),
                        notification.resultNow()
                    );

                } catch (Exception e) {
                    throw new RuntimeException("Request processing failed", e);
                }
            });
    }

    private String executeBusinessLogic() {
        String userId = USER_ID.get();
        String requestId = REQUEST_ID.get();

        // Simulate business logic
        try { Thread.sleep(100); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return "Business logic completed for user " + userId + " (request: " + requestId + ")";
    }

    private String createAuditLog() {
        String userId = USER_ID.get();
        String requestId = REQUEST_ID.get();
        Map<String, String> securityContext = SECURITY_CONTEXT.get();

        String securityInfo = securityContext.getOrDefault("role", "unknown");
        return "Audit logged for user " + userId + " with role " + securityInfo + " (request: " + requestId + ")";
    }

    private String sendNotification() {
        String userId = USER_ID.get();
        String requestId = REQUEST_ID.get();

        return "Notification sent to user " + userId + " (request: " + requestId + ")";
    }

    // Nested scoped value usage
    public void processWithNestedScope(String userId, List<String> tasks) {
        ScopedValue.where(USER_ID, userId)
            .run(() -> {
                for (String task : tasks) {
                    ScopedValue.where(REQUEST_ID, "task-" + task)
                        .run(() -> processTask(task));
                }
            });
    }

    private void processTask(String task) {
        String userId = USER_ID.get();
        String requestId = REQUEST_ID.get();

        System.out.println("Processing task " + task + " for user " + userId + " (request: " + requestId + ")");
    }

    private String combineResults(String businessResult, String auditResult, String notificationResult) {
        return String.format("Results: [%s] [%s] [%s]", businessResult, auditResult, notificationResult);
    }

    public void shutdown() {
        virtualExecutor.shutdown();
    }
}
```

**Bad example:**

```java
// AVOID: ThreadLocal with virtual threads
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Map;
import java.util.HashMap;

class BadScopedValueUsage {

    // BAD: Using ThreadLocal with virtual threads
    private static final ThreadLocal<String> USER_CONTEXT = new ThreadLocal<>();

    public void processWithThreadLocal(String userId) {
        USER_CONTEXT.set(userId);

        ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

        virtualExecutor.submit(() -> {
            // BAD: ThreadLocal values don't propagate to virtual threads properly
            String currentUserId = USER_CONTEXT.get(); // Likely null
            System.out.println("Processing for user: " + currentUserId);
        });

        // BAD: Not cleaning up ThreadLocal
        // USER_CONTEXT.remove(); // Missing cleanup
    }

    // BAD: Manual context propagation
    public void processWithManualPropagation(String userId, String requestId) {
        ExecutorService executor = Executors.newFixedThreadPool(10);

        // BAD: Manually passing context to each task
        executor.submit(() -> {
            processTask(userId, requestId, "task1");
        });

        executor.submit(() -> {
            processTask(userId, requestId, "task2");
        });

        // BAD: Error-prone and verbose
    }

    private void processTask(String userId, String requestId, String task) {
        System.out.println("Processing " + task + " for user " + userId + " (request: " + requestId + ")");
    }

    // BAD: ThreadLocal memory leak
    private static final ThreadLocal<Map<String, String>> CONTEXT =
        ThreadLocal.withInitial(HashMap::new);

    public void leakyContextManagement() {
        CONTEXT.get().put("userId", "123");
        CONTEXT.get().put("requestId", "456");

        // Process something
        processData();

        // BAD: Forgetting to clean up can cause memory leaks
        // CONTEXT.remove(); // Missing cleanup
    }

    private void processData() {
        System.out.println("Processing data...");
    }
}
```
