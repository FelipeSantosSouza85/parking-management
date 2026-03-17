# java-exception-handling

> Source: 127-java-exception-handling.md
> Chunk: 3/6
> Included sections: Examples - Example 5: Thread Interruption and Concurrent Exception Handling

### Example 5: Thread Interruption and Concurrent Exception Handling

Title: Handle InterruptedException and Concurrent Operations Properly
Description: Handle InterruptedException correctly by restoring the interrupted status and taking appropriate action. In concurrent code, ensure exception handling doesn't break thread safety or leave threads in inconsistent states.

**Good example:**

```java
// GOOD: Proper thread interruption and concurrent exception handling
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ConcurrentProcessor {
    private static final Logger logger = LoggerFactory.getLogger(ConcurrentProcessor.class);
    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    private final Lock processLock = new ReentrantLock();

    /**
     * Processes tasks with proper interruption handling.
     *
     * @param tasks the tasks to process
     * @return processing results
     * @throws ProcessingException if processing fails
     * @throws InterruptedException if thread is interrupted
     */
    public List<Result> processTasks(List<Task> tasks) throws ProcessingException, InterruptedException {
        List<Future<Result>> futures = new ArrayList<>();

        try {
            // Submit tasks
            for (Task task : tasks) {
                Future<Result> future = executor.submit(() -> processTask(task));
                futures.add(future);
            }

            // Collect results with proper timeout
            List<Result> results = new ArrayList<>();
            for (Future<Result> future : futures) {
                try {
                    // Use timeout to avoid indefinite blocking
                    Result result = future.get(30, TimeUnit.SECONDS);
                    results.add(result);

                } catch (InterruptedException e) {
                    // Restore interrupted status immediately
                    Thread.currentThread().interrupt();

                    // Cancel remaining tasks
                    cancelRemainingTasks(futures);

                    logger.info("Task processing interrupted");
                    throw e; // Re-throw to caller

                } catch (TimeoutException e) {
                    logger.warn("Task processing timed out, cancelling task");
                    future.cancel(true); // Interrupt if necessary
                    throw new ProcessingException("Task processing timed out", e);

                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    logger.error("Task execution failed", cause);
                    throw new ProcessingException("Task execution failed", cause);
                }
            }

            return results;

        } finally {
            // Clean up any remaining futures
            cancelRemainingTasks(futures);
        }
    }

    /**
     * Processes a single task with proper interruption checking.
     *
     * @param task the task to process
     * @return processing result
     * @throws ProcessingException if processing fails
     */
    private Result processTask(Task task) throws ProcessingException {
        try {
            // Check interruption status before starting
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Thread interrupted before processing");
            }

            // Step 1: Validate task
            validateTask(task);

            // Check interruption between steps
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Thread interrupted during validation");
            }

            // Step 2: Process with simulated work
            Thread.sleep(100); // Simulated processing time

            // Step 3: Generate result
            return new Result(task.getId(), "Processed successfully");

        } catch (InterruptedException e) {
            // Restore interrupted status
            Thread.currentThread().interrupt();

            logger.info("Task processing interrupted: {}", task.getId());
            throw new ProcessingException("Task processing was interrupted", e);

        } catch (Exception e) {
            logger.error("Unexpected error processing task: {}", task.getId(), e);
            throw new ProcessingException("Failed to process task: " + task.getId(), e);
        }
    }

    /**
     * Thread-safe method with proper lock exception handling.
     *
     * @param data the data to process safely
     * @throws ProcessingException if processing fails
     */
    public void processWithLock(String data) throws ProcessingException {
        boolean lockAcquired = false;

        try {
            // Try to acquire lock with timeout
            lockAcquired = processLock.tryLock(5, TimeUnit.SECONDS);

            if (!lockAcquired) {
                throw new ProcessingException("Could not acquire processing lock within timeout");
            }

            // Critical section - process data
            performCriticalProcessing(data);

        } catch (InterruptedException e) {
            // Restore interrupted status
            Thread.currentThread().interrupt();

            logger.info("Lock acquisition interrupted");
            throw new ProcessingException("Processing interrupted while waiting for lock", e);

        } catch (Exception e) {
            logger.error("Error during locked processing", e);
            throw new ProcessingException("Processing failed", e);

        } finally {
            // Always release lock if acquired
            if (lockAcquired) {
                processLock.unlock();
            }
        }
    }

    /**
     * Graceful shutdown with proper exception handling.
     */
    public void shutdown() {
        try {
            executor.shutdown();

            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                logger.warn("Executor did not terminate gracefully, forcing shutdown");
                executor.shutdownNow();

                // Wait a bit more for tasks to respond to being cancelled
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    logger.error("Executor did not terminate after forced shutdown");
                }
            }

        } catch (InterruptedException e) {
            // Restore interrupted status
            Thread.currentThread().interrupt();

            logger.warn("Shutdown interrupted, forcing immediate shutdown");
            executor.shutdownNow();
        }
    }

    private void cancelRemainingTasks(List<Future<Result>> futures) {
        for (Future<Result> future : futures) {
            if (!future.isDone()) {
                future.cancel(true);
            }
        }
    }

    private void validateTask(Task task) throws ValidationException {
        if (task == null || task.getId() == null) {
            throw new ValidationException("Invalid task: null task or ID");
        }
    }

    private void performCriticalProcessing(String data) {
        // Simulate critical processing
        logger.debug("Processing data in critical section: {}", data);
    }
}
```

**Bad example:**

```java
// AVOID: Poor thread interruption and concurrent exception handling
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class BadConcurrentProcessor {
    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    // BAD: Not handling InterruptedException properly
    public List<Result> processTasks(List<Task> tasks) throws Exception {
        List<Future<Result>> futures = new ArrayList<>();

        for (Task task : tasks) {
            Future<Result> future = executor.submit(() -> processTask(task));
            futures.add(future);
        }

        List<Result> results = new ArrayList<>();
        for (Future<Result> future : futures) {
            try {
                results.add(future.get());
            } catch (InterruptedException e) {
                // BAD: Ignoring interruption
                System.out.println("Interrupted, but continuing anyway");
                // NOT restoring interrupted status
            } catch (Exception e) {
                // BAD: Generic exception handling
                throw new Exception("Something failed");
            }
        }

        return results;
    }

    // BAD: Poor interruption handling in long-running task
    private Result processTask(Task task) {
        try {
            // BAD: Not checking interruption status
            for (int i = 0; i < 1000; i++) {
                // Long-running work without interruption checks
                Thread.sleep(10);
                // Should check Thread.currentThread().isInterrupted()
            }

            return new Result(task.getId(), "Processed");

        } catch (InterruptedException e) {
            // BAD: Swallowing InterruptedException
            return new Result(task.getId(), "Failed");
        }
    }

    // BAD: Poor lock exception handling
    public void processWithLock(String data) {
        try {
            lock.lock(); // BAD: No timeout, can block forever

            // Process data

        } catch (Exception e) {
            // BAD: Generic exception handling doesn't distinguish lock issues
            e.printStackTrace();
        } finally {
            // BAD: Releasing lock even if never acquired
            lock.unlock(); // Could throw IllegalMonitorStateException
        }
    }

    // BAD: Poor shutdown handling
    public void shutdown() {
        executor.shutdown();

        try {
            executor.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // BAD: Not handling interruption during shutdown
        }

        // BAD: No forced shutdown if graceful shutdown fails
    }

    // BAD: Race condition in exception handling
    private volatile boolean processing = false;

    public void unsafeProcess(String data) {
        try {
            processing = true;

            // Some processing...

        } catch (Exception e) {
            // BAD: Exception handling has race condition
            if (processing) { // Could be changed by another thread
                processing = false;
            }
        }
        // BAD: processing flag might not be reset if no exception
    }

    // BAD: Not preserving interrupted status in utility method
    public void waitForCondition() {
        while (!conditionMet()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // BAD: Not restoring interrupted status
                break; // Exits but doesn't signal interruption to caller
            }
        }
    }
}
```
