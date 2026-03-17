# java-ood

> Source: 121-java-object-oriented-design.md
> Chunk: 4/6
> Included sections: Examples - Example 31: Favor Composition Over Inheritance | Examples - Example 32: Design and Document for Inheritance or Else Prohibit It | Examples - Example 33: Enums and Annotations | Examples - Example 34: Use Enums Instead of Int Constants | Examples - Example 35: Use Instance Fields Instead of Ordinals | Examples - Example 36: Use EnumSet Instead of Bit Fields | Examples - Example 37: Use EnumMap Instead of Ordinal Indexing | Examples - Example 38: Consistently Use the Override Annotation | Examples - Example 39: Method Design | Examples - Example 40: Check Parameters for Validity

### Example 31: Favor Composition Over Inheritance

Title: Use composition instead of inheritance when you want to reuse code
Description: Composition is more flexible than inheritance and avoids the fragility of inheritance hierarchies.

**Good example:**

```java
// Using composition
public class InstrumentedSet<E> {
    private final Set<E> s;
    private int addCount = 0;

    public InstrumentedSet(Set<E> s) {
        this.s = s;
    }

    public boolean add(E e) {
        addCount++;
        return s.add(e);
    }

    public boolean addAll(Collection<? extends E> c) {
        addCount += c.size();
        return s.addAll(c);
    }

    public int getAddCount() {
        return addCount;
    }

    // Delegate other methods to the wrapped set
    public int size() { return s.size(); }
    public boolean isEmpty() { return s.isEmpty(); }
    public boolean contains(Object o) { return s.contains(o); }
    // ... other delegating methods
}
```

**Bad example:**

```java
// Using inheritance - fragile and error-prone
public class InstrumentedHashSet<E> extends HashSet<E> {
    private int addCount = 0;

    @Override
    public boolean add(E e) {
        addCount++;
        return super.add(e);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        addCount += c.size();
        return super.addAll(c);  // This calls add() internally, double-counting!
    }

    public int getAddCount() {
        return addCount;
    }
}
```

### Example 32: Design and Document for Inheritance or Else Prohibit It

Title: Either design classes specifically for inheritance or make them final
Description: Classes not designed for inheritance can break when subclassed. Document self-use patterns or prohibit inheritance.

**Good example:**

```java
// Designed for inheritance with proper documentation
public abstract class AbstractProcessor {

    /**
     * Processes the given data. This implementation calls {@link #validate(String)}
     * followed by {@link #transform(String)}. Subclasses may override this method
     * to provide different processing logic.
     *
     * @param data the data to process
     * @return the processed result
     * @throws IllegalArgumentException if data is invalid
     */
    public String process(String data) {
        validate(data);
        return transform(data);
    }

    /**
     * Validates the input data. The default implementation checks for null.
     * Subclasses may override to provide additional validation.
     */
    protected void validate(String data) {
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }
    }

    /**
     * Transforms the validated data. Subclasses must implement this method.
     */
    protected abstract String transform(String data);
}

// Or prohibit inheritance
public final class UtilityClass {
    private UtilityClass() { /* prevent instantiation */ }

    public static String formatName(String firstName, String lastName) {
        return firstName + " " + lastName;
    }
}
```

**Bad example:**

```java
// Not designed for inheritance but not prohibited
public class DataProcessor {
    public String process(String data) {
        // Complex logic that might break if overridden
        String validated = validate(data);
        String transformed = transform(validated);
        return finalize(transformed);
    }

    private String validate(String data) { /* ... */ return data; }
    private String transform(String data) { /* ... */ return data; }
    private String finalize(String data) { /* ... */ return data; }
}
```

### Example 33: Enums and Annotations

Title: Effective Use of Enums and Annotations
Description: Enums and annotations are powerful Java features that, when used correctly, can make code more readable, type-safe, and maintainable.

### Example 34: Use Enums Instead of Int Constants

Title: Replace int constants with type-safe enums
Description: Enums provide type safety, namespace protection, and additional functionality that int constants cannot offer.

**Good example:**

```java
public enum Planet {
    MERCURY(3.302e+23, 2.439e6),
    VENUS  (4.869e+24, 6.052e6),
    EARTH  (5.975e+24, 6.378e6),
    MARS   (6.419e+23, 3.393e6);

    private final double mass;           // In kilograms
    private final double radius;         // In meters
    private final double surfaceGravity; // In m / s^2

    // Universal gravitational constant in m^3 / kg s^2
    private static final double G = 6.67300E-11;

    Planet(double mass, double radius) {
        this.mass = mass;
        this.radius = radius;
        surfaceGravity = G * mass / (radius * radius);
    }

    public double mass()           { return mass; }
    public double radius()         { return radius; }
    public double surfaceGravity() { return surfaceGravity; }

    public double surfaceWeight(double mass) {
        return mass * surfaceGravity;  // F = ma
    }
}

// Usage
double earthWeight = 175;
double mass = earthWeight / Planet.EARTH.surfaceGravity();
for (Planet p : Planet.values()) {
    System.out.printf("Weight on %s is %f%n", p, p.surfaceWeight(mass));
}
```

**Bad example:**

```java
// Int constants - not type-safe, no namespace
public class Planet {
    public static final int MERCURY = 0;
    public static final int VENUS   = 1;
    public static final int EARTH   = 2;
    public static final int MARS    = 3;

    // Separate arrays for data - error-prone
    private static final double[] MASS = {3.302e+23, 4.869e+24, 5.975e+24, 6.419e+23};
    private static final double[] RADIUS = {2.439e6, 6.052e6, 6.378e6, 3.393e6};

    public static double surfaceWeight(int planet, double mass) {
        // No compile-time checking - could pass any int
        if (planet < 0 || planet >= MASS.length) {
            throw new IllegalArgumentException("Invalid planet: " + planet);
        }
        // Complex calculations with array indexing
        return mass * (6.67300E-11 * MASS[planet] / (RADIUS[planet] * RADIUS[planet]));
    }
}
```

### Example 35: Use Instance Fields Instead of Ordinals

Title: Don't derive values from enum ordinals; use instance fields
Description: Ordinal values can change when enum constants are reordered, making code fragile.

**Good example:**

```java
public enum Ensemble {
    SOLO(1), DUET(2), TRIO(3), QUARTET(4), QUINTET(5),
    SEXTET(6), SEPTET(7), OCTET(8), DOUBLE_QUARTET(8),
    NONET(9), DECTET(10), TRIPLE_QUARTET(12);

    private final int numberOfMusicians;

    Ensemble(int size) {
        this.numberOfMusicians = size;
    }

    public int numberOfMusicians() {
        return numberOfMusicians;
    }
}
```

**Bad example:**

```java
public enum Ensemble {
    SOLO, DUET, TRIO, QUARTET, QUINTET,
    SEXTET, SEPTET, OCTET, NONET, DECTET;

    public int numberOfMusicians() {
        return ordinal() + 1;  // Fragile - breaks if order changes
    }
}
```

### Example 36: Use EnumSet Instead of Bit Fields

Title: Replace bit field enums with EnumSet for better type safety and performance
Description: EnumSet provides all the benefits of bit fields with better readability and type safety.

**Good example:**

```java
public class Text {
    public enum Style { BOLD, ITALIC, UNDERLINE, STRIKETHROUGH }

    // EnumSet - type-safe and efficient
    public void applyStyles(Set<Style> styles) {
        System.out.printf("Applying styles %s to text%n", styles);
        // Implementation here
    }
}

// Usage
text.applyStyles(EnumSet.of(Style.BOLD, Style.ITALIC));
```

**Bad example:**

```java
public class Text {
    public static final int STYLE_BOLD          = 1 << 0;  // 1
    public static final int STYLE_ITALIC        = 1 << 1;  // 2
    public static final int STYLE_UNDERLINE     = 1 << 2;  // 4
    public static final int STYLE_STRIKETHROUGH = 1 << 3;  // 8

    // Bit field - not type-safe
    public void applyStyles(int styles) {
        System.out.printf("Applying styles %s to text%n", styles);
        // Implementation here
    }
}

// Usage - error-prone, no type safety
text.applyStyles(STYLE_BOLD | STYLE_ITALIC);
```

### Example 37: Use EnumMap Instead of Ordinal Indexing

Title: Use EnumMap for enum-keyed data instead of ordinal indexing
Description: EnumMap is specifically designed for enum keys and provides better performance and type safety.

**Good example:**

```java
public enum Phase {
    SOLID, LIQUID, GAS;

    public enum Transition {
        MELT(SOLID, LIQUID), FREEZE(LIQUID, SOLID),
        BOIL(LIQUID, GAS), CONDENSE(GAS, LIQUID),
        SUBLIME(SOLID, GAS), DEPOSIT(GAS, SOLID);

        private final Phase from;
        private final Phase to;

        Transition(Phase from, Phase to) {
            this.from = from;
            this.to = to;
        }

        // Initialize the phase transition map
        private static final Map<Phase, Map<Phase, Transition>> m =
            Stream.of(values()).collect(groupingBy(t -> t.from,
                () -> new EnumMap<>(Phase.class),
                toMap(t -> t.to, t -> t, (x, y) -> y, () -> new EnumMap<>(Phase.class))));

        public static Transition from(Phase from, Phase to) {
            return m.get(from).get(to);
        }
    }
}
```

**Bad example:**

```java
public enum Phase {
    SOLID, LIQUID, GAS;

    public enum Transition {
        MELT, FREEZE, BOIL, CONDENSE, SUBLIME, DEPOSIT;

        // Ordinal-based array - fragile and error-prone
        private static final Transition[][] TRANSITIONS = {
            { null,    MELT,     SUBLIME  },  // SOLID
            { FREEZE,  null,     BOIL     },  // LIQUID
            { DEPOSIT, CONDENSE, null     }   // GAS
        };

        public static Transition from(Phase from, Phase to) {
            return TRANSITIONS[from.ordinal()][to.ordinal()];
        }
    }
}
```

### Example 38: Consistently Use the Override Annotation

Title: Always use @Override when overriding methods
Description: The @Override annotation catches errors at compile time and makes code more readable.

**Good example:**

```java
public class Bigram {
    private final char first;
    private final char second;

    public Bigram(char first, char second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public boolean equals(Object o) {  // Correct signature
        if (!(o instanceof Bigram)) return false;
        Bigram b = (Bigram) o;
        return b.first == first && b.second == second;
    }

    @Override
    public int hashCode() {
        return 31 * first + second;
    }

    @Override
    public String toString() {
        return String.format("(%c, %c)", first, second);
    }
}
```

**Bad example:**

```java
public class Bigram {
    private final char first;
    private final char second;

    public Bigram(char first, char second) {
        this.first = first;
        this.second = second;
    }

    // Missing @Override - typo in method signature won't be caught
    public boolean equals(Bigram b) {  // Wrong signature! Should be equals(Object)
        return b.first == first && b.second == second;
    }

    public int hashCode() {  // Missing @Override
        return 31 * first + second;
    }
}
```

### Example 39: Method Design

Title: Design Methods for Clarity, Safety, and Usability
Description: Well-designed methods are the building blocks of maintainable code. These practices ensure methods are robust, clear, and easy to use correctly.

### Example 40: Check Parameters for Validity

Title: Validate method parameters early and clearly
Description: Fail fast by checking parameters at the beginning of methods. This makes debugging easier and prevents corruption of object state.

**Good example:**

```java
public class MathUtils {
    /**
     * Returns a BigInteger whose value is (this mod m).
     * @param m the modulus, which must be positive
     * @return this mod m
     * @throws ArithmeticException if m <= 0
     */
    public BigInteger mod(BigInteger m) {
        if (m.signum() <= 0) {
            throw new ArithmeticException("Modulus <= 0: " + m);
        }
        // ... do the computation
        return this;
    }

    /**
     * Returns the index of the first occurrence of needle in haystack,
     * or -1 if needle is not contained in haystack.
     * @param haystack the string to search in
     * @param needle the string to search for
     * @throws NullPointerException if haystack or needle is null
     */
    public static int indexOf(String haystack, String needle) {
        Objects.requireNonNull(haystack, "haystack");
        Objects.requireNonNull(needle, "needle");
        // ... do the search
        return haystack.indexOf(needle);
    }
}
```

**Bad example:**

```java
public class MathUtils {
    public BigInteger mod(BigInteger m) {
        // No parameter validation - could cause confusing errors later
        // ... do the computation
        return this;
    }

    public static int indexOf(String haystack, String needle) {
        // No null checks - will throw NullPointerException at some random point
        return haystack.indexOf(needle);
    }
}
```
