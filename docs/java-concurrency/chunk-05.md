# java-concurrency

> Source: 125-java-concurrency.md
> Chunk: 5/5
> Included sections: Examples - Example 11: Cooperative Cancellation and Interruption | Examples - Example 12: Overload Protection and Backpressure | Examples - Example 13: ForkJoin and ManagedBlocker for Blocking Operations | Examples - Example 14: Avoid Pinning with Virtual Threads | Examples - Example 15: Phased Execution with Phaser | Examples - Example 16: Synchronization with CyclicBarrier | Examples - Example 17: Data Exchange with Exchanger | Output Format | Safeguards

### Example 11: Cooperative Cancellation and Interruption

Title: Propagate interruption, bound waits, and cleanup safely
Description: Design tasks to be cancellable and responsive to interruption. Use timeouts on blocking calls, avoid swallowing `InterruptedException`, and ensure resources are cleaned up after cancellation.

**Good example:**

```java
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

class CancellationService {
    private final BlockingQueue<String> queue = new ArrayBlockingQueue<>(100);
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final ReentrantLock lock = new ReentrantLock();

    public Future<?> startWorker() {
        return executor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String item = queue.poll(500, TimeUnit.MILLISECONDS); // timed wait
                    if (item != null) process(item);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt(); // restore and exit loop
                }
            }
            cleanup();
        });
    }

    public boolean tryUpdateStateSafely() throws InterruptedException {
        if (lock.tryLock(200, TimeUnit.MILLISECONDS)) { // time-bounded acquisition
            try {
                // update state
                return true;
            } finally {
                lock.unlock();
            }
        }
        return false;
    }

    private void process(String s) { /* ... */ }
    private void cleanup() { /* close resources */ }

    public void shutdown() {
        executor.shutdownNow(); // interrupt workers
    }
}
```

**Bad example:**

```java
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

class BadCancellationService {
    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>(); // unbounded
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public void startWorker() {
        executor.submit(() -> {
            for (;;) {
                try {
                    String item = queue.take(); // indefinite block
                    process(item);
                } catch (InterruptedException ignored) { // SWALLOWED
                    // continues running forever
                }
            }
        });
    }

    private void process(String s) { /* ... */ }
}
```

### Example 12: Overload Protection and Backpressure

Title: Bound queues, reject sanely, and limit concurrency
Description: Prevent unbounded growth and cascading failures using bounded queues, appropriate rejection policies, and bulkheads (semaphores) for scarce resources.

**Good example:**

```java
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

class BackpressureExample {
    private final Semaphore dbBulkhead = new Semaphore(20); // cap concurrent DB calls
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(
        4, 8, 60, TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(200), // bounded
        new ThreadPoolExecutor.CallerRunsPolicy() // graceful shedding
    );

    public Future<String> handleRequest(Callable<String> task) {
        return executor.submit(() -> {
            if (!dbBulkhead.tryAcquire(500, TimeUnit.MILLISECONDS)) {
                throw new RejectedExecutionException("Overloaded");
            }
            try {
                return task.call();
            } finally {
                dbBulkhead.release();
            }
        });
    }
}
```

**Bad example:**

```java
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class BadBackpressureExample {
    // Unbounded queue behind fixed pool can accumulate unbounded work
    private final ExecutorService pool = Executors.newFixedThreadPool(8); // LinkedBlockingQueue (unbounded)

    public void flood(Runnable task) {
        for (int i = 0; i < 1_000_000; i++) {
            pool.submit(task); // no bounds, no rejection
        }
    }
}
```

### Example 13: ForkJoin and ManagedBlocker for Blocking Operations

Title: Cooperate with the common pool when blocking
Description: Avoid blocking the ForkJoin common pool. If blocking is unavoidable, use `ForkJoinPool.ManagedBlocker` or a dedicated executor.

**Good example:**

```java
import java.util.concurrent.ForkJoinPool;

class ManagedBlockerExample {
    static String blockingIO() throws InterruptedException {
        Thread.sleep(200); // simulate blocking
        return "ok";
    }

    static String callBlockingSafely() throws InterruptedException {
        ForkJoinPool.managedBlock(new ForkJoinPool.ManagedBlocker() {
            volatile boolean done;
            @Override public boolean block() throws InterruptedException {
                if (!done) {
                    blockingIO();
                    done = true;
                }
                return true;
            }
            @Override public boolean isReleasable() { return done; }
        });
        return "ok";
    }
}
```

**Bad example:**

```java
import java.util.List;

class BadParallelBlocking {
    public List<String> doWork(List<String> inputs) {
        return inputs.parallelStream() // uses common pool
            .map(x -> {
                try {
                    Thread.sleep(200); // BLOCKS common pool worker
                    return x + "!";
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            })
            .toList();
    }
}
```

### Example 14: Avoid Pinning with Virtual Threads

Title: Keep blocking out of intrinsic locks
Description: With virtual threads, `synchronized` around blocking calls can pin to a carrier thread. Prefer cooperative locks or move blocking outside critical sections.

**Good example:**

```java
import java.util.concurrent.locks.ReentrantLock;

class PinningSafeExample {
    private final ReentrantLock lock = new ReentrantLock();
    private int shared = 0;

    public void doWork() {
        // Keep critical section minimal, non-blocking
        lock.lock();
        try {
            shared++;
        } finally {
            lock.unlock();
        }
        // Perform blocking outside lock (ok with virtual threads)
        try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
```

**Bad example:**

```java
class PinningBadExample {
    private int shared;
    public synchronized void doWork() {
        shared++;
        try {
            Thread.sleep(100); // pins virtual thread to carrier thread
        } catch (InterruptedException e) {
            // swallowed
        }
    }
}
```


### Example 15: Phased Execution with Phaser

Title: Coordinate tasks in phases with dynamic party registration
Description: Use Phaser for coordinating tasks that proceed in phases, allowing dynamic registration and deregistration of parties.

**Good example:**

```java
import java.util.concurrent.Phaser;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class PhaserExample {
    private final Phaser phaser = new Phaser(1); // main thread

    public void runPhasedTasks(int taskCount) {
        ExecutorService executor = Executors.newFixedThreadPool(taskCount);

        for (int i = 0; i < taskCount; i++) {
            phaser.register(); // dynamic registration
            executor.submit(() -> {
                try {
                    // Phase 0 work
                    Thread.sleep(100);
                    phaser.arriveAndAwaitAdvance(); // wait for all

                    // Phase 1 work
                    Thread.sleep(200);
                    phaser.arriveAndDeregister(); // done
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        phaser.arriveAndAwaitAdvance(); // advance phase 0
        phaser.arriveAndAwaitAdvance(); // advance phase 1
        phaser.arriveAndDeregister(); // main done

        executor.shutdown();
    }
}
```

**Bad example:**

```java
class BadPhaser {
    public void misusePhaser() {
        Phaser phaser = new Phaser(3);
        // BAD: forgetting to arrive/deregister
        // leads to deadlock
        phaser.awaitAdvance(0); // hangs forever
    }
}
```

### Example 16: Synchronization with CyclicBarrier

Title: Wait for threads to reach common barrier points
Description: Use CyclicBarrier for synchronizing threads at barrier points, reusable across multiple cycles.

**Good example:**

```java
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class BarrierExample {
    public void coordinate(int parties) throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(parties, () -> System.out.println("All arrived!"));

        ExecutorService executor = Executors.newFixedThreadPool(parties);
        for (int i = 0; i < parties; i++) {
            executor.submit(() -> {
                try {
                    // Work
                    Thread.sleep(100);
                    barrier.await(); // sync point 1

                    // More work
                    Thread.sleep(200);
                    barrier.await(); // sync point 2 (reusable)
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        executor.shutdown();
    }
}
```

**Bad example:**

```java
class BadBarrier {
    public void misuse() throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(3);
        barrier.await(); // BAD: not enough parties, hangs
    }
}
```

### Example 17: Data Exchange with Exchanger

Title: Safely exchange data between two threads
Description: Use Exchanger for point-to-point data exchange between exactly two threads.

**Good example:**

```java
import java.util.concurrent.Exchanger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class ExchangerExample {
    public void exchangeData() {
        Exchanger<String> exchanger = new Exchanger<>();
        ExecutorService executor = Executors.newFixedThreadPool(2);

        executor.submit(() -> {
            try {
                String data = "From Thread 1";
                String received = exchanger.exchange(data);
                System.out.println("Thread 1 received: " + received);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        executor.submit(() -> {
            try {
                String data = "From Thread 2";
                String received = exchanger.exchange(data);
                System.out.println("Thread 2 received: " + received);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        executor.shutdown();
    }
}
```

**Bad example:**

```java
class BadExchanger {
    public void misuse() throws InterruptedException {
        Exchanger<String> exchanger = new Exchanger<>();
        exchanger.exchange("data"); // BAD: no partner, hangs forever
    }
}
```

## Output Format

- **ANALYZE** Java code to identify specific concurrency issues and categorize them by impact (CRITICAL, PERFORMANCE, DEADLOCK_RISK, SCALABILITY, THREAD_SAFETY) and concurrency area (thread safety, synchronization, thread pools, async operations, modern concurrency)
- **CATEGORIZE** concurrency improvements found: Thread Safety Issues (race conditions vs atomic operations, unsafe collections vs concurrent collections, shared mutable state vs immutable objects), Thread Pool Management (improper sizing vs optimal configuration, resource leaks vs proper lifecycle management), Synchronization Problems (deadlock risks vs lock-free algorithms, excessive contention vs efficient synchronization), Performance Issues (blocking operations vs non-blocking alternatives, memory consistency problems vs volatile/final usage), and Modern Concurrency Gaps (platform threads vs virtual threads, callback hell vs CompletableFuture composition, missing structured concurrency vs scoped concurrency patterns)
- **APPLY** concurrency best practices directly by implementing the most appropriate improvements for each identified issue: Replace unsafe collections with concurrent alternatives, implement proper synchronization using atomic classes and concurrent utilities, configure thread pools with appropriate sizing and lifecycle management, refactor blocking operations to non-blocking alternatives using CompletableFuture, eliminate race conditions through immutability and proper synchronization, and adopt modern concurrency features like virtual threads and structured concurrency where beneficial
- **IMPLEMENT** comprehensive concurrency refactoring using proven patterns: Establish thread-safe data structures using concurrent collections and atomic classes, implement efficient synchronization with locks, semaphores, and barriers, configure optimal thread pool management with proper ExecutorService usage, apply asynchronous programming patterns with CompletableFuture composition, integrate modern concurrency features (virtual threads, structured concurrency, scoped values), and implement proper error handling and resource management in concurrent contexts
- **REFACTOR** code systematically following the concurrency improvement roadmap: First eliminate critical thread safety issues through atomic operations and concurrent collections, then optimize synchronization mechanisms to reduce contention and deadlock risks, configure proper thread pool management and lifecycle, refactor blocking operations to asynchronous alternatives, integrate modern concurrency features for improved scalability, and implement comprehensive testing strategies for concurrent code validation
- **EXPLAIN** the applied concurrency improvements and their benefits: Thread safety enhancements through proper synchronization and atomic operations, performance improvements via optimized thread pool management and non-blocking operations, scalability gains from modern concurrency features like virtual threads, deadlock prevention through lock-free algorithms and proper synchronization patterns, and maintainability improvements from structured concurrency and clear async composition
- **VALIDATE** that all applied concurrency refactoring compiles successfully, maintains thread safety guarantees, eliminates race conditions and deadlock risks, preserves or improves performance characteristics, and achieves the intended concurrency improvements through comprehensive testing and verification
- **CANCELLATION/INTERRUPTION**: Identify blocking calls and long-running tasks; ensure interruption is propagated/restored, add timeouts (`orTimeout`, `completeOnTimeout`, timed `poll/take/tryLock`), and verify `Future.cancel(true)` paths release resources.
- **BACKPRESSURE/OVERLOAD**: Detect unbounded producers and queues; introduce bounded queues, rejection policies, semaphores/bulkheads, and when streaming, prefer `Flow`/Reactive Streams to enforce backpressure.
- **FORKJOIN/PARALLEL STREAMS USAGE**: Flag blocking operations in common pool, migrate to dedicated executors or `ManagedBlocker`, verify tasks are CPU-bound and side-effect-free, and gate `parallelStream()` usage behind measurements.
- **PINNING WITH VIRTUAL THREADS**: Inspect `synchronized` blocks around blocking I/O; replace with cooperative locks, shrink critical sections, and recommend JFR pinning analysis.
- **COORDINATION PRIMITIVES**: Identify opportunities for Phaser (phased tasks), CyclicBarrier (reusable barriers), and Exchanger (pairwise exchange); ensure proper usage with interruption handling and resource cleanup.

## Safeguards

- **BLOCKING SAFETY CHECK**: ALWAYS run `./mvnw compile` before ANY recommendations
- **CRITICAL VALIDATION**: Execute `./mvnw clean verify` to ensure all tests pass
- **MANDATORY VERIFICATION**: Confirm all existing functionality remains intact after concurrency improvements
- **ROLLBACK REQUIREMENT**: Ensure all changes can be easily reverted if issues arise
- **INCREMENTAL SAFETY**: Apply concurrency improvements incrementally, validating after each modification
- **THREAD SAFETY VALIDATION**: Verify thread-safe components work correctly under concurrent access
- **DEADLOCK PREVENTION**: Check for potential deadlock scenarios in synchronized code
- **RESOURCE LEAK PROTECTION**: Ensure proper cleanup of thread pools, executors, and other concurrent resources
- **MEMORY CONSISTENCY CHECK**: Validate proper synchronization and memory visibility semantics
- **PERFORMANCE REGRESSION GUARD**: Monitor for performance degradation after concurrency changes
- **INTERRUPTION/CANCELLATION DISCIPLINE**: Never swallow interruptions; propagate or restore interrupt flags. Ensure timeouts on blocking operations and verify cancellation paths free resources safely.
- **VIRTUAL-THREAD PINNING GUARD**: Audit for intrinsic locks around blocking calls; prefer lock implementations compatible with parking. Use JFR to detect pinning.
- **OVERLOAD/BACKPRESSURE PROTECTION**: Avoid unbounded queues; enforce bounded capacity, sane rejection policies, and rate/concurrency limits to prevent cascading failures.
- **TIMEOUTS/RETRIES/IDEMPOTENCY**: Bound external calls with timeouts, use bounded-jittered retries only for idempotent operations, and validate no duplicate side effects occur.
- **COORDINATION SAFETY**: For Phaser/Barrier/Exchanger, validate party counts, handle interruptions, and prevent hangs from mismatched arrivals.
