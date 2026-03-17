# java-secure-coding

> Source: 124-java-secure-coding.md
> Chunk: 1/2
> Included sections: intro | Role | Goal | Constraints | Examples | Examples - Table of contents | Examples - Example 1: Input Validation | Examples - Example 2: Protect Against Injection Attacks | Examples - Example 3: Minimize Attack Surface

---
author: Juan Antonio Breña Moral
version: 0.12.0-SNAPSHOT
---
# Java Secure coding guidelines

## Role

You are a Senior software engineer with extensive experience in Java software development

## Goal

This document provides essential Java secure coding guidelines, focusing on five key areas: validating all untrusted inputs to prevent attacks like injection and path traversal; protecting against injection attacks (e.g., SQL injection) by using parameterized queries or prepared statements; minimizing the attack surface by adhering to the principle of least privilege and reducing exposure; employing strong, current cryptographic algorithms for hashing, encryption, and digital signatures while avoiding deprecated ones; and handling exceptions securely by avoiding the exposure of sensitive information in error messages to users and logging detailed, non-sensitive diagnostic information for developers.

### Implementing These Principles

These guidelines are built upon the following core principles:

1.  **Comprehensive Input Validation**: Treat all external input as untrusted. Rigorously validate and sanitize data for type, length, format, and range before processing to prevent common vulnerabilities like injection attacks, path traversal, and buffer overflows.
2.  **Defense Against Injection**: Actively protect against all forms of injection attacks (e.g., SQL, OS Command, LDAP, XPath). Primarily achieve this by using safe APIs like parameterized queries (e.g., `PreparedStatement` in JDBC) or dedicated libraries that correctly handle data escaping, and by never directly concatenating untrusted input into executable commands or queries.
3.  **Attack Surface Minimization**: Adhere to the principle of least privilege for users, processes, and code. Reduce the exposure of system components by running with minimal necessary permissions, exposing only essential functionalities and network ports, and regularly reviewing and removing unused features, libraries, and accounts.
4.  **Strong Cryptographic Practices**: Employ current, robust, and industry-standard cryptographic algorithms and libraries for all sensitive operations, including hashing (especially for passwords), encryption, and digital signatures. Avoid deprecated or weak algorithms. Ensure cryptographic keys are generated securely, stored safely, and managed properly throughout their lifecycle.
5.  **Secure Exception Handling**: Manage exceptions in a way that does not expose sensitive information to users or attackers. Log detailed, non-sensitive diagnostic information for developers to aid in debugging, but provide generic, non-revealing error messages to clients. Avoid direct exposure of stack traces or internal system details in error outputs.
6.  **Secrets Management and Configuration Security**: Never hardcode secrets (passwords, API keys, tokens) in source code. Load secrets at runtime from secure sources (environment variables, secret managers, container/Docker/Kubernetes secrets), enforce least-privilege for credentials, rotate regularly, and prevent secrets from appearing in logs or error messages.
7.  **Safe Serialization and Deserialization**: Avoid Java native serialization for untrusted data. Prefer data binding to explicit DTOs (e.g., JSON/XML) without permissive polymorphic typing. If deserialization is necessary, implement strict allow-lists of types and validate content before use.
8. **Output Encoding for XSS Prevention**: Always encode user input when outputting to prevent cross-site scripting attacks.

## Constraints

Before applying any recommendations, ensure the project is in a valid state by running Maven compilation. Compilation failure is a BLOCKING condition that prevents any further processing.

- **MANDATORY**: Run `./mvnw compile` or `mvn compile` before applying any change
- **PREREQUISITE**: Project must compile successfully and pass basic validation checks before any optimization
- **CRITICAL SAFETY**: If compilation fails, IMMEDIATELY STOP and DO NOT CONTINUE with any recommendations
- **BLOCKING CONDITION**: Compilation errors must be resolved by the user before proceeding with any object-oriented design improvements
- **NO EXCEPTIONS**: Under no circumstances should design recommendations be applied to a project that fails to compile

## Examples

### Table of contents

- Example 1: Input Validation
- Example 2: Protect Against Injection Attacks
- Example 3: Minimize Attack Surface
- Example 4: Use Strong Cryptography
- Example 5: Handle Exceptions Securely
- Example 6: Secrets Management
- Example 7: Safe Deserialization
- Example 8: Prevent Cross-Site Scripting (XSS)

### Example 1: Input Validation

Title: Validate All Untrusted Inputs
Description: Always validate and sanitize data received from untrusted sources (users, network, files, etc.) before processing. This helps prevent various attacks like injection, path traversal, and buffer overflows. Validation should check for type, length, format, and range.

**Good example:**

```java
// GOOD: Comprehensive input validation
import java.util.Objects;
import java.util.regex.Pattern;

public class SecureInputValidator {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,16}$");
    private static final int MAX_AGE = 120;
    private static final int MIN_AGE = 0;

    public void processUserData(String username, String ageString) {
        // Validate username format
        if (Objects.isNull(username) || !USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException("Invalid username format. Must be 3-16 alphanumeric characters or underscores.");
        }

        // Validate and parse age
        int age;
        try {
            age = Integer.parseInt(ageString);
            if (age < MIN_AGE || age > MAX_AGE) {
                throw new IllegalArgumentException("Age must be between " + MIN_AGE + " and " + MAX_AGE);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid age format. Must be a valid integer.", e);
        }

        // Input is now validated and safe to process
        System.out.println("Processing validated user: " + username + ", age: " + age);
    }

    public String sanitizeFilePath(String userPath) {
        if (userPath == null) {
            throw new IllegalArgumentException("File path cannot be null");
        }

        // Prevent path traversal attacks
        String sanitized = userPath.replaceAll("\\.\\.", "").replaceAll("/", "");

        // Additional validation
        if (sanitized.length() > 255) {
            throw new IllegalArgumentException("File path too long");
        }

        return sanitized;
    }
}
```

**Bad example:**

```java
// AVOID: No input validation
public class UnsafeInputProcessor {
    public void processUserData(String username, String ageString) {
        // Directly using input without validation - DANGEROUS!
        int age = Integer.parseInt(ageString); // Can throw NumberFormatException

        // No checks for malicious username strings
        System.out.println("Processing user: " + username + ", age: " + age);

        // This could lead to issues if username contains scripts or ageString is not an integer
    }

    public String loadFile(String userPath) {
        // VULNERABLE: No validation allows path traversal attacks
        // User could pass "../../etc/passwd" to access sensitive files
        return readFileContent(userPath);
    }
}
```

### Example 2: Protect Against Injection Attacks

Title: Use Parameterized Queries and Safe APIs
Description: To prevent SQL Injection and other injection attacks, always use parameterized queries (PreparedStatements in JDBC) or an ORM that handles this automatically. Never concatenate user input directly into SQL queries, OS commands, or other executable statements.

**Good example:**

```java
// GOOD: Using PreparedStatement to prevent SQL Injection
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SecureDataAccess {
    private static final String DB_URL = "jdbc:h2:mem:testdb";
    private static final String USER = "sa";
    private static final String PASS = "";

    public List<Order> getOrdersByCustomerId(String customerId) throws SQLException {
        // Safe parameterized query
        String query = "SELECT order_id, customer_id, amount FROM orders WHERE customer_id = ?";
        List<Order> orders = new ArrayList<>();

        try (Connection con = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement pstmt = con.prepareStatement(query)) {

            pstmt.setString(1, customerId); // Parameter is safely bound
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Order order = new Order();
                order.setOrderId(rs.getString("order_id"));
                order.setCustomerId(rs.getString("customer_id"));
                order.setAmount(rs.getBigDecimal("amount"));
                orders.add(order);
            }
        }

        return orders;
    }

    public void updateCustomerEmail(String customerId, String newEmail) throws SQLException {
        String updateQuery = "UPDATE customers SET email = ? WHERE customer_id = ?";

        try (Connection con = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement pstmt = con.prepareStatement(updateQuery)) {

            pstmt.setString(1, newEmail);
            pstmt.setString(2, customerId);
            pstmt.executeUpdate();
        }
    }
}
```

**Bad example:**

```java
// AVOID: Vulnerable to SQL Injection
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class VulnerableDataAccess {
    private static final String DB_URL = "jdbc:h2:mem:testdb";
    private static final String USER = "sa";
    private static final String PASS = "";

    public List<Order> getOrdersByCustomerId(String customerId) throws SQLException {
        // DANGEROUS: User input directly concatenated into SQL query
        String query = "SELECT order_id, customer_id, amount FROM orders WHERE customer_id = '" + customerId + "'";
        List<Order> orders = new ArrayList<>();

        try (Connection con = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = con.createStatement()) {

            // User could pass: "'; DROP TABLE orders; --" to execute malicious SQL
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                // Process results...
            }
        }

        return orders;
    }

    public void executeCommand(String userCommand) {
        // EXTREMELY DANGEROUS: Command injection vulnerability
        String command = "ls " + userCommand; // User could inject "; rm -rf /"
        try {
            Runtime.getRuntime().exec(command);
        } catch (Exception e) {
            // Handle exception
        }
    }
}
```

### Example 3: Minimize Attack Surface

Title: Apply Principle of Least Privilege
Description: Grant only necessary permissions to code and users. Avoid running processes with excessive privileges. Expose only essential functionality and network ports. Regularly review and remove unused features, libraries, and accounts.

**Good example:**

```java
// GOOD: Minimal privileges and controlled access
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.Set;

public class SecureFileManager {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".txt", ".log", ".json");
    private static final Path SAFE_DIRECTORY = Paths.get("/app/data/uploads");

    public String readUserFile(String filename, Principal user) throws SecurityException, IOException {
        // Check user permissions
        if (!hasReadPermission(user, filename)) {
            throw new SecurityException("User does not have permission to read this file");
        }

        // Validate file extension
        String extension = getFileExtension(filename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new SecurityException("File type not allowed: " + extension);
        }

        // Ensure file is within safe directory
        Path filePath = SAFE_DIRECTORY.resolve(filename).normalize();
        if (!filePath.startsWith(SAFE_DIRECTORY)) {
            throw new SecurityException("File access outside allowed directory");
        }

        // Check file size limits
        if (Files.size(filePath) > 1024 * 1024) { // 1MB limit
            throw new SecurityException("File too large");
        }

        return Files.readString(filePath);
    }

    private boolean hasReadPermission(Principal user, String filename) {
        // Implement proper authorization logic
        return user != null && user.getName() != null;
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot) : "";
    }
}

// Example of interface segregation - expose only necessary methods
public interface UserService {
    User findById(Long id);
    void updateProfile(Long id, UserProfile profile);
    // Don't expose administrative methods to regular users
}

public interface AdminService extends UserService {
    void deleteUser(Long id);
    List<User> getAllUsers();
    void resetPassword(Long id);
}
```

**Bad example:**

```java
// AVOID: Excessive privileges and exposure
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class UnsafeFileManager {

    // BAD: No access controls or validations
    public String readAnyFile(String filename) throws IOException {
        // DANGEROUS: Can read any file on the system
        File file = new File(filename);
        return Files.readString(file.toPath());
    }

    // BAD: Exposing dangerous functionality
    public void executeSystemCommand(String command) throws IOException {
        // EXTREMELY DANGEROUS: Allows arbitrary command execution
        Runtime.getRuntime().exec(command);
    }

    // BAD: Administrative functions mixed with user functions
    public class UserController {
        public void updateProfile(User user) { /* ... */ }
        public void deleteAllUsers() { /* ... */ } // Should not be here
        public void resetDatabase() { /* ... */ }  // Extremely dangerous
        public void viewSystemLogs() { /* ... */ } // Sensitive operation
    }
}

// BAD: Running with excessive privileges
// Starting application as root/administrator user
// Using database accounts with DBA privileges for regular operations
// Exposing debug endpoints in production
// Having default passwords or credentials
```
