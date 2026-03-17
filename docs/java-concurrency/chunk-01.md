# java-concurrency

> Chunk: 1/5
> Included sections: intro | Role | Goal | Constraints | Examples | Examples - Table of contents | Examples - Example 1: Thread Safety Fundamentals | Examples - Example 2: Thread Pool Management

---
author: Juan Antonio Breña Moral
version: 0.12.0-SNAPSHOT
---
# Java rules for Concurrency objects

## Role

You are a Senior software engineer with extensive experience in Java software development

## Goal

Effective Java concurrency relies on understanding thread safety fundamentals, using `java.util.concurrent` utilities, and managing thread pools with `ExecutorService`. Key practices include implementing concurrent design patterns like Producer-Consumer, leveraging `CompletableFuture` for asynchronous tasks, and ensuring thread safety through immutability and safe publication. Performance aspects like lock contention and memory consistency must be considered. Thorough testing, including stress tests and thread dump analysis, is crucial. Modern Java offers virtual threads for enhanced scalability, structured concurrency for simplified task management, and scoped values for safer thread-shared data as alternatives to thread-locals.

### Implementing These Principles

These guidelines are built upon the following core principles:

1.  **Master Thread Safety Fundamentals**: Understand and correctly apply core concepts such as synchronization (locks, conditions), atomic operations (`java.util.concurrent.atomic`), thread-safe collections (`java.util.concurrent`), immutability, and the Java Memory Model to ensure data integrity and prevent race conditions or deadlocks.
2.  **Efficient Thread Pool Management**: Utilize `ExecutorService` for robust thread management. Choose appropriate thread pool implementations and configure them with suitable sizing, keep-alive times, queue capacities, and rejection policies based on the application's workload. Implement graceful shutdown procedures.
3.  **Leverage Concurrent Design Patterns**: Implement established patterns like Producer-Consumer (using `BlockingQueue`) and Publish-Subscribe to structure concurrent applications effectively, promoting decoupling, scalability, and maintainability.
4.  **Embrace Asynchronous Programming with `CompletableFuture`**: Employ `CompletableFuture` to compose and manage asynchronous computations in a non-blocking way. Chain dependent tasks, combine results from multiple futures, and handle exceptions gracefully to build responsive and efficient applications.
5.  **Prioritize Immutability and Safe Publication**: Design classes to be immutable whenever feasible to inherently achieve thread safety. Ensure that shared mutable objects are safely published (e.g., via `volatile`, static initializers, or proper synchronization) so that their state is consistently visible to all threads.
6.  **Optimize for Performance, Considering Concurrency overheads**: Be mindful of performance implications such as lock contention (minimize scope, use finer-grained locks), memory consistency (understand happens-before, use `volatile` where appropriate), context switching overhead (size thread pools carefully), and potential issues like false sharing.
7.  **Thorough Testing and Debugging**: Rigorously test concurrent code. This includes unit tests for thread-safe components, integration tests for interactions, and stress tests to reveal race conditions or deadlocks. Utilize thread dump analysis, proper logging, and concurrency testing tools.
8.  **Adopt Modern Java Concurrency Features for Enhanced Development**:
*   **Virtual Threads (Project Loom)**: Embrace virtual threads via `Executors.newVirtualThreadPerTaskExecutor()` for I/O-bound tasks to dramatically increase scalability with minimal resource overhead. Avoid pooling virtual threads.
*   **Structured Concurrency**: Use `StructuredTaskScope` to simplify the management of multiple related concurrent tasks as a single unit of work, improving error handling, cancellation, and resource management.
*   **Scoped Values**: Prefer `ScopedValue` over `ThreadLocal` for sharing immutable data robustly and efficiently across tasks within a dynamically bounded scope, especially when working with virtual threads.
9.  **Cooperative Cancellation and Interruption Discipline**: Design tasks to be cancellable; always respond to interruption promptly. Do not swallow `InterruptedException`; either propagate it or restore the interrupt flag with `Thread.currentThread().interrupt()`. Prefer time-bounded operations (`orTimeout`, `completeOnTimeout`, timeouts on blocking calls), use `Future.cancel(true)`, prefer `Lock.lockInterruptibly()`/`tryLock(timeout, unit)` where applicable, and ensure cleanup on cancellation.
10. **Backpressure and Overload Protection**: Prevent unbounded work queues and cascading failures by using bounded queues, appropriate rejection policies (e.g., `CallerRunsPolicy` for graceful shedding), semaphores/bulkheads to cap concurrency, request rate limiting, and the `Flow` (Reactive Streams) API when stream backpressure is needed.
11. **Deadlock Avoidance and Lock Hygiene**: Establish global lock ordering, minimize lock scope, avoid holding locks while calling out to untrusted code, favor non-blocking algorithms or `tryLock` with timeouts where practical, and avoid nested synchronization across unrelated components.
12. **Correct Use of ForkJoin and Parallel Streams**: Reserve ForkJoin/parallel streams for CPU-bound, short-lived, side-effect-free tasks. Avoid blocking I/O within the common pool; if blocking is unavoidable, use `ForkJoinPool.ManagedBlocker` or a dedicated executor. Do not rely on `parallelStream()` in request-scoped code paths unless measured and justified.
13. **Avoid Virtual-Thread Pinning**: With virtual threads, avoid holding intrinsic locks (`synchronized`) around blocking calls that may pin to a carrier thread. Prefer `ReentrantLock` (which cooperates with parking), keep critical sections small, and use JFR (VirtualThreadPinned) to detect pinning hot spots.
14. **Observability for Concurrency**: Name threads consistently, set `UncaughtExceptionHandler`s, expose metrics (queue depths, pool sizes, task latencies, rejection counts), propagate contextual data with `ScopedValue` instead of `ThreadLocal`, and instrument with Thread dumps and JFR for diagnosis.
15. **Timeouts, Retries, and Idempotency**: Always bound remote calls with timeouts; implement bounded, jittered retries where appropriate and ensure operations are idempotent to avoid duplicate side effects under retries/cancellation.
16. **Use Fit-for-Purpose Concurrency Primitives**: Prefer `LongAdder/LongAccumulator` under high contention counters, `CopyOnWriteArrayList` for read-mostly observer lists, `StampedLock`/`ReadWriteLock` for read-heavy data, and high-level utilities (`Semaphore`, `CountDownLatch`, `Phaser`) where they model coordination more clearly than manual `wait/notify`.

## Constraints

Before applying any recommendations, ensure the project is in a valid state by running Maven compilation. Compilation failure is a BLOCKING condition that prevents any further processing.

- **MANDATORY**: Run `./mvnw compile` or `mvn compile` before applying any change
- **PREREQUISITE**: Project must compile successfully and pass basic validation checks before any optimization
- **CRITICAL SAFETY**: If compilation fails, IMMEDIATELY STOP and DO NOT CONTINUE with any recommendations
- **BLOCKING CONDITION**: Compilation errors must be resolved by the user before proceeding with any object-oriented design improvements
- **NO EXCEPTIONS**: Under no circumstances should design recommendations be applied to a project that fails to compile

## Examples

### Table of contents

- Example 1: Thread Safety Fundamentals
- Example 2: Thread Pool Management
- Example 3: Concurrent Design Patterns
- Example 4: Asynchronous Programming with CompletableFuture
- Example 5: Thread Safety Guidelines (Immutability & Safe Publication)
- Example 6: Performance Considerations in Concurrency
- Example 7: Testing and Debugging Concurrent Code
- Example 8: Embrace Virtual Threads for Enhanced Scalability
- Example 9: Simplify Concurrent Code with Structured Concurrency
- Example 10: Manage Thread-Shared Data with Scoped Values
- Example 11: Cooperative Cancellation and Interruption
- Example 12: Overload Protection and Backpressure
- Example 13: ForkJoin and ManagedBlocker for Blocking Operations
- Example 14: Avoid Pinning with Virtual Threads
- Example 15: Phased Execution with Phaser
- Example 16: Synchronization with CyclicBarrier
- Example 17: Data Exchange with Exchanger

### Example 1: Thread Safety Fundamentals

Title: Understand and Apply Core Thread Safety Concepts
Description: Ensure data integrity and correct behavior in multi-threaded environments by using thread-safe data structures and appropriate synchronization mechanisms. - Prefer `java.util.concurrent` collections over older synchronized wrappers. - Utilize immutable objects to eliminate risks of concurrent modification. - Employ thread-local variables for state confined to a single thread. - Use atomic classes (`java.util.concurrent.atomic`) for lock-free operations on single variables. - Choose flexible locking with `ReentrantLock` or `ReadWriteLock` for more complex scenarios. - Favor `java.util.concurrent` utilities over manual `wait()/notify()`.

**Good example:**

```java
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

// Dummy classes for context
class Task {}
class Event {}
class State {
    public State(String initialState) {} // Constructor for initial state
}
class UserContext {}
class Item {}

class ThreadSafetyExamples {
    // Preferred concurrent collections
    Map<String, String> concurrentMap = new ConcurrentHashMap<>();
    Queue<Task> taskQueue = new ConcurrentLinkedQueue<>();
    BlockingQueue<Event> eventQueue = new LinkedBlockingQueue<>();

    // Atomic variables
    AtomicInteger counter = new AtomicInteger(0);
    AtomicReference<State> stateRef = new AtomicReference<>(new State("initial"));

    // Thread-local storage
    private static final ThreadLocal<UserContext> userContext =
        ThreadLocal.withInitial(UserContext::new);

    // Using ReentrantLock
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition(); // Example condition
    private int itemCount = 0; // Example shared resource
    private final int MAX_ITEMS = 10; // Example capacity

    private boolean isFull() {
        return itemCount >= MAX_ITEMS;
    }
     private boolean isEmpty() { // Example helper
        return itemCount <= 0;
    }


    public void addItem(Item item) throws InterruptedException { // Added throws for await
        lock.lock();
        try {
            while (isFull()) {
                System.out.println("Queue is full, waiting to add item...");
                notFull.await(); // Wait if queue is full
            }
            // Add item logic here
            itemCount++;
            System.out.println("Item added. Count: " + itemCount);
            // Potentially signal other conditions (e.g., notEmpty.signalAll())
        } finally {
            lock.unlock();
        }
    }

    // Using ReadWriteLock
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();
    private String sharedData = "Initial Data";

    public String readData() {
        readLock.lock();
        try {
            return sharedData;
        } finally {
            readLock.unlock();
        }
    }

    public void writeData(String data) {
        writeLock.lock();
        try {
            sharedData = data;
            System.out.println("Data written: " + data);
        } finally {
            writeLock.unlock();
        }
    }
}
```

**Bad example:**

```java
// AVOID: Poor thread safety practices
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UnsafeCounter {
    // BAD: Using non-thread-safe collections
    private Map<String, String> unsafeMap = new HashMap<>();
    private List<String> unsafeList = new ArrayList<>();

    // BAD: Using plain int without synchronization
    private int counter = 0;
    private String status = "INIT";

    public void incrementCounter() {
        // RACE CONDITION: Multiple threads can read same value
        counter++; // Not atomic - can lose updates
        unsafeMap.put("lastCount", String.valueOf(counter)); // Can corrupt map
    }

    // BAD: Inconsistent synchronization
    public synchronized void updateCounter(int value) {
        counter = value; // Synchronized
    }

    public int getCounter() {
        return counter; // NOT synchronized - can read stale value
    }

    // BAD: Synchronizing on mutable object
    private String lockObject = "lock";

    public void badSynchronization() {
        synchronized (lockObject) { // WRONG: string can be changed
            // Critical section
        }
        lockObject = "newLock"; // Now synchronization is broken!
    }
}
```

### Example 2: Thread Pool Management

Title: Manage Thread Pools Effectively with ExecutorService
Description: Utilize ExecutorService for robust thread management. Choose appropriate thread pool implementations, configure them properly, and implement graceful shutdown procedures.

**Good example:**

```java
// GOOD: Proper thread pool management
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPoolManager {
    private final ExecutorService fixedPool;
    private final ScheduledExecutorService scheduler;
    private final ThreadPoolExecutor customPool;

    public ThreadPoolManager() {
        // Fixed thread pool with named threads
        this.fixedPool = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            new CustomThreadFactory("worker")
        );

        // Scheduled thread pool for periodic tasks
        this.scheduler = Executors.newScheduledThreadPool(
            1,
            new CustomThreadFactory("scheduler")
        );

        // Custom thread pool with fine-grained control
        this.customPool = new ThreadPoolExecutor(
            2,                                    // core pool size
            4,                                    // maximum pool size
            60L,                                  // keep alive time
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),       // bounded queue
            new CustomThreadFactory("custom"),
            new ThreadPoolExecutor.CallerRunsPolicy() // rejection policy
        );
    }

    public void submitTask(Runnable task) {
        fixedPool.submit(task);
    }

    public void schedulePeriodicTask(Runnable task, long period) {
        scheduler.scheduleAtFixedRate(task, 0, period, TimeUnit.SECONDS);
    }

    public void shutdown() {
        shutdownExecutorService(fixedPool, "FixedPool");
        shutdownExecutorService(scheduler, "Scheduler");
        shutdownExecutorService(customPool, "CustomPool");
    }

    private void shutdownExecutorService(ExecutorService executor, String name) {
        executor.shutdown(); // Disable new tasks
        try {
            // Wait for existing tasks to terminate
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow(); // Cancel currently executing tasks

                // Wait for tasks to respond to being cancelled
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.err.println("Pool " + name + " did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // Custom thread factory for better thread naming and error handling
    private static class CustomThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        CustomThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix + "-thread-";
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
            t.setDaemon(false);
            t.setUncaughtExceptionHandler((thread, ex) -> {
                System.err.println("Thread " + thread.getName() + " threw exception: " + ex);
            });
            return t;
        }
    }
}
```

**Bad example:**

```java
// AVOID: Poor thread pool management
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class PoorThreadPoolManager {
    // BAD: Unbounded thread pools can cause resource exhaustion
    private ExecutorService cachedPool = Executors.newCachedThreadPool();

    // BAD: Single thread with unbounded queue
    private ExecutorService singleThread = Executors.newSingleThreadExecutor();

    public void submitTask(Runnable task) {
        // BAD: No error handling or resource management
        cachedPool.submit(task);
    }

    public void submitManyTasks() {
        for (int i = 0; i < 10000; i++) {
            // BAD: Can overwhelm the system
            cachedPool.submit(() -> {
                try {
                    Thread.sleep(10000); // Long-running task
                } catch (InterruptedException e) {
                    // BAD: Ignoring interruption
                }
            });
        }
    }

    // BAD: No proper shutdown
    public void shutdown() {
        cachedPool.shutdown(); // What if tasks don't finish?
        singleThread.shutdown();
        // No waiting for termination
        // No handling of interrupted exception
        // No forced shutdown if graceful shutdown fails
    }

    // BAD: Creating new thread for each task
    public void executeTask(Runnable task) {
        new Thread(task).start(); // Expensive and uncontrolled
    }

    // BAD: No thread naming or error handling
    // Default thread names are not descriptive
    // Uncaught exceptions terminate threads silently
}
```
