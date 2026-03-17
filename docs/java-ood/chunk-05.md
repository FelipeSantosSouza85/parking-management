# java-ood

> Source: 121-java-object-oriented-design.md
> Chunk: 5/6
> Included sections: Examples - Example 41: Make Defensive Copies When Needed | Examples - Example 42: Design Method Signatures Carefully | Examples - Example 43: Return Empty Collections or Arrays, Not Nulls | Examples - Example 44: Return Optionals Judiciously | Examples - Example 45: Exception Handling | Examples - Example 46: Use Exceptions Only for Exceptional Conditions | Examples - Example 47: Use Checked Exceptions for Recoverable Conditions and Runtime Exceptions for Programming Errors | Examples - Example 48: Favor the Use of Standard Exceptions

### Example 41: Make Defensive Copies When Needed

Title: Protect against malicious or accidental modification of mutable parameters
Description: When accepting mutable objects as parameters or returning them, make defensive copies to maintain class invariants.

**Good example:**

```java
public final class Period {
    private final Date start;
    private final Date end;

    /**
     * @param start the beginning of the period
     * @param end the end of the period; must not precede start
     * @throws IllegalArgumentException if start is after end
     * @throws NullPointerException if start or end is null
     */
    public Period(Date start, Date end) {
        this.start = new Date(start.getTime());  // Defensive copy
        this.end = new Date(end.getTime());      // Defensive copy

        if (this.start.compareTo(this.end) > 0) {
            throw new IllegalArgumentException(this.start + " after " + this.end);
        }
    }

    public Date start() {
        return new Date(start.getTime());  // Defensive copy on return
    }

    public Date end() {
        return new Date(end.getTime());    // Defensive copy on return
    }
}
```

**Bad example:**

```java
public final class Period {
    private final Date start;
    private final Date end;

    public Period(Date start, Date end) {
        if (start.compareTo(end) > 0) {
            throw new IllegalArgumentException(start + " after " + end);
        }
        this.start = start;  // No defensive copy - client can modify after construction
        this.end = end;      // No defensive copy - client can modify after construction
    }

    public Date start() {
        return start;  // No defensive copy - client can modify internal state
    }

    public Date end() {
        return end;    // No defensive copy - client can modify internal state
    }
}
```

### Example 42: Design Method Signatures Carefully

Title: Choose method names carefully and avoid long parameter lists
Description: Good method signatures are self-documenting and hard to use incorrectly.

**Good example:**

```java
public class UserService {
    // Clear, descriptive method names
    public User createUser(String username, String email, LocalDate birthDate) {
        // Implementation
        return new User(username, email, birthDate);
    }

    // Use builder pattern for many parameters
    public static class UserBuilder {
        private String username;
        private String email;
        private LocalDate birthDate;
        private String firstName;
        private String lastName;
        private Address address;

        public UserBuilder username(String username) { this.username = username; return this; }
        public UserBuilder email(String email) { this.email = email; return this; }
        public UserBuilder birthDate(LocalDate birthDate) { this.birthDate = birthDate; return this; }
        public UserBuilder firstName(String firstName) { this.firstName = firstName; return this; }
        public UserBuilder lastName(String lastName) { this.lastName = lastName; return this; }
        public UserBuilder address(Address address) { this.address = address; return this; }

        public User build() {
            return new User(this);
        }
    }

    // Use helper classes to group related parameters
    public void updateUserProfile(User user, ProfileUpdate update) {
        // Implementation
    }
}

class ProfileUpdate {
    private final String firstName;
    private final String lastName;
    private final Address address;

    // Constructor and getters
}
```

**Bad example:**

```java
public class UserService {
    // Unclear method name and too many parameters
    public User doUserStuff(String s1, String s2, int d, int m, int y,
                           String s3, String s4, String s5, String s6, String s7) {
        // What do these parameters mean?
        return new User(s1, s2, LocalDate.of(y, m, d));
    }

    // Ambiguous parameter types
    public void updateUser(String username, String data) {
        // What kind of data? How is it formatted?
    }
}
```

### Example 43: Return Empty Collections or Arrays, Not Nulls

Title: Never return null from methods that return collections or arrays
Description: Returning null forces clients to handle null checks and is a common source of bugs.

**Good example:**

```java
public class ShoppingCart {
    private final List<Item> items = new ArrayList<>();

    /**
     * Returns a list of items in the cart.
     * @return the items in the cart (never null, but may be empty)
     */
    public List<Item> getItems() {
        return new ArrayList<>(items);  // Return copy of list, never null
    }

    /**
     * Returns items matching the given category.
     * @param category the category to filter by
     * @return matching items (never null, but may be empty)
     */
    public List<Item> getItemsByCategory(String category) {
        return items.stream()
                   .filter(item -> category.equals(item.getCategory()))
                   .collect(Collectors.toList());  // Returns empty list if no matches
    }

    /**
     * Returns an array of item names.
     * @return array of item names (never null, but may be empty)
     */
    public String[] getItemNames() {
        return items.stream()
                   .map(Item::getName)
                   .toArray(String[]::new);  // Returns empty array if no items
    }
}
```

**Bad example:**

```java
public class ShoppingCart {
    private final List<Item> items = new ArrayList<>();

    public List<Item> getItems() {
        return items.isEmpty() ? null : new ArrayList<>(items);  // Bad: returns null
    }

    public List<Item> getItemsByCategory(String category) {
        List<Item> result = items.stream()
                                .filter(item -> category.equals(item.getCategory()))
                                .collect(Collectors.toList());
        return result.isEmpty() ? null : result;  // Bad: returns null
    }

    public String[] getItemNames() {
        if (items.isEmpty()) {
            return null;  // Bad: returns null instead of empty array
        }
        return items.stream()
                   .map(Item::getName)
                   .toArray(String[]::new);
    }
}
```

### Example 44: Return Optionals Judiciously

Title: Use Optional for methods that may not return a value, but use it carefully
Description: Optional is intended for return types where there might legitimately be no result and the client needs to perform special processing.

**Good example:**

```java
public class UserRepository {
    private final Map<String, User> users = new HashMap<>();

    /**
     * Finds a user by username.
     * @param username the username to search for
     * @return an Optional containing the user if found, empty otherwise
     */
    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(users.get(username));
    }

    /**
     * Gets the maximum age among all users.
     * @return an Optional containing the max age if users exist, empty otherwise
     */
    public OptionalInt getMaxAge() {
        return users.values().stream()
                   .mapToInt(User::getAge)
                   .max();
    }
}

// Usage
Optional<User> user = repository.findByUsername("john");
if (user.isPresent()) {
    System.out.println("Found user: " + user.get().getName());
} else {
    System.out.println("User not found");
}

// Or with functional style
repository.findByUsername("john")
         .ifPresentOrElse(
             u -> System.out.println("Found: " + u.getName()),
             () -> System.out.println("User not found")
         );
```

**Bad example:**

```java
public class UserRepository {
    private final Map<String, User> users = new HashMap<>();

    // Bad: Using Optional for fields
    private Optional<String> defaultUsername = Optional.empty();

    // Bad: Using Optional for parameters
    public void updateUser(Optional<String> username, Optional<String> email) {
        // This makes the API harder to use
    }

    // Bad: Using Optional for collections
    public Optional<List<User>> getAllUsers() {
        return users.isEmpty() ? Optional.empty() : Optional.of(new ArrayList<>(users.values()));
        // Should just return empty list instead
    }

    // Bad: Optional in performance-critical code where null would be fine
    public Optional<User> findByUsernameInLoop(String username) {
        // If this is called in a tight loop, the Optional allocation overhead matters
        return Optional.ofNullable(users.get(username));
    }
}
```

### Example 45: Exception Handling

Title: Handle Exceptions Effectively and Appropriately
Description: Proper exception handling makes code more robust and easier to debug. These practices ensure exceptions are used correctly and provide meaningful information.

### Example 46: Use Exceptions Only for Exceptional Conditions

Title: Don't use exceptions for ordinary control flow
Description: Exceptions should be used for exceptional conditions, not for normal program flow. They are expensive and make code harder to understand.

**Good example:**

```java
public class NumberProcessor {
    public void processNumbers(int[] numbers) {
        for (int number : numbers) {  // Normal iteration
            if (isValid(number)) {    // Normal condition checking
                process(number);
            } else {
                System.out.println("Skipping invalid number: " + number);
            }
        }
    }

    private boolean isValid(int number) {
        return number >= 0 && number <= 1000;
    }

    private void process(int number) {
        // Process the number
        System.out.println("Processing: " + number);
    }
}
```

**Bad example:**

```java
public class NumberProcessor {
    public void processNumbers(int[] numbers) {
        try {
            int i = 0;
            while (true) {  // Using exception for loop termination
                process(numbers[i++]);
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            // Using exception for normal control flow - bad!
        }
    }

    private void process(int number) {
        System.out.println("Processing: " + number);
    }
}
```

### Example 47: Use Checked Exceptions for Recoverable Conditions and Runtime Exceptions for Programming Errors

Title: Choose the right type of exception for the situation
Description: Checked exceptions force the caller to handle recoverable conditions, while runtime exceptions indicate programming errors.

**Good example:**

```java
public class FileProcessor {
    /**
     * Processes a file. Throws checked exception for recoverable I/O issues.
     */
    public void processFile(String filename) throws FileProcessingException {
        try {
            // File operations that might fail due to external factors
            Files.readAllLines(Paths.get(filename));
        } catch (IOException e) {
            // Wrap in domain-specific checked exception
            throw new FileProcessingException("Failed to process file: " + filename, e);
        }
    }

    /**
     * Validates input parameters. Throws runtime exception for programming errors.
     */
    public void validateInput(String input) {
        if (input == null) {
            // Programming error - should never happen in correct code
            throw new IllegalArgumentException("Input cannot be null");
        }
        if (input.trim().isEmpty()) {
            // Programming error - caller should validate before calling
            throw new IllegalArgumentException("Input cannot be empty");
        }
    }
}

// Custom checked exception for recoverable conditions
class FileProcessingException extends Exception {
    public FileProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

**Bad example:**

```java
public class FileProcessor {
    // Bad: Using checked exception for programming error
    public void validateInput(String input) throws ValidationException {
        if (input == null) {
            throw new ValidationException("Input cannot be null");  // Should be RuntimeException
        }
    }

    // Bad: Using runtime exception for recoverable condition
    public void processFile(String filename) {
        try {
            Files.readAllLines(Paths.get(filename));
        } catch (IOException e) {
            // Should be checked exception so caller can handle
            throw new RuntimeException("File processing failed", e);
        }
    }
}
```

### Example 48: Favor the Use of Standard Exceptions

Title: Use standard Java exceptions when appropriate
Description: Standard exceptions are familiar to developers and have clear semantics. Don't reinvent the wheel.

**Good example:**

```java
public class Calculator {
    public double divide(double dividend, double divisor) {
        if (divisor == 0.0) {
            throw new ArithmeticException("Division by zero");  // Standard exception
        }
        return dividend / divisor;
    }

    public int getElement(List<Integer> list, int index) {
        Objects.requireNonNull(list, "list");  // Standard NullPointerException
        if (index < 0 || index >= list.size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + list.size());
        }
        return list.get(index);
    }

    public void processPositiveNumber(int number) {
        if (number <= 0) {
            throw new IllegalArgumentException("Number must be positive: " + number);
        }
        // Process the number
    }
}
```

**Bad example:**

```java
public class Calculator {
    public double divide(double dividend, double divisor) {
        if (divisor == 0.0) {
            throw new DivisionByZeroException("Cannot divide by zero");  // Custom exception when standard would do
        }
        return dividend / divisor;
    }

    public int getElement(List<Integer> list, int index) {
        if (list == null) {
            throw new ListIsNullException("List cannot be null");  // Custom exception when standard would do
        }
        if (index < 0 || index >= list.size()) {
            throw new InvalidIndexException("Bad index: " + index);  // Custom exception when standard would do
        }
        return list.get(index);
    }
}

// Unnecessary custom exceptions
class DivisionByZeroException extends RuntimeException {
    public DivisionByZeroException(String message) {
        super(message);
    }
}
class ListIsNullException extends RuntimeException {
    public ListIsNullException(String message) {
        super(message);
    }
}
class InvalidIndexException extends RuntimeException {
    public InvalidIndexException(String message) {
        super(message);
    }
}
```
