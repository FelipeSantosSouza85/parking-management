# java-concurrency

> Source: 125-java-concurrency.md
> Chunk: 2/5
> Included sections: Examples - Example 3: Concurrent Design Patterns | Examples - Example 4: Asynchronous Programming with CompletableFuture | Examples - Example 5: Thread Safety Guidelines (Immutability & Safe Publication)

### Example 3: Concurrent Design Patterns

Title: Implement Producer-Consumer and Publish-Subscribe
Description: Leverage established patterns like Producer-Consumer and Publish-Subscribe to structure concurrent applications effectively, promoting decoupling, scalability, and maintainability.

**Good example:**

```java
// GOOD: Producer-Consumer pattern with BlockingQueue
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

// Producer-Consumer implementation
public class ProducerConsumerExample {
    private final BlockingQueue<Task> queue = new LinkedBlockingQueue<>(100);
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private volatile boolean running = true;

    public void startProcessing() {
        // Start multiple consumers
        for (int i = 0; i < 2; i++) {
            executor.submit(this::consumer);
        }
    }

    public void produce(Task task) throws InterruptedException {
        if (running) {
            queue.put(task); // Blocks if queue is full
        }
    }

    private void consumer() {
        while (running || !queue.isEmpty()) {
            try {
                Task task = queue.poll(1, TimeUnit.SECONDS);
                if (task != null) {
                    processTask(task);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void processTask(Task task) {
        System.out.println("Processing: " + task + " on " + Thread.currentThread().getName());
        // Simulate work
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void shutdown() {
        running = false;
        executor.shutdown();
    }

    private static class Task {
        private final String data;
        public Task(String data) { this.data = data; }
        @Override public String toString() { return "Task(" + data + ")"; }
    }
}

// Publish-Subscribe implementation
public class EventBus {
    private final ConcurrentHashMap<String, Set<EventListener>> listeners = new ConcurrentHashMap<>();
    private final ExecutorService notificationExecutor = ForkJoinPool.commonPool();

    public void subscribe(String topic, EventListener listener) {
        listeners.computeIfAbsent(topic, k -> ConcurrentHashMap.newKeySet())
                 .add(listener);
    }

    public void unsubscribe(String topic, EventListener listener) {
        Set<EventListener> topicListeners = listeners.get(topic);
        if (topicListeners != null) {
            topicListeners.remove(listener);
        }
    }

    public void publish(String topic, Event event) {
        Set<EventListener> topicListeners = listeners.get(topic);
        if (topicListeners != null && !topicListeners.isEmpty()) {
            // Notify listeners asynchronously
            topicListeners.forEach(listener ->
                notificationExecutor.submit(() -> {
                    try {
                        listener.onEvent(event);
                    } catch (Exception e) {
                        System.err.println("Error notifying listener: " + e.getMessage());
                    }
                })
            );
        }
    }

    private static class Event {
        private final String data;
        public Event(String data) { this.data = data; }
        @Override public String toString() { return "Event(" + data + ")"; }
    }

    @FunctionalInterface
    private interface EventListener {
        void onEvent(Event event);
    }
}
```

**Bad example:**

```java
// AVOID: Poor concurrent pattern implementation
import java.util.ArrayList;
import java.util.List;

public class BadProducerConsumer {
    // BAD: Using non-thread-safe collection
    private List<String> tasks = new ArrayList<>();
    private boolean running = true;

    public void produce(String task) {
        // RACE CONDITION: Multiple producers can corrupt the list
        synchronized (this) {
            tasks.add(task);
            notify(); // BAD: Should use notifyAll()
        }
    }

    public void consume() {
        while (running) {
            String task = null;
            synchronized (this) {
                while (tasks.isEmpty() && running) {
                    try {
                        wait(); // BAD: Can miss notifications
                    } catch (InterruptedException e) {
                        // BAD: Not handling interruption properly
                        return;
                    }
                }
                if (!tasks.isEmpty()) {
                    task = tasks.remove(0); // BAD: Inefficient removal from front
                }
            }

            if (task != null) {
                // BAD: Processing inside synchronized block would be even worse
                processTask(task);
            }
        }
    }

    // BAD: No proper shutdown mechanism
    public void stop() {
        running = false; // Consumers might not wake up
    }
}

// BAD: Synchronous event handling
public class BadEventBus {
    private Map<String, List<EventListener>> listeners = new HashMap<>();

    public void subscribe(String topic, EventListener listener) {
        // BAD: Not thread-safe
        listeners.computeIfAbsent(topic, k -> new ArrayList<>()).add(listener);
    }

    public void publish(String topic, String event) {
        List<EventListener> topicListeners = listeners.get(topic);
        if (topicListeners != null) {
            // BAD: Synchronous notification blocks publisher
            for (EventListener listener : topicListeners) {
                try {
                    listener.onEvent(event);
                } catch (Exception e) {
                    // BAD: One failing listener affects others
                    throw new RuntimeException("Event handling failed", e);
                }
            }
        }
    }
}
```

### Example 4: Asynchronous Programming with CompletableFuture

Title: Compose Non-blocking Asynchronous Operations
Description: Employ CompletableFuture to compose and manage asynchronous computations in a non-blocking way. Chain dependent tasks, combine results from multiple futures, and handle exceptions gracefully.

**Good example:**

```java
// GOOD: CompletableFuture for asynchronous processing
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class AsyncService {
    private final ExecutorService customExecutor = Executors.newFixedThreadPool(4);

    public CompletableFuture<String> processDataAsync(String input) {
        return CompletableFuture
            .supplyAsync(() -> validateInput(input), customExecutor)
            .thenApplyAsync(this::transformData, customExecutor)
            .thenApply(this::formatResult)
            .exceptionally(this::handleError)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    System.err.println("Processing failed for: " + input);
                } else {
                    System.out.println("Successfully processed: " + input);
                }
            });
    }

    public CompletableFuture<List<String>> processMultipleAsync(List<String> inputs) {
        List<CompletableFuture<String>> futures = inputs.stream()
            .map(this::processDataAsync)
            .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList()))
            .exceptionally(ex -> {
                System.err.println("Batch processing failed: " + ex.getMessage());
                return List.of("ERROR");
            });
    }

    public CompletableFuture<String> combineResults(String input1, String input2) {
        CompletableFuture<String> future1 = processDataAsync(input1);
        CompletableFuture<String> future2 = processDataAsync(input2);

        return future1.thenCombine(future2, (result1, result2) ->
            "Combined: " + result1 + " + " + result2);
    }

    public CompletableFuture<String> getFirstSuccessful(List<String> inputs) {
        CompletableFuture<String>[] futures = inputs.stream()
            .map(this::processDataAsync)
            .toArray(CompletableFuture[]::new);

        return CompletableFuture.anyOf(futures)
            .thenApply(result -> (String) result);
    }

    private String validateInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Input cannot be null or empty");
        }
        // Simulate validation work
        try { Thread.sleep(100); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        return input.trim();
    }

    private String transformData(String input) {
        // Simulate transformation work
        try { Thread.sleep(200); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        return "transformed_" + input;
    }

    private String formatResult(String input) {
        return "[" + input + "]";
    }

    private String handleError(Throwable throwable) {
        System.err.println("Error occurred: " + throwable.getMessage());
        return "ERROR: " + throwable.getClass().getSimpleName();
    }

    public void shutdown() {
        customExecutor.shutdown();
        try {
            if (!customExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                customExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            customExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
```

**Bad example:**

```java
// AVOID: Blocking operations and poor error handling
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BadAsyncService {

    public String processDataBlocking(String input) {
        // BAD: Using CompletableFuture but blocking immediately
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            return processInput(input);
        });

        try {
            return future.get(); // BLOCKING! Defeats the purpose
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> processMultipleBlocking(List<String> inputs) {
        List<String> results = new ArrayList<>();

        // BAD: Sequential processing instead of parallel
        for (String input : inputs) {
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() ->
                processInput(input));

            try {
                results.add(future.get()); // BLOCKING in loop
            } catch (Exception e) {
                // BAD: One failure stops everything
                throw new RuntimeException("Processing failed", e);
            }
        }

        return results;
    }

    public CompletableFuture<String> badErrorHandling(String input) {
        return CompletableFuture.supplyAsync(() -> {
            if ("fail".equals(input)) {
                throw new RuntimeException("Simulated failure");
            }
            return processInput(input);
        });
        // BAD: No error handling - exceptions will propagate
    }

    public void badChaining(String input) {
        // BAD: Not chaining properly, creating nested futures
        CompletableFuture<CompletableFuture<String>> nestedFuture =
            CompletableFuture.supplyAsync(() -> {
                return CompletableFuture.supplyAsync(() -> {
                    return processInput(input);
                });
            });

        // Now you have a nested CompletableFuture - hard to work with
    }

    // BAD: Resource leak - no executor shutdown
    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    public CompletableFuture<String> processWithLeakedExecutor(String input) {
        return CompletableFuture.supplyAsync(() -> processInput(input), executor);
        // BAD: Executor never gets shut down
    }

    private String processInput(String input) {
        try {
            Thread.sleep(1000); // Simulate work
        } catch (InterruptedException e) {
            // BAD: Not handling interruption properly
        }
        return "processed_" + input;
    }
}
```

### Example 5: Thread Safety Guidelines (Immutability & Safe Publication)

Title: Ensure Thread Safety through Immutability and Safe Publication
Description: Minimize concurrency risks by designing classes to be immutable and ensuring shared objects are safely published. - **Immutability**: - Make fields `final` whenever possible. - Ensure all fields are initialized during construction. - Do not provide setters for mutable state. - Use defensive copying for mutable components (like `List` or `Date`) passed into constructors or returned from getters, or use unmodifiable collections. - Consider using the builder pattern for complex immutable objects. - **Safe Publication**: - Ensure that shared objects are correctly published to other threads (e.g., by initializing them in static initializers, storing them in `volatile` fields, or using proper synchronization). - `java.util.concurrent` collections and atomics handle safe publication internally for their state.

**Good example:**

```java
// GOOD: Immutable class with safe publication
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Immutable class
public final class ImmutableValue {
    private final int value;
    private final List<String> items;

    public ImmutableValue(int value, List<String> items) {
        this.value = value;
        // Defensive copy to ensure immutability
        this.items = List.copyOf(items);
    }

    public int getValue() {
        return value;
    }

    public List<String> getItems() {
        return items; // Already immutable
    }
}

// Safe publication example
class SafePublicationExample {
    private static final Map<String, String> cache = new ConcurrentHashMap<>();

    public static String getData(String key) {
        return cache.computeIfAbsent(key, k -> {
            // Safe publication through ConcurrentHashMap
            return "Data for " + k;
        });
    }
}
```

**Bad example:**

```java
// AVOID: Mutable class with unsafe publication
import java.util.ArrayList;
import java.util.List;

public class MutableValue {
    private int value;
    private List<String> items; // Mutable field

    public MutableValue(int value, List<String> items) {
        this.value = value;
        this.items = items; // Direct reference - not safe
    }

    public List<String> getItems() {
        return items; // Returns mutable reference
    }

    // BAD: Unsafe publication
    public static MutableValue instance;

    public static void initialize() {
        // Unsafe publication - other threads might see partially constructed object
        instance = new MutableValue(42, new ArrayList<>());
    }
}
```
