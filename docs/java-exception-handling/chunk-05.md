# java-exception-handling

> Source: 127-java-exception-handling.md
> Chunk: 5/6
> Included sections: Examples - Example 8: Use Checked Exceptions for Recoverable Conditions and Runtime Exceptions for Programming Errors | Examples - Example 9: Favor the Use of Standard Exceptions | Examples - Example 10: Include Failure-Capture Information in Detail Messages | Examples - Example 11: Don't Ignore Exceptions | Examples - Example 12: Testing Exception Scenarios Effectively

### Example 8: Use Checked Exceptions for Recoverable Conditions and Runtime Exceptions for Programming Errors

Title: Choose the right type of exception for the situation
Description: Checked exceptions force the caller to handle recoverable conditions, while runtime exceptions indicate programming errors that should be fixed in code. Use checked exceptions for conditions the caller can reasonably recover from.

**Good example:**

```java
// GOOD: Appropriate exception types for different scenarios
public class FileProcessor {

    /**
     * Processes a file. Throws checked exception for recoverable I/O issues.
     *
     * @param filename the file to process
     * @throws FileProcessingException if file cannot be processed (recoverable)
     */
    public void processFile(String filename) throws FileProcessingException {
        Objects.requireNonNull(filename, "Filename cannot be null");

        try {
            List<String> lines = Files.readAllLines(Paths.get(filename));
            lines.forEach(this::processLine);
        } catch (IOException e) {
            // Wrap in domain-specific checked exception - caller can retry
            throw new FileProcessingException("Failed to process file: " + filename, e);
        }
    }

    /**
     * Validates input parameters. Throws runtime exception for programming errors.
     *
     * @param input the input to validate
     * @throws IllegalArgumentException if input is invalid (programming error)
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

    private void processLine(String line) {
        // Processing logic
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
// AVOID: Wrong exception types for the situation
public class FileProcessor {

    // BAD: Using checked exception for programming error
    public void validateInput(String input) throws ValidationException {
        if (input == null) {
            throw new ValidationException("Input cannot be null");  // Should be RuntimeException
        }
    }

    // BAD: Using runtime exception for recoverable condition
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

### Example 9: Favor the Use of Standard Exceptions

Title: Use standard Java exceptions when appropriate
Description: Standard exceptions are familiar to developers and have clear semantics. Don't create custom exceptions when standard ones sufficiently express the error condition.

**Good example:**

```java
// GOOD: Using standard exceptions with descriptive messages
public class Calculator {

    public double divide(double dividend, double divisor) {
        if (divisor == 0.0) {
            throw new ArithmeticException("Division by zero is not allowed");  // Standard exception
        }
        return dividend / divisor;
    }

    public int getElement(List<Integer> list, int index) {
        Objects.requireNonNull(list, "List cannot be null");

        if (index < 0 || index >= list.size()) {
            throw new IndexOutOfBoundsException(
                String.format("Index %d is out of bounds for list of size %d", index, list.size())
            );
        }
        return list.get(index);
    }

    public void processPositiveNumber(int number) {
        if (number <= 0) {
            throw new IllegalArgumentException("Number must be positive, was: " + number);
        }
        // Process the number
    }
}
```

**Bad example:**

```java
// AVOID: Custom exceptions when standard ones would suffice
public class Calculator {

    public double divide(double dividend, double divisor) {
        if (divisor == 0.0) {
            throw new DivisionByZeroException("Cannot divide by zero");  // Unnecessary custom exception
        }
        return dividend / divisor;
    }

    public int getElement(List<Integer> list, int index) {
        if (list == null) {
            throw new ListIsNullException("List cannot be null");  // Should use NullPointerException
        }
        if (index < 0 || index >= list.size()) {
            throw new InvalidIndexException("Bad index: " + index);  // Should use IndexOutOfBoundsException
        }
        return list.get(index);
    }
}

// Unnecessary custom exceptions
class DivisionByZeroException extends RuntimeException {
    public DivisionByZeroException(String message) { super(message); }
}

class ListIsNullException extends RuntimeException {
    public ListIsNullException(String message) { super(message); }
}

class InvalidIndexException extends RuntimeException {
    public InvalidIndexException(String message) { super(message); }
}
```

### Example 10: Include Failure-Capture Information in Detail Messages

Title: Provide detailed, actionable information in exception messages
Description: Exception messages should provide enough context to understand what went wrong and how to fix it. Include relevant parameter values, expected ranges, and specific failure conditions.

**Good example:**

```java
// GOOD: Detailed exception messages with context
public class BankAccount {
    private double balance;
    private final String accountNumber;
    private final String accountHolderName;

    public BankAccount(String accountNumber, String accountHolderName, double initialBalance) {
        this.accountNumber = Objects.requireNonNull(accountNumber, "Account number cannot be null");
        this.accountHolderName = Objects.requireNonNull(accountHolderName, "Account holder name cannot be null");

        if (initialBalance < 0) {
            throw new IllegalArgumentException(
                String.format("Initial balance cannot be negative. Account: %s, Attempted balance: %.2f",
                             accountNumber, initialBalance)
            );
        }
        this.balance = initialBalance;
    }

    public void withdraw(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException(
                String.format("Withdrawal amount must be positive. Account: %s, Attempted amount: %.2f",
                             accountNumber, amount)
            );
        }

        if (amount > balance) {
            throw new InsufficientFundsException(
                String.format("Insufficient funds for withdrawal. Account: %s, Current balance: %.2f, Requested: %.2f",
                             accountNumber, balance, amount)
            );
        }

        balance -= amount;
    }

    public void transfer(BankAccount toAccount, double amount) {
        Objects.requireNonNull(toAccount, "Destination account cannot be null");

        if (toAccount.accountNumber.equals(this.accountNumber)) {
            throw new IllegalArgumentException(
                String.format("Cannot transfer to the same account. Account number: %s", accountNumber)
            );
        }

        withdraw(amount); // This will validate amount and balance
        toAccount.deposit(amount);
    }

    public void deposit(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException(
                String.format("Deposit amount must be positive. Account: %s, Attempted amount: %.2f",
                             accountNumber, amount)
            );
        }
        balance += amount;
    }
}
```

**Bad example:**

```java
// AVOID: Vague exception messages without context
public class BankAccount {
    private double balance;
    private final String accountNumber;

    public void withdraw(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Invalid amount");  // Too vague
        }
        if (amount > balance) {
            throw new InsufficientFundsException("Not enough money");  // No specific details
        }
        balance -= amount;
    }

    public void transfer(BankAccount toAccount, double amount) {
        if (toAccount == null) {
            throw new IllegalArgumentException("Bad account");  // No context
        }
        if (toAccount.accountNumber.equals(this.accountNumber)) {
            throw new IllegalArgumentException("Error");  // Completely unhelpful
        }
        withdraw(amount);
        toAccount.deposit(amount);
    }
}
```

### Example 11: Don't Ignore Exceptions

Title: Always handle exceptions appropriately, never ignore them silently
Description: Ignoring exceptions can hide bugs and make debugging extremely difficult. Always log exceptions appropriately, handle them meaningfully, or re-throw them with additional context.

**Good example:**

```java
// GOOD: Proper exception handling without ignoring
public class FileManager {
    private static final Logger logger = LoggerFactory.getLogger(FileManager.class);

    public Optional<String> readFileContent(String filename) {
        Objects.requireNonNull(filename, "Filename cannot be null");

        try {
            return Optional.of(Files.readString(Paths.get(filename)));
        } catch (IOException e) {
            // Log the exception with context
            logger.warn("Failed to read file: {}", filename, e);
            return Optional.empty();  // Return meaningful result
        }
    }

    public void saveToFile(String filename, String content) throws FileOperationException {
        Objects.requireNonNull(filename, "Filename cannot be null");
        Objects.requireNonNull(content, "Content cannot be null");

        try {
            Files.writeString(Paths.get(filename), content);
            logger.info("Successfully saved content to file: {}", filename);
        } catch (IOException e) {
            // Re-throw as domain-specific exception with context
            throw new FileOperationException("Failed to save content to file: " + filename, e);
        }
    }

    public void cleanupTempFiles(List<String> tempFiles) {
        for (String tempFile : tempFiles) {
            try {
                Files.deleteIfExists(Paths.get(tempFile));
            } catch (IOException e) {
                // Log but continue with other files
                logger.warn("Failed to delete temporary file: {}", tempFile, e);
            }
        }
    }
}

class FileOperationException extends Exception {
    public FileOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

**Bad example:**

```java
// AVOID: Ignoring exceptions
public class FileManager {

    public String readFileContent(String filename) {
        try {
            return Files.readString(Paths.get(filename));
        } catch (IOException e) {
            // Silently ignoring exception - very bad!
        }
        return null;  // Caller has no idea what went wrong
    }

    public void saveToFile(String filename, String content) {
        try {
            Files.writeString(Paths.get(filename), content);
        } catch (IOException e) {
            // Empty catch block - hiding the problem
        }
    }

    public void processFiles(List<String> files) {
        for (String file : files) {
            try {
                processFile(file);
            } catch (Exception e) {
                // Ignoring all exceptions - could hide critical issues
                e.printStackTrace(); // Poor logging
            }
        }
    }
}
```

### Example 12: Testing Exception Scenarios Effectively

Title: Comprehensive patterns for testing exception handling
Description: Test exception scenarios thoroughly using AssertJ's fluent API for clear, maintainable test code. Verify both that exceptions are thrown when expected and that they contain appropriate messages and context.

**Good example:**

```java
// GOOD: Comprehensive exception testing with AssertJ
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalculatorExceptionTest {

    private final Calculator calculator = new Calculator();

    @Test
    @DisplayName("Should throw ArithmeticException when dividing by zero")
    void divide_byZero_throwsArithmeticException() {
        // Given
        double dividend = 10.0;
        double divisor = 0.0;

        // When & Then
        assertThatThrownBy(() -> calculator.divide(dividend, divisor))
            .isInstanceOf(ArithmeticException.class)
            .hasMessageContaining("Division by zero")
            .hasNoCause();
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for negative number validation")
    void processPositiveNumber_withNegative_throwsIllegalArgumentException() {
        // Given
        int negativeNumber = -5;

        // When & Then
        assertThatThrownBy(() -> calculator.processPositiveNumber(negativeNumber))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Number must be positive")
            .hasMessageContaining("-5");
    }

    @Test
    @DisplayName("Should throw IndexOutOfBoundsException with detailed message")
    void getElement_withInvalidIndex_throwsIndexOutOfBoundsException() {
        // Given
        List<Integer> list = Arrays.asList(1, 2, 3);
        int invalidIndex = 5;

        // When & Then
        assertThatThrownBy(() -> calculator.getElement(list, invalidIndex))
            .isInstanceOf(IndexOutOfBoundsException.class)
            .hasMessageContaining("Index 5")
            .hasMessageContaining("size 3");
    }

    @Test
    @DisplayName("Should handle chained exceptions properly")
    void processFile_withIOError_throwsFileProcessingExceptionWithCause() {
        // Given
        FileProcessor processor = new FileProcessor();
        String nonExistentFile = "non-existent-file.txt";

        // When & Then
        assertThatThrownBy(() -> processor.processFile(nonExistentFile))
            .isInstanceOf(FileProcessingException.class)
            .hasMessageContaining("Failed to process file")
            .hasMessageContaining(nonExistentFile)
            .hasCauseInstanceOf(IOException.class);
    }

    @Test
    @DisplayName("Should verify exception is not thrown for valid input")
    void divide_withValidInput_doesNotThrowException() {
        // Given
        double dividend = 10.0;
        double divisor = 2.0;

        // When & Then
        assertThat(calculator.divide(dividend, divisor))
            .isEqualTo(5.0);
    }
}

// Test for graceful degradation
class FileManagerTest {

    @Test
    @DisplayName("Should return empty Optional when file read fails")
    void readFileContent_withNonExistentFile_returnsEmptyOptional() {
        // Given
        FileManager fileManager = new FileManager();
        String nonExistentFile = "non-existent.txt";

        // When
        Optional<String> result = fileManager.readFileContent(nonExistentFile);

        // Then
        assertThat(result).isEmpty();
        // Verify that exception was logged (would need log capture in real test)
    }
}
```

**Bad example:**

```java
// AVOID: Poor exception testing practices
class CalculatorTestBad {

    @Test
    void testDivision() {
        Calculator calculator = new Calculator();

        try {
            calculator.divide(10, 0);
            fail("Should have thrown exception"); // JUnit 4 style
        } catch (Exception e) {
            // Too generic - doesn't verify exception type or message
            assertTrue(e.getMessage().contains("zero"));
        }
    }

    @Test
    void testValidation() {
        Calculator calculator = new Calculator();

        // BAD: Not testing exception scenarios at all
        assertThat(calculator.processPositiveNumber(5)).isEqualTo(5);
        // Missing: What happens with negative numbers?
    }

    @Test
    void testFileProcessing() {
        // BAD: No exception testing for file operations
        FileProcessor processor = new FileProcessor();
        // Only testing happy path, ignoring error conditions
    }
}
```
