# java-ood

> Source: 121-java-object-oriented-design.md
> Chunk: 3/6
> Included sections: Examples - Example 23: Consider a Builder When Faced with Many Constructor Parameters | Examples - Example 24: Enforce the Singleton Property with a Private Constructor or an Enum Type | Examples - Example 25: Prefer Dependency Injection to Hardwiring Resources | Examples - Example 26: Avoid Creating Unnecessary Objects | Examples - Example 27: Classes and Interfaces Best Practices | Examples - Example 28: Minimize the Accessibility of Classes and Members | Examples - Example 29: In Public Classes, Use Accessor Methods, Not Public Fields | Examples - Example 30: Minimize Mutability

### Example 23: Consider a Builder When Faced with Many Constructor Parameters

Title: Use the Builder pattern for classes with multiple optional parameters
Description: The Builder pattern provides a readable alternative to telescoping constructors and is safer than JavaBeans pattern.

**Good example:**

```java
public class NutritionFacts {
    private final int servingSize;
    private final int servings;
    private final int calories;
    private final int fat;
    private final int sodium;
    private final int carbohydrate;

    public static class Builder {
        // Required parameters
        private final int servingSize;
        private final int servings;

        // Optional parameters - initialized to default values
        private int calories = 0;
        private int fat = 0;
        private int sodium = 0;
        private int carbohydrate = 0;

        public Builder(int servingSize, int servings) {
            this.servingSize = servingSize;
            this.servings = servings;
        }

        public Builder calories(int val) { calories = val; return this; }
        public Builder fat(int val) { fat = val; return this; }
        public Builder sodium(int val) { sodium = val; return this; }
        public Builder carbohydrate(int val) { carbohydrate = val; return this; }

        public NutritionFacts build() {
            return new NutritionFacts(this);
        }
    }

    private NutritionFacts(Builder builder) {
        servingSize = builder.servingSize;
        servings = builder.servings;
        calories = builder.calories;
        fat = builder.fat;
        sodium = builder.sodium;
        carbohydrate = builder.carbohydrate;
    }
}

// Usage - readable and flexible
NutritionFacts cocaCola = new NutritionFacts.Builder(240, 8)
    .calories(100)
    .sodium(35)
    .carbohydrate(27)
    .build();
```

**Bad example:**

```java
// Telescoping constructor pattern - hard to read and error-prone
public class NutritionFacts {
    private final int servingSize;
    private final int servings;
    private final int calories;
    private final int fat;
    private final int sodium;
    private final int carbohydrate;

    public NutritionFacts(int servingSize, int servings) {
        this(servingSize, servings, 0);
    }

    public NutritionFacts(int servingSize, int servings, int calories) {
        this(servingSize, servings, calories, 0);
    }

    public NutritionFacts(int servingSize, int servings, int calories, int fat) {
        this(servingSize, servings, calories, fat, 0);
    }

    public NutritionFacts(int servingSize, int servings, int calories, int fat, int sodium) {
        this(servingSize, servings, calories, fat, sodium, 0);
    }

    public NutritionFacts(int servingSize, int servings, int calories, int fat, int sodium, int carbohydrate) {
        this.servingSize = servingSize;
        this.servings = servings;
        this.calories = calories;
        this.fat = fat;
        this.sodium = sodium;
        this.carbohydrate = carbohydrate;
    }
}

// Usage - confusing parameter order, easy to make mistakes
NutritionFacts cocaCola = new NutritionFacts(240, 8, 100, 0, 35, 27);  // What do these numbers mean?
```

### Example 24: Enforce the Singleton Property with a Private Constructor or an Enum Type

Title: Use enum or private constructor with static field for singletons
Description: Enum-based singletons are the best way to implement singletons, providing serialization and reflection safety.

**Good example:**

```java
// Enum singleton - preferred approach
public enum DatabaseConnection {
    INSTANCE;

    public void connect() {
        System.out.println("Connecting to database...");
    }

    public void executeQuery(String query) {
        System.out.println("Executing: " + query);
    }
}

// Alternative: Static field with private constructor
public class Logger {
    private static final Logger INSTANCE = new Logger();

    private Logger() { /* private constructor */ }

    public static Logger getInstance() {
        return INSTANCE;
    }

    public void log(String message) {
        System.out.println("LOG: " + message);
    }
}

// Usage
DatabaseConnection.INSTANCE.connect();
Logger.getInstance().log("Application started");
```

**Bad example:**

```java
// Not thread-safe singleton
public class BadSingleton {
    private static BadSingleton instance;

    private BadSingleton() {}

    public static BadSingleton getInstance() {
        if (instance == null) {  // Race condition possible
            instance = new BadSingleton();
        }
        return instance;
    }
}
```

### Example 25: Prefer Dependency Injection to Hardwiring Resources

Title: Use dependency injection instead of hardcoded dependencies
Description: Classes should not create their dependencies directly but receive them from external sources, improving testability and flexibility.

**Good example:**

```java
public class SpellChecker {
    private final Lexicon dictionary;

    // Dependency injected through constructor
    public SpellChecker(Lexicon dictionary) {
        this.dictionary = Objects.requireNonNull(dictionary);
    }

    public boolean isValid(String word) {
        return dictionary.contains(word);
    }
}

interface Lexicon {
    boolean contains(String word);
}

class EnglishLexicon implements Lexicon {
    public boolean contains(String word) {
        // English dictionary lookup
        return true;
    }
}

// Usage - flexible and testable
Lexicon englishDict = new EnglishLexicon();
SpellChecker checker = new SpellChecker(englishDict);
```

**Bad example:**

```java
// Hardwired dependency - inflexible and hard to test
public class SpellChecker {
    private static final Lexicon dictionary = new EnglishLexicon();  // Hardcoded

    private SpellChecker() {}  // Noninstantiable

    public static boolean isValid(String word) {
        return dictionary.contains(word);
    }
}
```

### Example 26: Avoid Creating Unnecessary Objects

Title: Reuse objects when possible to improve performance
Description: Object creation can be expensive. Reuse immutable objects and avoid creating objects in loops when possible.

**Good example:**

```java
public class DateUtils {
    // Reuse expensive objects
    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public String formatDate(LocalDate date) {
        return FORMATTER.format(date);  // Reuse formatter
    }

    // Use primitives when possible
    public boolean isEven(int number) {
        return number % 2 == 0;  // No object creation
    }
}
```

**Bad example:**

```java
public class DateUtils {
    public String formatDate(LocalDate date) {
        // Creates new formatter every time - expensive
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return formatter.format(date);
    }

    // Unnecessary autoboxing
    public boolean isEven(Integer number) {
        return number % 2 == 0;  // Creates Integer objects
    }
}
```

### Example 27: Classes and Interfaces Best Practices

Title: Design Classes and Interfaces for Maximum Effectiveness
Description: Well-designed classes and interfaces are the foundation of maintainable and robust Java applications. These practices ensure proper encapsulation, inheritance, and interface design.

### Example 28: Minimize the Accessibility of Classes and Members

Title: Use the most restrictive access level that makes sense
Description: Proper encapsulation hides implementation details and allows for easier maintenance and evolution of code.

**Good example:**

```java
public class BankAccount {
    private final String accountNumber;  // Private - implementation detail
    private double balance;              // Private - internal state

    // Package-private for testing
    static final double MINIMUM_BALANCE = 0.0;

    public BankAccount(String accountNumber, double initialBalance) {  // Public - part of API
        this.accountNumber = accountNumber;
        this.balance = initialBalance;
    }

    public double getBalance() {  // Public - part of API
        return balance;
    }

    public void deposit(double amount) {  // Public - part of API
        validateAmount(amount);
        balance += amount;
    }

    private void validateAmount(double amount) {  // Private - implementation detail
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }
}
```

**Bad example:**

```java
public class BankAccount {
    public String accountNumber;  // Should be private
    public double balance;        // Should be private
    public static final double MINIMUM_BALANCE = 0.0;  // Unnecessarily public

    public BankAccount(String accountNumber, double initialBalance) {
        this.accountNumber = accountNumber;
        this.balance = initialBalance;
    }

    public void validateAmount(double amount) {  // Should be private
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }
}
```

### Example 29: In Public Classes, Use Accessor Methods, Not Public Fields

Title: Provide getter and setter methods instead of exposing fields directly
Description: Accessor methods provide flexibility to add validation, logging, or other logic without breaking clients.

**Good example:**

```java
public class Point {
    private double x;
    private double y;

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() { return x; }
    public double getY() { return y; }

    public void setX(double x) {
        // Can add validation or other logic
        if (Double.isNaN(x)) {
            throw new IllegalArgumentException("x cannot be NaN");
        }
        this.x = x;
    }

    public void setY(double y) {
        if (Double.isNaN(y)) {
            throw new IllegalArgumentException("y cannot be NaN");
        }
        this.y = y;
    }
}
```

**Bad example:**

```java
public class Point {
    public double x;  // Direct field access - no validation possible
    public double y;  // Cannot add logic later without breaking clients

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }
}
```

### Example 30: Minimize Mutability

Title: Make classes immutable when possible
Description: Immutable classes are simpler, safer, and can be freely shared. They are inherently thread-safe and have no temporal coupling.

**Good example:**

```java
public final class Complex {
    private final double real;
    private final double imaginary;

    public Complex(double real, double imaginary) {
        this.real = real;
        this.imaginary = imaginary;
    }

    public double realPart() { return real; }
    public double imaginaryPart() { return imaginary; }

    // Operations return new instances instead of modifying
    public Complex plus(Complex c) {
        return new Complex(real + c.real, imaginary + c.imaginary);
    }

    public Complex minus(Complex c) {
        return new Complex(real - c.real, imaginary - c.imaginary);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Complex)) return false;
        Complex c = (Complex) o;
        return Double.compare(c.real, real) == 0 &&
               Double.compare(c.imaginary, imaginary) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(real, imaginary);
    }
}
```

**Bad example:**

```java
public class Complex {
    private double real;      // Mutable fields
    private double imaginary; // Mutable fields

    public Complex(double real, double imaginary) {
        this.real = real;
        this.imaginary = imaginary;
    }

    public double getRealPart() { return real; }
    public double getImaginaryPart() { return imaginary; }

    // Mutating operations - not thread-safe, harder to reason about
    public void plus(Complex c) {
        this.real += c.real;
        this.imaginary += c.imaginary;
    }

    public void setReal(double real) { this.real = real; }
    public void setImaginary(double imaginary) { this.imaginary = imaginary; }
}
```
