# java-ood

> Source: 121-java-object-oriented-design.md
> Chunk: 6/6
> Included sections: Examples - Example 49: Include Failure-Capture Information in Detail Messages | Examples - Example 50: Don't Ignore Exceptions | Output Format | Safeguards

### Example 49: Include Failure-Capture Information in Detail Messages

Title: Provide detailed, actionable information in exception messages
Description: Exception messages should contain all information needed to diagnose the failure.

**Good example:**

```java
public class BankAccount {
    private double balance;
    private final String accountNumber;

    public BankAccount(String accountNumber, double initialBalance) {
        this.accountNumber = accountNumber;
        this.balance = initialBalance;
    }

    public void withdraw(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException(
                String.format("Withdrawal amount must be positive. Account: %s, Amount: %.2f",
                             accountNumber, amount));
        }
        if (amount > balance) {
            throw new InsufficientFundsException(
                String.format("Insufficient funds. Account: %s, Balance: %.2f, Requested: %.2f",
                             accountNumber, balance, amount));
        }
        balance -= amount;
    }

    public void transfer(BankAccount toAccount, double amount) {
        if (toAccount == null) {
            throw new IllegalArgumentException(
                String.format("Destination account cannot be null. Source account: %s, Amount: %.2f",
                             accountNumber, amount));
        }
        if (toAccount.accountNumber.equals(this.accountNumber)) {
            throw new IllegalArgumentException(
                String.format("Cannot transfer to same account. Account: %s, Amount: %.2f",
                             accountNumber, amount));
        }
        withdraw(amount);
        toAccount.deposit(amount);
    }

    public void deposit(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException(
                String.format("Deposit amount must be positive. Account: %s, Amount: %.2f",
                             accountNumber, amount));
        }
        balance += amount;
    }
}

class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}
```

**Bad example:**

```java
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

### Example 50: Don't Ignore Exceptions

Title: Always handle exceptions appropriately, never ignore them silently
Description: Ignoring exceptions can hide bugs and make debugging extremely difficult.

**Good example:**

```java
public class FileManager {
    private static final Logger logger = LoggerFactory.getLogger(FileManager.class);

    public Optional<String> readFileContent(String filename) {
        try {
            return Optional.of(Files.readString(Paths.get(filename)));
        } catch (IOException e) {
            // Log the exception with context
            logger.warn("Failed to read file: {}", filename, e);
            return Optional.empty();  // Return meaningful result
        }
    }

    public void saveToFile(String filename, String content) throws FileOperationException {
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
                // Some file processing
                Files.readString(Paths.get(file));
            } catch (Exception e) {
                // Catching Exception is too broad, and then ignoring it
            }
        }
    }
}
```

## Output Format

- **ANALYZE** Java code to identify specific object-oriented design issues and categorize them by impact (CRITICAL, MAINTAINABILITY, FLEXIBILITY, CODE_QUALITY) and design area (SOLID violations, code smells, encapsulation problems, inheritance misuse, composition opportunities)
- **CATEGORIZE** object-oriented design improvements found: SOLID Principle Violations (single responsibility breaches vs focused classes, open/closed violations vs extensible design, Liskov substitution problems vs proper inheritance, interface segregation issues vs role-based interfaces, dependency inversion problems vs abstraction-based design), Code Smell Issues (God Classes vs decomposed responsibilities, Feature Envy vs proper method placement, Data Clumps vs cohesive objects, Inappropriate Intimacy vs proper encapsulation), Design Problems (inheritance misuse vs composition patterns, exposed state vs encapsulated behavior, tight coupling vs loose coupling)
- **APPLY** object-oriented design best practices directly by implementing the most appropriate improvements for each identified issue: Extract classes to enforce single responsibility, introduce interfaces to achieve open/closed compliance, refactor inheritance hierarchies to ensure Liskov substitution, segregate fat interfaces into focused role interfaces, apply dependency injection for inversion of control, decompose God Classes into focused components, move methods to eliminate feature envy, create cohesive objects from data clumps, and replace inheritance with composition where appropriate
- **IMPLEMENT** comprehensive object-oriented design refactoring using proven patterns: Apply SOLID principles systematically (Single Responsibility through class extraction, Open/Closed through strategy patterns, Liskov Substitution through proper inheritance, Interface Segregation through role interfaces, Dependency Inversion through abstraction), eliminate code smells through targeted refactoring (Extract Class, Move Method, Replace Inheritance with Composition, Introduce Parameter Object), improve encapsulation through access control and information hiding, and modernize design with appropriate patterns
- **REFACTOR** code systematically following the object-oriented design improvement roadmap: First enforce single responsibility by extracting focused classes, then establish proper abstractions through interfaces and inheritance hierarchies, apply composition over inheritance where beneficial, improve encapsulation by hiding implementation details, eliminate code smells through targeted refactoring techniques, and integrate modern design patterns where they add value
- **EXPLAIN** the applied object-oriented design improvements and their benefits: Maintainability enhancements through SOLID compliance, flexibility gains from proper abstraction and composition, testability improvements through dependency injection and focused responsibilities, code clarity benefits from eliminated code smells, and long-term sustainability advantages from well-structured object-oriented design
- **VALIDATE** that all applied object-oriented design refactoring compiles successfully, maintains existing functionality, preserves business logic integrity, follows established design principles, and achieves the intended architectural improvements through comprehensive testing and verification

## Safeguards

- **BLOCKING SAFETY CHECK**: ALWAYS run `./mvnw compile` or `mvn compile` before ANY design recommendations - compilation failure is a HARD STOP
- **CRITICAL VALIDATION**: Execute `./mvnw clean verify` or `mvn clean verify` to ensure all tests pass after any refactoring changes
- **MANDATORY VERIFICATION**: Confirm all existing functionality remains intact after applying object-oriented design improvements
- **SAFETY PROTOCOL**: If ANY compilation error occurs, IMMEDIATELY cease recommendations and require user intervention
- **ROLLBACK REQUIREMENT**: Ensure all changes can be easily reverted if they introduce regressions or compilation issues
- **INCREMENTAL SAFETY**: Apply refactoring changes incrementally, validating compilation after each significant modification
- **DEPENDENCY VALIDATION**: Check that refactoring doesn't break existing dependencies, imports, or class relationships
- **FINAL VERIFICATION**: After completing all design improvements, perform a final full project compilation and test run
