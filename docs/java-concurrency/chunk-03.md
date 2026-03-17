# java-concurrency

> Source: 125-java-concurrency.md
> Chunk: 3/5
> Included sections: Examples - Example 6: Performance Considerations in Concurrency | Examples - Example 7: Testing and Debugging Concurrent Code | Examples - Example 8: Embrace Virtual Threads for Enhanced Scalability

### Example 6: Performance Considerations in Concurrency

Title: Optimize Concurrent Code for Performance
Description: Be mindful of performance implications in concurrent applications. - **Lock Contention**: Reduce contention by narrowing lock scopes, using finer-grained locks (lock striping), or exploring optimistic locking or lock-free data structures. - **Memory Consistency**: Understand the Java Memory Model (JMM). Use `volatile` for visibility of single variables across threads. Be aware of happens-before relationships established by synchronization. - **Context Switching**: Excessive threads can lead to high context-switching overhead. Size thread pools appropriately. - **False Sharing**: Be aware of false sharing when mutable fields accessed by different threads reside on the same cache line.

**Good example:**

```java
// GOOD: Performance-optimized concurrent code
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

// Lock striping for better performance
class StripedMap<K, V> {
    private static final int STRIPE_COUNT = 16;
    private final List<Lock> stripes;
    private final List<Map<K, V>> buckets;

    public StripedMap() {
        stripes = new ArrayList<>(STRIPE_COUNT);
        buckets = new ArrayList<>(STRIPE_COUNT);
        for (int i = 0; i < STRIPE_COUNT; i++) {
            stripes.add(new ReentrantLock());
            buckets.add(new HashMap<>());
        }
    }

    private int getStripeIndex(K key) {
        return Math.abs(key.hashCode() % STRIPE_COUNT);
    }

    public V get(K key) {
        int index = getStripeIndex(key);
        Lock lock = stripes.get(index);
        lock.lock();
        try {
            return buckets.get(index).get(key);
        } finally {
            lock.unlock();
        }
    }

    public V put(K key, V value) {
        int index = getStripeIndex(key);
        Lock lock = stripes.get(index);
        lock.lock();
        try {
            return buckets.get(index).put(key, value);
        } finally {
            lock.unlock();
        }
    }
}

// Memory consistency with volatile
class MemoryConsistencyExample {
    private volatile boolean flag = false;
    private int data = 0;

    public void writer() {
        data = 42;
        flag = true; // Volatile write establishes happens-before
    }

    public void reader() {
        while (!flag) {
            // Spin wait
        }
        // Guaranteed to see data = 42
        System.out.println("Data: " + data);
    }
}
```

**Bad example:**

```java
// AVOID: Performance-harming concurrent code
import java.util.HashMap;
import java.util.Map;

public class PoorPerformance {
    private final Map<String, String> map = new HashMap<>();

    // BAD: Coarse-grained locking
    public synchronized String get(String key) {
        return map.get(key);
    }

    public synchronized void put(String key, String value) {
        map.put(key, value);
    }

    // BAD: Non-volatile field without synchronization
    private boolean flag = false;
    private int data = 0;

    public void writer() {
        data = 42;
        flag = true; // Other threads might not see this change
    }

    public void reader() {
        while (!flag) {
            // Might loop forever
        }
        // Might see stale data
        System.out.println("Data: " + data);
    }
}
```

### Example 7: Testing and Debugging Concurrent Code

Title: Rigorously Test and Debug Concurrent Applications
Description: Thoroughly test concurrent code using appropriate tools and techniques to identify race conditions, deadlocks, and other concurrency issues. - Write unit tests for thread-safe components with proper synchronization testing. - Use stress tests to reveal race conditions and deadlocks under load. - Implement integration tests for concurrent interactions between components. - Utilize thread dump analysis and profiling tools for debugging. - Apply proper logging strategies for concurrent environments. - Use testing frameworks like JUnit 5 with parallel execution capabilities.

**Good example:**

```java
// GOOD: Proper testing of concurrent code
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConcurrentTestExample {

    @Test
    @RepeatedTest(100) // Run multiple times to catch race conditions
    void testThreadSafeCounter() throws InterruptedException {
        ThreadSafeCounter counter = new ThreadSafeCounter();
        int numberOfThreads = 10;
        int incrementsPerThread = 1000;

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < incrementsPerThread; j++) {
                        counter.increment();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(numberOfThreads * incrementsPerThread, counter.getValue());
    }

    @Test
    void testProducerConsumer() throws InterruptedException {
        BlockingQueue<String> queue = new LinkedBlockingQueue<>(10);
        List<String> consumed = Collections.synchronizedList(new ArrayList<>());

        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch latch = new CountDownLatch(2);

        // Producer
        executor.submit(() -> {
            try {
                for (int i = 0; i < 5; i++) {
                    queue.put("item-" + i);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        });

        // Consumer
        executor.submit(() -> {
            try {
                for (int i = 0; i < 5; i++) {
                    String item = queue.take();
                    consumed.add(item);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        executor.shutdown();
        assertEquals(5, consumed.size());
    }

    private static class ThreadSafeCounter {
        private final AtomicInteger count = new AtomicInteger(0);

        public void increment() {
            count.incrementAndGet();
        }

        public int getValue() {
            return count.get();
        }
    }
}
```

**Bad example:**

```java
// AVOID: Inadequate testing of concurrent code
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PoorConcurrentTesting {

    @Test
    void testCounter() {
        // BAD: Only testing single-threaded scenario
        UnsafeCounter counter = new UnsafeCounter();
        counter.increment();
        assertEquals(1, counter.getValue());

        // This test passes but doesn't verify thread safety
    }

    @Test
    void testConcurrentAccess() throws InterruptedException {
        UnsafeCounter counter = new UnsafeCounter();

        // BAD: Creating threads but not waiting for completion
        new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                counter.increment();
            }
        }).start();

        new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                counter.increment();
            }
        }).start();

        // BAD: Not waiting for threads to complete
        Thread.sleep(100); // Unreliable timing

        // This assertion might pass or fail randomly
        // assertEquals(2000, counter.getValue());
    }

    private static class UnsafeCounter {
        private int count = 0;

        public void increment() {
            count++; // Not thread-safe
        }

        public int getValue() {
            return count;
        }
    }
}
```

### Example 8: Embrace Virtual Threads for Enhanced Scalability

Title: Use Virtual Threads for I/O-bound Tasks
Description: Leverage virtual threads (Project Loom) for I/O-bound tasks to dramatically increase scalability with minimal resource overhead. Avoid pooling virtual threads and use structured concurrency where appropriate.

**Good example:**

```java
// GOOD: Using virtual threads for scalable I/O operations
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScopedValue;
import java.util.concurrent.StructuredTaskScope;
import java.util.stream.IntStream;

public class VirtualThreadExample {

    // Use virtual thread executor for I/O-bound tasks
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public void handleManyIORequests() {
        List<Future<String>> futures = IntStream.range(0, 10000)
            .mapToObj(i -> virtualExecutor.submit(() -> performIOOperation("task-" + i)))
            .toList();

        // Collect results
        futures.forEach(future -> {
            try {
                String result = future.get();
                System.out.println("Completed: " + result);
            } catch (Exception e) {
                System.err.println("Task failed: " + e.getMessage());
            }
        });
    }

    // Virtual threads are perfect for blocking I/O operations
    private String performIOOperation(String taskId) {
        try {
            // Simulate I/O operation (database call, web request, etc.)
            Thread.sleep(1000); // This would block a platform thread
            return "Result for " + taskId;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Interrupted: " + taskId;
        }
    }

    // Using scoped values with virtual threads (Java 20+)
    private static final ScopedValue<String> USER_ID = ScopedValue.newInstance();

    public void processWithScopedValue(String userId, List<String> tasks) {
        // Run with scoped value
        ScopedValue.where(USER_ID, userId)
            .run(() -> {
                tasks.parallelStream().forEach(task -> {
                    virtualExecutor.submit(() -> {
                        // Access scoped value safely
                        String currentUserId = USER_ID.get();
                        performTaskForUser(currentUserId, task);
                    });
                });
            });
    }

    private void performTaskForUser(String userId, String task) {
        System.out.println("Processing task " + task + " for user " + userId +
                          " on thread " + Thread.currentThread());
    }

    // Structured concurrency for managing related tasks
    public String fetchUserDataWithStructuredConcurrency(String userId) throws Exception {
        // Use static factory method instead of constructor
        try (var scope = StructuredTaskScope.open()) {

            Future<String> profile = scope.fork(() -> fetchUserProfile(userId));
            Future<String> preferences = scope.fork(() -> fetchUserPreferences(userId));
            Future<String> history = scope.fork(() -> fetchUserHistory(userId));

            scope.join();           // Wait for all tasks
            scope.throwIfFailed();  // Propagate any failures

            // All tasks completed successfully
            return combineUserData(profile.resultNow(),
                                 preferences.resultNow(),
                                 history.resultNow());
        }
    }

    private String fetchUserProfile(String userId) {
        // Simulate I/O operation
        try { Thread.sleep(100); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "Profile for " + userId;
    }

    private String fetchUserPreferences(String userId) {
        try { Thread.sleep(150); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "Preferences for " + userId;
    }

    private String fetchUserHistory(String userId) {
        try { Thread.sleep(200); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "History for " + userId;
    }

    private String combineUserData(String profile, String preferences, String history) {
        return String.format("User data: %s, %s, %s", profile, preferences, history);
    }

    public void shutdown() {
        virtualExecutor.shutdown();
    }
}
```

**Bad example:**

```java
// AVOID: Misusing virtual threads
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocal;

public class BadVirtualThreadUsage {

    // BAD: Creating thread pools for virtual threads
    private final ExecutorService virtualPool = Executors.newFixedThreadPool(100,
        Thread.ofVirtual().factory()); // Don't pool virtual threads!

    // BAD: Using virtual threads for CPU-intensive tasks
    public void performCPUIntensiveWork() {
        ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

        for (int i = 0; i < 1000; i++) {
            virtualExecutor.submit(() -> {
                // BAD: Virtual threads are not suitable for CPU-bound work
                double result = 0;
                for (int j = 0; j < 1_000_000; j++) {
                    result += Math.sqrt(j) * Math.sin(j);
                }
                return result;
            });
        }
    }

    // BAD: Using platform thread patterns with virtual threads
    public void badResourceManagement() {
        // Creating virtual threads manually instead of using executor
        for (int i = 0; i < 10000; i++) {
            Thread.ofVirtual().start(() -> {
                performIOOperation();
                // BAD: No proper cleanup or error handling
            });
        }
    }

    // BAD: Blocking operations that shouldn't be used with virtual threads
    public void problematicBlocking() {
        ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

        virtualExecutor.submit(() -> {
            try {
                synchronized (this) { // BAD: Synchronized blocks can pin virtual threads
                    Thread.sleep(1000); // This pins the virtual thread to platform thread
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    // BAD: Using ThreadLocal instead of ScopedValue
    private static final ThreadLocal<String> USER_CONTEXT = new ThreadLocal<>();

    public void badContextPropagation() {
        USER_CONTEXT.set("user123");

        ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
        virtualExecutor.submit(() -> {
            // BAD: ThreadLocal values don't propagate to virtual threads properly
            String userId = USER_CONTEXT.get(); // Likely null
            performTaskForUser(userId);
        });
    }

    private void performIOOperation() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void performTaskForUser(String userId) {
        System.out.println("Processing for user: " + userId);
    }
}
```
