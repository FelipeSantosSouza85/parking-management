# java-functional-programming

> Source: 142-java-functional-programming.md
> Chunk: 2/3
> Included sections: Examples - Example 10: Use Switch Expressions for Concise Multi-way Conditionals | Examples - Example 11: Leverage Sealed Classes and Interfaces for Controlled Hierarchies | Examples - Example 12: Create Type-Safe Wrappers for Domain Types | Examples - Example 13: Explore Stream Gatherers for Custom Stream Operations | Examples - Example 14: Use Optional Idiomatically | Examples - Example 15: Currying and Partial Application | Examples - Example 16: Separate Effects from Pure Logic | Examples - Example 17: Collectors Best Practices | Examples - Example 18: Use Record Patterns in Switch | Examples - Example 19: Compose Async Pipelines Functionally | Examples - Example 20: Leverage Laziness and Infinite Streams | Examples - Example 21: Functional Error Handling | Examples - Example 22: Immutable Collections

### Example 10: Use Switch Expressions for Concise Multi-way Conditionals

Title: Employ Switch Expressions for Safer Conditional Logic
Description: Prefer Switch Expressions (JEP 361, Java 14) over traditional switch statements for assigning the result of a multi-way conditional to a variable. Switch expressions are more concise, less error-prone (e.g., no fall-through by default, compiler checks for exhaustiveness with enums/sealed types). They fit well with functional programming's emphasis on expressions over statements.

**Good example:**

```java
public String getDayTypeWithSwitchExpr(String day) {
    return switch (day) {
        case "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY" -> "Weekday";
        case "SATURDAY", "SUNDAY" -> "Weekend";
        default -> throw new IllegalArgumentException("Invalid day: " + day);
    };
}

// Example with enum for exhaustive switch
enum Day { MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY }

public String getDayCategory(Day day) {
    return switch (day) {
        case MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY -> "Weekday";
        case SATURDAY, SUNDAY -> "Weekend";
        // No default needed if all enum constants are covered
    };
}

```

**Bad example:**

```java
public String getDayTypeLegacy(String day) {
    String type;
    switch (day) {
        case "MONDAY":
        case "TUESDAY":
        case "WEDNESDAY":
        case "THURSDAY":
        case "FRIDAY":
            type = "Weekday";
            break;
        case "SATURDAY":
        case "SUNDAY":
            type = "Weekend";
            break;
        default:
            throw new IllegalArgumentException("Invalid day: " + day);
    }
    return type;
}

```

### Example 11: Leverage Sealed Classes and Interfaces for Controlled Hierarchies

Title: Use Sealed Types for Domain Modeling
Description: Use Sealed Classes and Interfaces (JEP 409, Java 17) to define class/interface hierarchies where all direct subtypes are known, finite, and explicitly listed. This enables more robust domain modeling and allows the compiler to perform exhaustive checks in pattern matching (e.g., with `switch` expressions), eliminating the need for a default case in many scenarios. Particularly useful for creating sum types (algebraic data types) which are common in functional programming.

**Good example:**

```java
// Define a sealed interface for different types of events
public sealed interface Event permits LoginEvent, LogoutEvent, FileUploadEvent {
    long getTimestamp();
}

// Define permitted implementations (often records for immutability)
public record LoginEvent(String userId, long timestamp) implements Event {
    @Override public long getTimestamp() { return timestamp; }
}

public record LogoutEvent(String userId, long timestamp) implements Event {
    @Override public long getTimestamp() { return timestamp; }
}

public record FileUploadEvent(String userId, String fileName, long fileSize, long timestamp) implements Event {
    @Override public long getTimestamp() { return timestamp; }
}

// A function processing the sealed hierarchy can be made exhaustive
public class EventProcessor {
    public String processEvent(Event event) {
        return switch (event) {
            case LoginEvent le -> "User " + le.userId() + " logged in at " + le.getTimestamp();
            case LogoutEvent loe -> "User " + loe.userId() + " logged out at " + loe.getTimestamp();
            case FileUploadEvent fue -> "User " + fue.userId() + " uploaded " + fue.fileName() + " at " + fue.getTimestamp();
            // No default case is necessary if the switch is exhaustive for all permitted types of Event.
        };
    }
}

```

### Example 12: Create Type-Safe Wrappers for Domain Types

Title: Use Strong Types for Domain Modeling
Description: Create type-safe wrappers for domain-specific types instead of using primitive types or general-purpose types like String. These wrapper types enhance type safety by enforcing invariants at compile-time and clearly communicate the intended meaning and constraints of data. This approach from type design thinking improves the functional programming paradigm by making invalid states unrepresentable.

**Good example:**

```java
// Type-safe wrappers for functional programming domains
public record UserId(String value) {
    public UserId {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("UserId cannot be null or empty");
        }
    }
}

public record EmailAddress(String value) {
    public EmailAddress {
        if (value == null || !isValidEmail(value)) {
            throw new IllegalArgumentException("Invalid email format: " + value);
        }
    }

    private static boolean isValidEmail(String email) {
        return email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$");
    }
}

// Usage in functional context
public class UserService {
    public Optional<User> findUser(UserId userId) {
        // Type safety ensures only valid UserIds are passed
        return userRepository.findById(userId.value());
    }

    public List<User> findUsersByEmail(EmailAddress email) {
        // Type safety ensures only valid emails are processed
        return userRepository.findByEmail(email.value());
    }
}

```

**Bad example:**

```java
// Primitive obsession - error prone
public class UserService {
    public Optional<User> findUser(String userId) {
        // No validation, could be null or empty
        return userRepository.findById(userId);
    }

    public List<User> findUsersByEmail(String email) {
        // No validation, could be invalid email format
        return userRepository.findByEmail(email);
    }

    // Easy to make mistakes:
    // findUser(null); // Runtime error
    // findUsersByEmail("invalid-email"); // Invalid data propagated
    // findUser("user@example.com"); // Wrong parameter type confusion
}

```

### Example 13: Explore Stream Gatherers for Custom Stream Operations

Title: Use Stream Gatherers for Advanced Stream Processing
Description: For complex or highly custom stream processing tasks that are not easily achieved with standard terminal operations or collectors, investigate Stream Gatherers (JEP 461). Gatherers (`java.util.stream.Gatherer`) allow defining custom intermediate operations, offering more flexibility and power for sophisticated data transformations within functional pipelines. This feature is aimed at more advanced use cases where reusability and composition of stream operations are key.

**Good example:**

```java
import java.util.List;
import java.util.stream.Stream;
// import java.util.stream.Gatherers; // Assuming this is where predefined gatherers might reside

public class StreamGathererExample {

    // Hypothetical: A custom gatherer that creates sliding windows of elements.
    // The actual implementation of such a gatherer would be more involved.
    // static <T> Gatherer<T, ?, List<T>> windowed(int size) {
    //     // ... implementation details ...
    //     return null; // Placeholder
    // }

    public static void main(String[] args) {
        // List<List<Integer>> windows = Stream.of(1, 2, 3, 4, 5, 6, 7)
        //        .gather(windowed(3)) // Using a hypothetical custom 'windowed' gatherer
        //        .toList();
        //
        // // Expected output might be: [[1, 2, 3], [2, 3, 4], [3, 4, 5], [4, 5, 6], [5, 6, 7]]
        // System.out.println(windows);

        System.out.println("Stream Gatherers are a new feature. Refer to official Java documentation for concrete examples and API details as they become available.");
    }
}

// Rule of Thumb:
// Before implementing very complex custom collectors or resorting to imperative loops for intricate stream transformations,
// evaluate if a Stream Gatherer could offer a more declarative, reusable, and composable solution.
// This is for advanced stream users looking to build sophisticated data processing pipelines.

```

### Example 14: Use Optional Idiomatically

Title: Prefer map/flatMap/filter over isPresent/get
Description: Prefer `map`, `flatMap`, `filter`, `orElseGet`, and `orElseThrow` to express transformations and defaults declaratively. Avoid `isPresent()` + `get()` imperative patterns. Use `Optional` for return types to signal absence; avoid using it for fields and method parameters in most cases.

**Good example:**

```java
import java.util.Optional;

public class OptionalExamples {
    record User(String id, String email) {}

    public static Optional<String> normalizedEmail(Optional<User> maybeUser) {
        return maybeUser
                .map(User::email)
                .filter(email -> !email.isBlank())
                .map(String::trim)
                .map(String::toLowerCase);
    }

    public static String emailOrThrow(Optional<User> maybeUser) {
        return maybeUser
                .map(User::email)
                .filter(e -> !e.isBlank())
                .orElseThrow(() -> new IllegalStateException("Missing email"));
    }

    public static String emailOrDefault(Optional<User> maybeUser) {
        return maybeUser
                .map(User::email)
                .filter(e -> !e.isBlank())
                .orElseGet(() -> "unknown@example.com");
    }
}

```

### Example 15: Currying and Partial Application

Title: Build specialized functions from generic ones
Description: Use higher-order helpers to transform `BiFunction` into chains of `Function` (currying) or to bind the first argument (partial application). This enables composition and reuse without mutable state.

**Good example:**

```java
import java.util.function.BiFunction;
import java.util.function.Function;

public class CurryingExamples {
    public static <A, B, R> Function<A, Function<B, R>> curry(BiFunction<A, B, R> bi) {
        return a -> b -> bi.apply(a, b);
    }

    public static <A, B, R> Function<B, R> partial(BiFunction<A, B, R> bi, A a) {
        return b -> bi.apply(a, b);
    }

    public static void main(String[] args) {
        BiFunction<Integer, Integer, Integer> add = Integer::sum;

        Function<Integer, Function<Integer, Integer>> curriedAdd = curry(add);
        int six = curriedAdd.apply(1).apply(5);

        Function<Integer, Integer> addTen = partial(add, 10);
        int thirteen = addTen.apply(3);

        System.out.println(six + ", " + thirteen);
    }
}

```

### Example 16: Separate Effects from Pure Logic

Title: Model I/O as Suppliers and keep cores pure
Description: Isolate side effects at the edges. Pass `Supplier`/`Function` for effectful operations and keep transformation pipelines pure. This improves testability and reasoning.

**Good example:**

```java
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class EffectBoundaries {
    record Order(String id, double amount) {}

    // Pure transformation
    public static List<String> highValueOrderIds(List<Order> orders, double min) {
        return orders.stream()
                .filter(o -> o.amount() >= min)
                .map(Order::id)
                .collect(Collectors.toUnmodifiableList());
    }

    // Effect boundary
    public static List<String> fetchAndFilter(Supplier<List<Order>> fetchOrders, double min) {
        List<Order> orders = fetchOrders.get(); // side-effect here only
        return highValueOrderIds(orders, min);  // pure
    }
}

```

### Example 17: Collectors Best Practices

Title: Use downstream, merging, and unmodifiable collectors
Description: Prefer `toUnmodifiable*` or `collectingAndThen(..., List::copyOf)` for immutability. With `toMap`, always provide a merge function when keys may collide. Use downstream collectors like `mapping`, `flatMapping`, `filtering`, and `teeing` to express logic declaratively.

**Good example:**

```java
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CollectorPatterns {
    public static Map<String, Long> wordFrequencies(List<String> words) {
        return words.stream()
                .collect(Collectors.toMap(
                        w -> w,
                        w -> 1L,
                        Long::sum,
                        LinkedHashMap::new)); // stable order
    }

    public static Map<Character, List<String>> groupedByInitial(List<String> names) {
        return names.stream()
                .collect(Collectors.groupingBy(
                        s -> s.charAt(0),
                        Collectors.collectingAndThen(
                                Collectors.mapping(String::toUpperCase, Collectors.toList()),
                                List::copyOf)));
    }

    public static double averageOfDistinct(List<Integer> numbers) {
        return numbers.stream()
                .distinct()
                .collect(Collectors.teeing(
                        Collectors.summingDouble(Integer::doubleValue),
                        Collectors.counting(),
                        (sum, count) -> count == 0 ? 0.0 : sum / count));
    }
}

```

### Example 18: Use Record Patterns in Switch

Title: Deconstruct records directly in pattern switches
Description: Record patterns (Java 21) allow deconstruction directly in `switch`, improving expressiveness and type safety. Combine with sealed hierarchies for exhaustiveness.

**Good example:**

```java
sealed interface Shape2 permits Circle2, Rect2 {}
record Circle2(double radius) implements Shape2 {}
record Rect2(double width, double height) implements Shape2 {}

public class RecordPatternDemo {
    public static double perimeter(Shape2 s) {
        return switch (s) {
            case Circle2(double r) -> 2 * Math.PI * r;
            case Rect2(double w, double h) -> 2 * (w + h);
        };
    }
}

```

### Example 19: Compose Async Pipelines Functionally

Title: Prefer thenApply/thenCompose and allOf
Description: Use `CompletableFuture` combinators to build non-blocking, declarative pipelines. Avoid premature `join()`/`get()` in the middle of the flow; keep blocking at the outer boundary if needed.

**Good example:**

```java
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class AsyncComposition {
    static final ExecutorService pool = Executors.newFixedThreadPool(4);

    public static CompletableFuture<Integer> fetchPrice(String sku) {
        return CompletableFuture.supplyAsync(() -> sku.length() * 10, pool);
    }

    public static CompletableFuture<Double> applyDiscount(CompletableFuture<Integer> price, double discount) {
        return price.thenApply(p -> p * (1 - discount));
    }

    public static CompletableFuture<List<Double>> pricesForSkus(List<String> skus, double discount) {
        List<CompletableFuture<Double>> futures = skus.stream()
                .map(AsyncComposition::fetchPrice)
                .map(p -> applyDiscount(p, discount))
                .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .thenApply(v -> futures.stream().map(CompletableFuture::join).collect(Collectors.toList()));
    }
}

```

### Example 20: Leverage Laziness and Infinite Streams

Title: Generate values on-demand with iterate/generate
Description: Streams are lazy; nothing runs until a terminal operation. Model infinite sequences with `iterate`/`generate` and bound them with `limit`/`takeWhile`. Keep operations stateless and side-effect free.

**Good example:**

```java
import java.util.stream.Stream;

public class LazyStreams {
    public static Stream<Long> fibonacci() {
        return Stream.iterate(new long[]{0, 1}, p -> new long[]{p[1], p[0] + p[1]})
                .map(p -> p[0]);
    }

    public static void main(String[] args) {
        System.out.println(fibonacci().limit(10).toList());
        System.out.println(Stream.generate(Math::random).limit(3).toList());
    }
}

```

### Example 21: Functional Error Handling

Title: Use Either for Value or Error
Description: Model computations that may fail using an Either type, where Left represents error and Right represents success. This allows functional composition without throwing exceptions, improving flow and testability.

**Good example:**

```java
sealed interface Either<L, R> permits Left, Right {}
record Left<L, R>(L value) implements Either<L, R> {}
record Right<L, R>(R value) implements Either<L, R> {}

public class ErrorHandling {
    public static Either<String, Integer> safeDivide(int a, int b) {
        return (b == 0) ? new Left<>("Division by zero") : new Right<>(a / b);
    }

    public static Either<String, Integer> divideAndAdd(int a, int b, int add) {
        return safeDivide(a, b).flatMap(res -> safeDivide(res, add));
    }

    // Extension method for flatMap (would be in a utility class)
    public static <L, R, T> Either<L, T> flatMap(Either<L, R> either, Function<R, Either<L, T>> mapper) {
        return switch (either) {
            case Left<L, R> left -> new Left<>(left.value());
            case Right<L, R> right -> mapper.apply(right.value());
        };
    }
}

```

### Example 22: Immutable Collections

Title: Use Factory Methods and Unmodifiable Wrappers
Description: Create immutable collections using factory methods like List.of() or Collectors.toUnmodifiableList(). For existing collections, use Collections.unmodifiableList() to prevent modifications.

**Good example:**

```java
import java.util.List;
import java.util.Collections;

public class ImmutableCollections {
    public static List<String> getImmutableList() {
        return List.of("apple", "banana", "cherry"); // Immutable by design
    }

    public static List<String> makeImmutable(List<String> mutable) {
        return Collections.unmodifiableList(mutable); // Wrapper prevents changes
    }
}

```
