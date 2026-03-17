# java-exception-handling

> Source: 127-java-exception-handling.md
> Chunk: 2/6
> Included sections: Examples - Example 3: Secure Exception Handling | Examples - Example 4: Exception Chaining and Context Preservation

### Example 3: Secure Exception Handling

Title: Protect Sensitive Information While Enabling Debugging
Description: Handle exceptions securely by logging detailed diagnostic information for developers while providing only generic, safe error messages to users. Never expose sensitive system details, file paths, database schemas, or internal implementation details in user-facing error messages.

**Good example:**

```java
// GOOD: Secure exception handling with proper information separation
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecureService {
    private static final Logger logger = LoggerFactory.getLogger(SecureService.class);

    /**
     * Processes user authentication securely.
     *
     * @param username the username to authenticate
     * @param password the password to verify
     * @return authentication result
     * @throws AuthenticationException if authentication fails
     */
    public AuthResult authenticate(String username, String password) throws AuthenticationException {
        try {
            validateCredentials(username, password);
            User user = userRepository.findByUsername(username);

            if (user == null || !passwordEncoder.matches(password, user.getHashedPassword())) {
                // Log detailed info for security monitoring
                logger.warn("Authentication failed for username: {} from IP: {}",
                           username, getCurrentClientIP());

                // Generic message - don't reveal which part failed
                throw new AuthenticationException("Invalid credentials");
            }

            logger.info("Successful authentication for user: {}", username);
            return new AuthResult(user, generateToken(user));

        } catch (DatabaseException e) {
            // Log technical details for developers
            logger.error("Database error during authentication for user: {}", username, e);

            // Don't expose database details to user
            throw new AuthenticationException("Authentication service temporarily unavailable");

        } catch (Exception e) {
            // Log unexpected errors with full context
            logger.error("Unexpected error during authentication for user: {}", username, e);

            // Generic error message
            throw new AuthenticationException("Authentication failed");
        }
    }

    /**
     * Processes sensitive data with secure error handling.
     *
     * @param dataId the identifier of data to process
     * @return processing result
     * @throws ProcessingException if processing fails
     */
    public ProcessingResult processData(String dataId) throws ProcessingException {
        try {
            validateDataAccess(dataId);
            SensitiveData data = dataRepository.findById(dataId);

            return performProcessing(data);

        } catch (UnauthorizedException e) {
            // Log security event with context
            logger.warn("Unauthorized access attempt to data: {} by user: {}",
                       dataId, getCurrentUser(), e);

            // Standard security message
            throw new ProcessingException("Access denied", ErrorCode.FORBIDDEN);

        } catch (ValidationException e) {
            // Log validation failure details
            logger.debug("Validation failed for data: {}, reason: {}", dataId, e.getMessage());

            // Safe validation message
            throw new ProcessingException("Invalid data format", ErrorCode.BAD_REQUEST);

        } catch (SystemException e) {
            // Log system errors with correlation ID for tracking
            String correlationId = generateCorrelationId();
            logger.error("System error processing data: {} [correlation: {}]", dataId, correlationId, e);

            // Generic error with correlation ID for support
            throw new ProcessingException("System error occurred. Reference: " + correlationId,
                                        ErrorCode.INTERNAL_ERROR);
        }
    }

    // Secure error response builder
    public ErrorResponse buildErrorResponse(ProcessingException e, String requestId) {
        return ErrorResponse.builder()
            .message(e.getMessage()) // Safe message only
            .errorCode(e.getErrorCode().getCode())
            .timestamp(Instant.now())
            .requestId(requestId) // For tracking, not sensitive
            .build();
    }
}

// Safe error codes enum
public enum ErrorCode {
    BAD_REQUEST(400, "Bad Request"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not Found"),
    INTERNAL_ERROR(500, "Internal Server Error");

    private final int httpStatus;
    private final String message;

    ErrorCode(int httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public int getHttpStatus() { return httpStatus; }
    public String getMessage() { return message; }
    public String getCode() { return name(); }
}
```

**Bad example:**

```java
// AVOID: Insecure exception handling that exposes sensitive information
public class InsecureService {

    // BAD: Exposing sensitive system information
    public AuthResult authenticate(String username, String password) throws Exception {
        try {
            User user = userRepository.findByUsername(username);

            if (user == null) {
                // BAD: Reveals that username doesn't exist
                throw new Exception("User '" + username + "' not found in database table 'users'");
            }

            if (!user.getPassword().equals(password)) {
                // BAD: Reveals that username exists but password is wrong
                throw new Exception("Invalid password for user '" + username + "'");
            }

            return new AuthResult(user, generateToken(user));

        } catch (SQLException e) {
            // TERRIBLE: Exposing database connection details
            throw new Exception("Database connection failed to host db.internal.company.com:5432, " +
                              "database 'userdb', table 'users': " + e.getMessage(), e);
        }
    }

    // BAD: Exposing full stack traces to users
    public ProcessingResult processData(String dataId) throws Exception {
        try {
            // Processing logic...
            return performProcessing(dataId);
        } catch (Exception e) {
            // TERRIBLE: Full stack trace in response
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);

            throw new Exception("Processing failed:\n" + sw.toString());
        }
    }

    // BAD: Logging sensitive data
    public void processPayment(PaymentRequest request) throws Exception {
        try {
            // Process payment...
        } catch (Exception e) {
            // TERRIBLE: Logging sensitive payment information
            System.out.println("Payment failed for card: " + request.getCardNumber() +
                             ", CVV: " + request.getCvv() +
                             ", amount: " + request.getAmount());
            e.printStackTrace(); // Stack trace in logs

            // TERRIBLE: Exposing sensitive details in exception
            throw new Exception("Payment failed for card ending in " +
                              request.getCardNumber().substring(12) +
                              " with error: " + e.getMessage());
        }
    }

    // BAD: Different error messages reveal system state
    public UserProfile getUserProfile(String userId) throws Exception {
        User user = userDatabase.findById(userId);

        if (user == null) {
            throw new Exception("User with ID " + userId + " does not exist in system");
        }

        if (!user.isActive()) {
            throw new Exception("User account " + userId + " is deactivated");
        }

        if (user.getProfile() == null) {
            throw new Exception("User " + userId + " has no profile data in profile_table");
        }

        // This reveals information about system state and database structure
        return user.getProfile();
    }
}
```

### Example 4: Exception Chaining and Context Preservation

Title: Maintain Full Error Context for Effective Debugging
Description: Use exception chaining to preserve the original exception context while adding higher-level semantic meaning. This provides the full error context needed for debugging while allowing different layers of the application to handle errors appropriately.

**Good example:**

```java
// GOOD: Proper exception chaining with context preservation
public class LayeredService {
    private static final Logger logger = LoggerFactory.getLogger(LayeredService.class);

    // Custom exception hierarchy with chaining support
    public static class ServiceException extends Exception {
        public ServiceException(String message) {
            super(message);
        }

        public ServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class DataAccessException extends ServiceException {
        public DataAccessException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class BusinessLogicException extends ServiceException {
        public BusinessLogicException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // Data layer - preserves original database exceptions
    public User findUserById(Long userId) throws DataAccessException {
        try {
            return userRepository.findById(userId);

        } catch (SQLException e) {
            // Chain the SQL exception with business context
            throw new DataAccessException("Failed to retrieve user with ID: " + userId, e);

        } catch (ConnectionException e) {
            // Chain connection issues
            throw new DataAccessException("Database connection failed while fetching user: " + userId, e);
        }
    }

    // Service layer - adds business context while preserving technical details
    public UserProfile getUserProfile(Long userId) throws BusinessLogicException {
        try {
            if (userId == null || userId <= 0) {
                throw new IllegalArgumentException("User ID must be positive, was: " + userId);
            }

            User user = findUserById(userId);

            if (user == null) {
                throw new UserNotFoundException("User not found with ID: " + userId);
            }

            if (!user.isActive()) {
                throw new UserInactiveException("User account is inactive: " + userId);
            }

            return buildUserProfile(user);

        } catch (DataAccessException e) {
            // Chain data access errors with business context
            throw new BusinessLogicException("Unable to retrieve user profile for ID: " + userId, e);

        } catch (IllegalArgumentException e) {
            // Chain validation errors
            throw new BusinessLogicException("Invalid user ID provided: " + userId, e);

        } catch (Exception e) {
            // Chain unexpected errors
            logger.error("Unexpected error retrieving user profile: {}", userId, e);
            throw new BusinessLogicException("Unexpected error occurred while retrieving user profile", e);
        }
    }

    // Controller layer - handles service exceptions appropriately
    public ResponseEntity<UserProfileDto> handleGetUserProfile(Long userId) {
        try {
            UserProfile profile = getUserProfile(userId);
            return ResponseEntity.ok(convertToDto(profile));

        } catch (BusinessLogicException e) {
            // Log the full chain for debugging
            logger.error("Business logic error for user profile request: {}", userId, e);

            // Determine response based on root cause
            Throwable rootCause = getRootCause(e);

            if (rootCause instanceof UserNotFoundException) {
                return ResponseEntity.notFound().build();
            } else if (rootCause instanceof UserInactiveException) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            } else if (rootCause instanceof IllegalArgumentException) {
                return ResponseEntity.badRequest().build();
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

    // Utility to find root cause in exception chain
    private Throwable getRootCause(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }

    // Example of rethrowing with additional context
    public void processUserData(User user) throws ProcessingException {
        try {
            validateUser(user);
            transformUserData(user);
            persistUser(user);

        } catch (ValidationException e) {
            // Add context while preserving original exception
            throw new ProcessingException("User data validation failed for user: " + user.getId(), e);

        } catch (TransformationException e) {
            // Chain transformation errors
            throw new ProcessingException("Failed to transform user data: " + user.getId(), e);

        } catch (PersistenceException e) {
            // Chain persistence errors
            throw new ProcessingException("Failed to save user data: " + user.getId(), e);
        }
    }
}
```

**Bad example:**

```java
// AVOID: Poor exception handling that loses context
public class BadLayeredService {

    // BAD: Losing original exception context
    public User findUserById(Long userId) throws Exception {
        try {
            return userRepository.findById(userId);
        } catch (SQLException e) {
            // TERRIBLE: Original exception is lost
            throw new Exception("Database error");
        }
    }

    // BAD: Generic exception handling loses specific error information
    public UserProfile getUserProfile(Long userId) throws Exception {
        try {
            User user = findUserById(userId);
            return buildUserProfile(user);
        } catch (Exception e) {
            // BAD: All specific error context is lost
            throw new Exception("Failed to get user profile");
        }
    }

    // BAD: Swallowing exceptions entirely
    public UserProfile getUserProfileSilent(Long userId) {
        try {
            User user = findUserById(userId);
            return buildUserProfile(user);
        } catch (Exception e) {
            // TERRIBLE: Exception completely swallowed
            e.printStackTrace(); // Poor logging
            return null; // Hiding the error
        }
    }

    // BAD: Creating new exceptions without chaining
    public void processUserData(User user) throws ProcessingException {
        try {
            validateUser(user);
            transformUserData(user);
            persistUser(user);
        } catch (ValidationException e) {
            // BAD: Original exception information is lost
            throw new ProcessingException("Validation failed");
        } catch (Exception e) {
            // BAD: Generic handling without context
            throw new ProcessingException("Processing failed");
        }
    }

    // BAD: Catching and rethrowing without adding value
    public String processRequest(String request) throws ServiceException {
        try {
            return businessLogic.process(request);
        } catch (BusinessException e) {
            // BAD: Catching just to rethrow without adding context
            throw e;
        } catch (Exception e) {
            // BAD: Unnecessary wrapping without semantic value
            throw new ServiceException(e.getMessage());
        }
    }

    // BAD: Multiple exception handling points that lose context
    public Result performComplexOperation(String input) {
        try {
            String processed = preprocessInput(input);

            try {
                String validated = validateInput(processed);

                try {
                    return executeOperation(validated);
                } catch (ExecutionException e) {
                    // BAD: Nested try-catch losing context
                    return Result.failure("Execution failed");
                }
            } catch (ValidationException e) {
                // BAD: Each level loses more context
                return Result.failure("Validation failed");
            }
        } catch (Exception e) {
            // BAD: Original error completely lost
            return Result.failure("Operation failed");
        }
    }
}
```
