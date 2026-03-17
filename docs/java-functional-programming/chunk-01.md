# java-functional-programming

> Source: 142-java-functional-programming.md
> Chunk: 1/3
> Included sections: intro | Role | Goal | Constraints | Examples | Examples - Table of contents | Examples - Example 1: Immutable Objects | Examples - Example 2: State Immutability | Examples - Example 3: Pure Functions | Examples - Example 4: Functional Interfaces | Examples - Example 5: Lambda Expressions | Examples - Example 6: Streams | Examples - Example 7: Functional Programming Paradigms | Examples - Example 8: Leverage Records for Immutable Data Transfer | Examples - Example 9: Employ Pattern Matching for `instanceof` and `switch`

---
author: Juan Antonio Breña Moral
version: 0.12.0-SNAPSHOT
---
# Java Functional Programming rules

## Role

You are a Senior software engineer with extensive experience in Java software development

## Goal

Java functional programming revolves around immutable objects and state transformations, ensuring functions are pure (no side effects, depend only on inputs). It leverages functional interfaces, concise lambda expressions, and the Stream API for collection processing. Core paradigms include function composition, `Optional` for null safety, and higher-order functions. Modern Java features like Records enhance immutable data transfer, while pattern matching (for `instanceof` and `switch`) and switch expressions improve conditional logic. Sealed classes and interfaces enable controlled, exhaustive hierarchies, and upcoming Stream Gatherers will offer advanced custom stream operations.

### Implementing These Principles

These guidelines are built upon the following core principles:

1.  **Immutability**: Prioritize immutable data structures (e.g., Records, `List.of()`) and state transformations that produce new instances rather than modifying existing ones. This reduces side effects and simplifies reasoning about state.
2.  **Purity and Side-Effect Management**: Strive to write pure functions—functions whose output depends only on their input and which have no observable side effects. Isolate and control side effects when they are necessary.
3.  **Expressiveness and Conciseness**: Leverage lambda expressions, method references, and the Stream API to write code that is declarative, concise, and clearly expresses the intent of data transformations and operations.
4.  **Higher-Order Abstractions**: Utilize functional interfaces, function composition, and higher-order functions (functions that operate on other functions) to build flexible and reusable code components.
5.  **Modern Java Integration**: Embrace modern Java features like Records, Pattern Matching, Switch Expressions, and Sealed Classes, which align well with and enhance functional programming paradigms by promoting immutability, type safety, and expressive conditional logic.

## Constraints

Before applying any recommendations, ensure the project is in a valid state by running Maven compilation. Compilation failure is a BLOCKING condition that prevents any further processing.

- **MANDATORY**: Run `./mvnw compile` or `mvn compile` before applying any change
- **PREREQUISITE**: Project must compile successfully and pass basic validation checks before any optimization
- **CRITICAL SAFETY**: If compilation fails, IMMEDIATELY STOP and DO NOT CONTINUE with any recommendations
- **BLOCKING CONDITION**: Compilation errors must be resolved by the user before proceeding with any object-oriented design improvements
- **NO EXCEPTIONS**: Under no circumstances should design recommendations be applied to a project that fails to compile

## Examples

### Table of contents

- Example 1: Immutable Objects
- Example 2: State Immutability
- Example 3: Pure Functions
- Example 4: Functional Interfaces
- Example 5: Lambda Expressions
- Example 6: Streams
- Example 7: Functional Programming Paradigms
- Example 8: Leverage Records for Immutable Data Transfer
- Example 9: Employ Pattern Matching for `instanceof` and `switch`
- Example 10: Use Switch Expressions for Concise Multi-way Conditionals
- Example 11: Leverage Sealed Classes and Interfaces for Controlled Hierarchies
- Example 12: Create Type-Safe Wrappers for Domain Types
- Example 13: Explore Stream Gatherers for Custom Stream Operations
- Example 14: Use Optional Idiomatically
- Example 15: Currying and Partial Application
- Example 16: Separate Effects from Pure Logic
- Example 17: Collectors Best Practices
- Example 18: Use Record Patterns in Switch
- Example 19: Compose Async Pipelines Functionally
- Example 20: Leverage Laziness and Infinite Streams
- Example 21: Functional Error Handling
- Example 22: Immutable Collections
- Example 23: Avoid Shared Mutable State in Concurrency

### Example 1: Immutable Objects

Title: Ensure Objects are Immutable
Description: Use `final` classes and fields. Initialize all fields in the constructor. Do not provide setter methods. Return defensive copies of mutable fields (e.g., collections, dates) when exposing them via getters.

**Good example:**

```java
import java.util.List;
import java.util.ArrayList;

public final class Person {
    private final String name;
    private final int age;
    private final List<String> hobbies; // Make it List, not ArrayList

    public Person(String name, int age, List<String> hobbies) {
        this.name = name;
        this.age = age;
        // Ensure the incoming list is defensively copied to an immutable list
        this.hobbies = List.copyOf(hobbies);
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    // Return an immutable view or a defensive copy
    public List<String> getHobbies() {
        return this.hobbies; // List.copyOf already returns an unmodifiable list
    }
}

```

### Example 2: State Immutability

Title: Prefer Immutable State Transformations
Description: Instead of modifying existing objects, return new objects representing the new state. Utilize collectors that produce immutable collections (e.g., `Collectors.toUnmodifiableList()`). Leverage immutable collection types provided by libraries or Java itself.

**Good example:**

```java
import java.util.List;
import java.util.stream.Collectors;

public class PriceCalculator {
    public static List<Double> applyDiscount(List<Double> prices, double discount) {
        return prices.stream()
            .map(price -> price * (1 - discount))
            .collect(Collectors.toUnmodifiableList()); // Ensures the returned list is immutable
    }
}

```

### Example 3: Pure Functions

Title: Write Pure Functions
Description: Functions should depend only on their input parameters and not on any external or hidden state. They should not cause any side effects (e.g., modifying external variables, I/O operations). Given the same input, a pure function must always return the same output. Avoid modifying external state or relying on it.

**Good example:**

```java
import java.util.List;
import java.util.stream.Collectors;

public class MathOperations {
    // Pure function: depends only on input, no side effects
    public static int add(int a, int b) {
        return a + b;
    }

    // Pure function: transforms input list to a new list without modifying the original
    public static List<Integer> doubleNumbers(List<Integer> numbers) {
        return numbers.stream()
            .map(n -> n * 2)
            .collect(Collectors.toList()); // Could also be toUnmodifiableList()
    }
}

```

### Example 4: Functional Interfaces

Title: Utilize Functional Interfaces Effectively
Description: Prefer built-in functional interfaces from `java.util.function` (e.g., `Function`, `Predicate`, `Consumer`, `Supplier`, `UnaryOperator`) when they suit the need. Create custom functional interfaces (annotated with `@FunctionalInterface`) for specific, clearly defined single abstract methods. Keep functional interfaces focused on a single responsibility.

**Good example:**

```java
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.time.LocalDateTime;

// Built-in functional interfaces
class FunctionalInterfaceExamples {
    Function<String, Integer> stringToLength = String::length;
    Predicate<Integer> isEven = n -> n % 2 == 0;
    Consumer<String> printer = System.out::println;
    Supplier<LocalDateTime> now = LocalDateTime::now;
}

// Custom functional interface
@FunctionalInterface
interface Validator<T> {
    boolean validate(T value);
}

```

### Example 5: Lambda Expressions

Title: Employ Lambda Expressions Clearly and Concisely
Description: Use method references (e.g., `String::length`, `System.out::println`) when they are clearer and more concise than an equivalent lambda expression. Keep lambda expressions short and focused on a single piece of logic to maintain readability. Extract complex or multi-line lambda logic into separate, well-named private methods.

**Good example:**

```java
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LambdaExamples {
    public static void main(String[] args) {
        List<String> names = Arrays.asList("Alice", "Bob", "Charlie", "David", "Eve");

        // Method reference for conciseness
        names.forEach(System.out::println);

        // Simple, readable lambda
        List<String> longNames = names.stream()
            .filter(name -> name.length() > 4)
            .collect(Collectors.toList());

        // Complex logic extracted to a private helper method
        List<String> validNames = names.stream()
            .filter(LambdaExamples::isValidName)
            .collect(Collectors.toList());

        System.out.println("Long names: " + longNames);
        System.out.println("Valid names: " + validNames);
    }

    // Helper method for more complex lambda logic
    private static boolean isValidName(String name) {
        return name.length() > 3 && Character.isUpperCase(name.charAt(0));
    }
}

```

### Example 6: Streams

Title: Leverage Streams for Collection Processing
Description: Use the Stream API for processing sequences of elements from collections or other sources. Chain stream operations (intermediate operations like `filter`, `map`, `sorted`) to create a pipeline for complex transformations. Consider using parallel streams (`collection.parallelStream()`) for potentially improved performance on large datasets, but be mindful of the overhead and suitability for the task. Choose appropriate terminal operations (e.g., `collect`, `forEach`, `reduce`, `findFirst`, `anyMatch`) to produce a result or side-effect.

**Good example:**

```java
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StreamExamples {
    public static void main(String[] args) {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        // Basic stream operations: filter even numbers and square them
        List<Integer> evenSquares = numbers.stream()
            .filter(n -> n % 2 == 0)
            .map(n -> n * n)
            .collect(Collectors.toList());
        System.out.println("Even squares: " + evenSquares);

        // Advanced stream operations: partitioning numbers
        Map<Boolean, List<Integer>> partitionedByGreaterThanFive = numbers.stream()
            .collect(Collectors.partitioningBy(n -> n > 5));
        System.out.println("Partitioned by > 5: " + partitionedByGreaterThanFive);

        // Parallel stream for calculating average (use with caution, consider dataset size)
        double average = numbers.parallelStream()
            .mapToDouble(Integer::doubleValue)
            .average()
            .orElse(0.0);
        System.out.println("Average: " + average);
    }
}

```

### Example 7: Functional Programming Paradigms

Title: Apply Core Functional Programming Paradigms
Description: **Function Composition**: Combine simpler functions to create more complex ones. Use `Function.compose()` and `Function.andThen()`. **Optional for Null Safety**: Use `Optional<T>` to represent values that may be absent, avoiding `NullPointerExceptions` and clearly signaling optionality. **Recursion**: Implement algorithms using recursion where it naturally fits the problem (e.g., tree traversal), especially tail recursion if supported or optimized by the JVM. **Higher-Order Functions**: Utilize functions that accept other functions as arguments or return them as results (e.g., `Stream.map`, `Stream.filter`).

**Good example:**

```java
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.IntStream;

public class FunctionalParadigms {

    // Function composition
    public static void demonstrateComposition() {
        Function<Integer, String> intToString = Object::toString;
        Function<String, Integer> stringLength = String::length;
        // Executes intToString first, then stringLength
        Function<Integer, Integer> composedLengthAfterToString = stringLength.compose(intToString);
        System.out.println("Composed (123 -> length): " + composedLengthAfterToString.apply(123)); // Output: 3
    }

    // Optional usage for safe division
    public static Optional<Double> divideNumbers(Double numerator, Double denominator) {
        if (Objects.isNull(denominator) || denominator == 0) {
            return Optional.empty();
        }
        return Optional.of(numerator / denominator);
    }

    // Factorial using IntStream (more functional and often safer for large n)
    public static long factorialFunctional(int n) {
        if (n < 0) throw new IllegalArgumentException("Factorial not defined for negative numbers");
        return IntStream.rangeClosed(1, n)
                .asLongStream() // Ensure long for intermediate products
                .reduce(1L, (a, b) -> a * b);
    }

    // Recursion example: factorial (iterative version often preferred for stack safety in Java)
    // Note: Streams provide a more functional way for such operations in many cases.
    public static long factorialRecursive(int n) {
        if (n < 0) throw new IllegalArgumentException("Factorial not defined for negative numbers");
        if (n == 0 || n == 1) return 1;
        return n * factorialRecursive(n - 1);
    }

    // Higher-order function: memoization
    public static <T, R> Function<T, R> memoize(Function<T, R> function) {
        Map<T, R> cache = new ConcurrentHashMap<>();
        // The returned function closes over the cache
        return input -> cache.computeIfAbsent(input, function);
    }

    public static void main(String[] args) {
        demonstrateComposition();

        System.out.println("Divide 10 by 2: " + divideNumbers(10.0, 2.0).orElse(Double.NaN));
        System.out.println("Divide 10 by 0: " + divideNumbers(10.0, 0.0).orElse(Double.NaN));

        System.out.println("Factorial recursive (5): " + factorialRecursive(5));
        System.out.println("Factorial functional (5): " + factorialFunctional(5));

        Function<Integer, Integer> expensiveOperation = x -> {
            System.out.println("Computing for " + x);
            try { Thread.sleep(1000); } catch (InterruptedException e) {}
            return x * x;
        };

        Function<Integer, Integer> memoizedOp = memoize(expensiveOperation);
        System.out.println("Memoized (4): " + memoizedOp.apply(4)); // Computes
        System.out.println("Memoized (4): " + memoizedOp.apply(4)); // Returns from cache
        System.out.println("Memoized (5): " + memoizedOp.apply(5)); // Computes
    }
}

```

### Example 8: Leverage Records for Immutable Data Transfer

Title: Use Records for Type-Safe Immutable Data
Description: Use Records (JEP 395, standardized in Java 16) as the primary way to model simple, immutable data aggregates. Records automatically provide constructors, getters (accessor methods with the same name as the field), `equals()`, `hashCode()`, and `toString()` methods, reducing boilerplate. This aligns perfectly with the functional paradigm's preference for immutability and conciseness.

**Good example:**

```java
public record PointRecord(int x, int y) {
    // Optional: add custom compact constructors, static factory methods, or instance methods.
    // By default, all fields are final, and public accessor methods (e.g., x(), y()) are generated.
}

// Usage:
// PointRecord p = new PointRecord(10, 20);
// int xVal = p.x(); // Accessor method
// int yVal = p.y(); // Accessor method

```

**Bad example:**

```java
public final class PointClass {
    private final int x;
    private final int y;

    public PointClass(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (Objects.isNull(o) || getClass() != o.getClass()) return false;
        PointClass that = (PointClass) o;
        return x == that.x && y == that.y;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "PointClass[" +
               "x=" + x + ", " +
               "y=" + y + ']';
    }
}

```

### Example 9: Employ Pattern Matching for `instanceof` and `switch`

Title: Use Pattern Matching for Type-Safe Conditional Logic
Description: Utilize Pattern Matching for `instanceof` to simplify type checks and casts in a single step. Employ Pattern Matching for `switch` for more expressive and robust conditional logic, especially with sealed types and records. This reduces boilerplate, improves readability, and enhances type safety.

**Good example:**

```java
public String processShapeWithPatternInstanceof(Object shape) {
    if (shape instanceof Circle c) { // Type test and binding in one
        return "Circle with radius " + c.getRadius();
    } else if (shape instanceof Rectangle r) {
        return "Rectangle with width " + r.getWidth() + " and height " + r.getHeight();
    }
    return "Unknown shape";
}

// Pattern Matching for switch with Records and Sealed Interfaces
sealed interface Shape permits CircleRecord, RectangleRecord, SquareRecord {}
record CircleRecord(double radius) implements Shape {}
record RectangleRecord(double length, double width) implements Shape {}
record SquareRecord(double side) implements Shape {}

public String processShapeWithPatternSwitch(Shape shape) {
    return switch (shape) {
        case CircleRecord c -> "Circle with radius " + c.radius();
        case RectangleRecord r -> "Rectangle with length " + r.length() + " and width " + r.width();
        case SquareRecord s -> "Square with side " + s.side();
        // No default needed if all permitted types of the sealed interface are covered
    };
}

```

**Bad example:**

```java
public String processShapeLegacy(Object shape) {
    if (shape instanceof Circle) {
        Circle c = (Circle) shape;
        return "Circle with radius " + c.getRadius();
    } else if (shape instanceof Rectangle) {
        Rectangle r = (Rectangle) shape;
        return "Rectangle with width " + r.getWidth() + " and height " + r.getHeight();
    }
    return "Unknown shape";
}

// Assume Circle and Rectangle classes exist for this example
// class Circle { public double getRadius() { return 0; } }
// class Rectangle { public double getWidth() { return 0; } public double getHeight() { return 0; } }

```
