# java-exception-handling

> Source: 127-java-exception-handling.md
> Chunk: 4/6
> Included sections: Examples - Example 6: Custom Exception Design and Documentation | Examples - Example 7: Use Exceptions Only for Exceptional Conditions

### Example 6: Custom Exception Design and Documentation

Title: Create Meaningful Exception Hierarchies with Proper Documentation
Description: Design custom exception hierarchies that provide semantic meaning and enable appropriate handling at different application layers. Document exceptions thoroughly with @throws tags and provide clear guidance on when and why they are thrown.

**Good example:**

```java
// GOOD: Well-designed custom exception hierarchy with proper documentation
/**
 * Base exception for all business-related errors in the user management system.
 * This is a checked exception to force explicit handling of business logic failures.
 */
public class UserManagementException extends Exception {
    private final ErrorCode errorCode;
    private final String userContext;

    public UserManagementException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.userContext = null;
    }

    public UserManagementException(String message, ErrorCode errorCode, String userContext) {
        super(message);
        this.errorCode = errorCode;
        this.userContext = userContext;
    }

    public UserManagementException(String message, ErrorCode errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.userContext = null;
    }

    public UserManagementException(String message, ErrorCode errorCode, String userContext, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.userContext = userContext;
    }

    public ErrorCode getErrorCode() { return errorCode; }
    public String getUserContext() { return userContext; }
}

/**
 * Thrown when user validation fails due to invalid input data.
 * This is a specific exception that indicates the client provided invalid data.
 */
public class UserValidationException extends UserManagementException {
    private final List<String> validationErrors;

    public UserValidationException(String message, List<String> validationErrors) {
        super(message, ErrorCode.VALIDATION_FAILED);
        this.validationErrors = new ArrayList<>(validationErrors);
    }

    public UserValidationException(String message, String userContext, List<String> validationErrors) {
        super(message, ErrorCode.VALIDATION_FAILED, userContext);
        this.validationErrors = new ArrayList<>(validationErrors);
    }

    public List<String> getValidationErrors() {
        return new ArrayList<>(validationErrors);
    }
}

/**
 * Thrown when a requested user cannot be found in the system.
 * This typically results in a 404 response in web applications.
 */
public class UserNotFoundException extends UserManagementException {
    private final String requestedUserId;

    public UserNotFoundException(String userId) {
        super("User not found: " + userId, ErrorCode.USER_NOT_FOUND, userId);
        this.requestedUserId = userId;
    }

    public UserNotFoundException(String userId, Throwable cause) {
        super("User not found: " + userId, ErrorCode.USER_NOT_FOUND, userId, cause);
        this.requestedUserId = userId;
    }

    public String getRequestedUserId() { return requestedUserId; }
}

/**
 * Thrown when user operations fail due to insufficient permissions.
 * This indicates an authorization failure after successful authentication.
 */
public class UserPermissionException extends UserManagementException {
    private final String requiredPermission;
    private final String currentUserRole;

    public UserPermissionException(String requiredPermission, String currentUserRole) {
        super("Insufficient permissions. Required: " + requiredPermission + ", Current role: " + currentUserRole,
              ErrorCode.INSUFFICIENT_PERMISSIONS);
        this.requiredPermission = requiredPermission;
        this.currentUserRole = currentUserRole;
    }

    public String getRequiredPermission() { return requiredPermission; }
    public String getCurrentUserRole() { return currentUserRole; }
}

/**
 * Service class demonstrating proper exception usage and documentation.
 */
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    /**
     * Creates a new user in the system.
     *
     * @param userRequest the user creation request containing user details
     * @param createdBy the ID of the user creating this user
     * @return the created user with generated ID
     * @throws UserValidationException if the user request contains invalid data
     * @throws UserPermissionException if the creating user lacks permission to create users
     * @throws UserManagementException if user creation fails due to system errors
     * @throws IllegalArgumentException if userRequest or createdBy is null
     */
    public User createUser(UserCreateRequest userRequest, String createdBy)
            throws UserValidationException, UserPermissionException, UserManagementException {

        // Input validation with specific exceptions
        if (userRequest == null) {
            throw new IllegalArgumentException("User request cannot be null");
        }
        if (createdBy == null) {
            throw new IllegalArgumentException("Created by user ID cannot be null");
        }

        try {
            // Validate permissions
            validateCreateUserPermission(createdBy);

            // Validate user data
            List<String> validationErrors = validateUserRequest(userRequest);
            if (!validationErrors.isEmpty()) {
                throw new UserValidationException("User validation failed",
                                                userRequest.getEmail(), validationErrors);
            }

            // Check for duplicate email
            if (userRepository.existsByEmail(userRequest.getEmail())) {
                throw new UserValidationException("Email already exists",
                                                userRequest.getEmail(),
                                                List.of("Email address is already registered"));
            }

            // Create user
            User user = new User(userRequest);
            user.setCreatedBy(createdBy);
            user.setCreatedAt(Instant.now());

            return userRepository.save(user);

        } catch (PermissionCheckException e) {
            throw new UserPermissionException("CREATE_USER", getCurrentUserRole(createdBy));

        } catch (DatabaseException e) {
            logger.error("Database error creating user for request: {}", userRequest.getEmail(), e);
            throw new UserManagementException("Failed to create user due to system error",
                                           ErrorCode.SYSTEM_ERROR, userRequest.getEmail(), e);

        } catch (Exception e) {
            logger.error("Unexpected error creating user: {}", userRequest.getEmail(), e);
            throw new UserManagementException("Unexpected error during user creation",
                                           ErrorCode.SYSTEM_ERROR, userRequest.getEmail(), e);
        }
    }

    /**
     * Retrieves a user by their ID.
     *
     * @param userId the ID of the user to retrieve
     * @param requestingUserId the ID of the user making the request
     * @return the requested user
     * @throws UserNotFoundException if no user exists with the given ID
     * @throws UserPermissionException if the requesting user cannot access the requested user
     * @throws UserManagementException if retrieval fails due to system errors
     * @throws IllegalArgumentException if userId or requestingUserId is null or invalid
     */
    public User getUserById(String userId, String requestingUserId)
            throws UserNotFoundException, UserPermissionException, UserManagementException {

        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        if (requestingUserId == null || requestingUserId.trim().isEmpty()) {
            throw new IllegalArgumentException("Requesting user ID cannot be null or empty");
        }

        try {
            // Check if user exists
            User user = userRepository.findById(userId);
            if (user == null) {
                throw new UserNotFoundException(userId);
            }

            // Check permissions
            if (!canAccessUser(requestingUserId, userId)) {
                throw new UserPermissionException("READ_USER", getCurrentUserRole(requestingUserId));
            }

            return user;

        } catch (UserNotFoundException | UserPermissionException e) {
            // Re-throw business exceptions as-is
            throw e;

        } catch (DatabaseException e) {
            logger.error("Database error retrieving user: {}", userId, e);
            throw new UserManagementException("Failed to retrieve user due to system error",
                                           ErrorCode.SYSTEM_ERROR, userId, e);

        } catch (Exception e) {
            logger.error("Unexpected error retrieving user: {}", userId, e);
            throw new UserManagementException("Unexpected error during user retrieval",
                                           ErrorCode.SYSTEM_ERROR, userId, e);
        }
    }

    private List<String> validateUserRequest(UserCreateRequest request) {
        List<String> errors = new ArrayList<>();

        if (request.getEmail() == null || !isValidEmail(request.getEmail())) {
            errors.add("Invalid email address");
        }

        if (request.getFirstName() == null || request.getFirstName().trim().isEmpty()) {
            errors.add("First name is required");
        }

        if (request.getLastName() == null || request.getLastName().trim().isEmpty()) {
            errors.add("Last name is required");
        }

        return errors;
    }

    private boolean isValidEmail(String email) {
        return email.contains("@") && email.contains(".");
    }

    private void validateCreateUserPermission(String userId) throws PermissionCheckException {
        // Implementation for permission checking
    }

    private String getCurrentUserRole(String userId) {
        // Implementation to get user role
        return "USER";
    }

    private boolean canAccessUser(String requestingUserId, String targetUserId) {
        // Implementation for access control
        return requestingUserId.equals(targetUserId);
    }
}

/**
 * Error codes for categorizing different types of user management failures.
 */
public enum ErrorCode {
    VALIDATION_FAILED("VALIDATION_FAILED", "Input validation failed"),
    USER_NOT_FOUND("USER_NOT_FOUND", "Requested user not found"),
    INSUFFICIENT_PERMISSIONS("INSUFFICIENT_PERMISSIONS", "Insufficient permissions for operation"),
    DUPLICATE_EMAIL("DUPLICATE_EMAIL", "Email address already exists"),
    SYSTEM_ERROR("SYSTEM_ERROR", "System error occurred");

    private final String code;
    private final String description;

    ErrorCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }
}
```

**Bad example:**

```java
// AVOID: Poor custom exception design and documentation
// BAD: Generic exception without semantic meaning
public class UserException extends Exception {
    public UserException(String message) {
        super(message);
    }
}

// BAD: Runtime exception for recoverable business logic
public class UserError extends RuntimeException {
    public UserError() {
        super();
    }
}

// BAD: Exception without context or useful information
public class ValidationFailed extends Exception {
}

public class BadUserService {

    // BAD: Poor exception documentation and handling
    public User createUser(UserRequest request) throws Exception {
        // BAD: Generic throws Exception clause

        if (request.getEmail() == null) {
            // BAD: No context about what validation failed
            throw new Exception("Invalid");
        }

        try {
            return userRepository.save(new User(request));
        } catch (Exception e) {
            // BAD: Catching and rethrowing generic exception
            throw new Exception("Failed");
        }
    }

    // BAD: No exception documentation
    public User getUserById(String userId) throws UserException {
        User user = userRepository.findById(userId);

        if (user == null) {
            // BAD: No context about which user wasn't found
            throw new UserException("Not found");
        }

        return user;
    }

    // BAD: Using runtime exceptions for business logic
    public void deleteUser(String userId) {
        User user = userRepository.findById(userId);

        if (user == null) {
            // BAD: Runtime exception for expected business condition
            throw new RuntimeException("User doesn't exist: " + userId);
        }

        if (user.hasActiveOrders()) {
            // BAD: Different exception types for similar business conditions
            throw new IllegalStateException("Cannot delete user with active orders");
        }

        userRepository.delete(user);
    }

    // BAD: Inconsistent exception handling
    public List<User> getUsers(String department) throws Exception {
        try {
            if (department == null) {
                // BAD: IllegalArgumentException for some validations
                throw new IllegalArgumentException("Department required");
            }

            List<User> users = userRepository.findByDepartment(department);

            if (users.isEmpty()) {
                // BAD: Different exception type for similar condition
                throw new UserException("No users found");
            }

            return users;

        } catch (DatabaseException e) {
            // BAD: Converting specific exception to generic
            throw new Exception("Database problem");
        }
    }

    // BAD: No exception hierarchy or categorization
    public void updateUserEmail(String userId, String newEmail) throws Exception {
        // No validation...

        try {
            User user = userRepository.findById(userId);
            user.setEmail(newEmail);
            userRepository.save(user);
        } catch (Exception e) {
            // BAD: All errors become the same generic exception
            throw new Exception("Update failed: " + e.getMessage());
        }
    }

    // BAD: Mixing checked and unchecked exceptions inconsistently
    public User authenticateUser(String email, String password)
            throws UserException { // Checked exception

        if (email == null) {
            // BAD: RuntimeException mixed with checked exceptions
            throw new NullPointerException("Email cannot be null");
        }

        User user = findByEmail(email);

        if (user == null) {
            // BAD: Inconsistent exception types for authentication
            throw new UserException("Authentication failed");
        }

        if (!passwordMatches(password, user.getPasswordHash())) {
            // BAD: Different exception for same logical condition
            throw new SecurityException("Invalid password");
        }

        return user;
    }
}
```


### Example 7: Use Exceptions Only for Exceptional Conditions

Title: Don't use exceptions for ordinary control flow
Description: Exceptions should be used for exceptional conditions, not for normal program flow. They are expensive and make code harder to understand. Use regular control structures for predictable conditions.

**Good example:**

```java
// GOOD: Normal control flow for array processing
public class NumberProcessor {
    public void processNumbers(int[] numbers) {
        for (int number : numbers) {  // Normal iteration
            if (isValid(number)) {    // Normal condition checking
                process(number);
            } else {
                logger.warn("Skipping invalid number: {}", number);
            }
        }
    }

    private boolean isValid(int number) {
        return number >= 0 && number <= 1000;
    }

    private void process(int number) {
        // Process the number
        logger.info("Processing: {}", number);
    }
}
```

**Bad example:**

```java
// AVOID: Using exceptions for normal control flow
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
