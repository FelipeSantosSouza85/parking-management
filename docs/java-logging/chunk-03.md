# java-logging

> Source: 126-java-logging.md
> Chunk: 3/4
> Included sections: Examples - Example 5: Implement Secure Logging Practices

### Example 5: Implement Secure Logging Practices

Title: Ensure Logs Do Not Compromise Security
Description: Actively mask or filter sensitive data, control access to log files, use secure transmission protocols, and comply with data protection regulations when logging information.

**Good example:**

```java
// GOOD: Secure logging practices
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Pattern;
import java.util.Objects;

public class SecureLoggingService {
    private static final Logger logger = LoggerFactory.getLogger(SecureLoggingService.class);

    // Patterns for detecting sensitive data
    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile("\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}");
    private static final Pattern SSN_PATTERN = Pattern.compile("\\d{3}-\\d{2}-\\d{4}");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");

    public void processUserRegistration(String username, String email, String ssn, String creditCard) {
        // GOOD: Sanitize all user inputs before logging
        String sanitizedUsername = sanitizeForLogging(username);
        String maskedEmail = maskEmail(email);
        String hashedSSN = hashSensitiveData(ssn);
        String maskedCard = maskCreditCard(creditCard);

        logger.info("Processing registration for user: {}, email: {}", sanitizedUsername, maskedEmail);
        logger.debug("User data validation - SSN hash: {}, card mask: {}", hashedSSN, maskedCard);

        try {
            validateUserData(username, email, ssn, creditCard);
            createUserAccount(username, email);

            // GOOD: Log successful operation without sensitive data
            logger.info("User registration completed successfully for: {}", sanitizedUsername);

            // GOOD: Security event logging for audit trail
            logger.info("SECURITY_EVENT: New user registration - username: {}, timestamp: {}",
                       sanitizedUsername, System.currentTimeMillis());

        } catch (ValidationException e) {
            // GOOD: Log validation failure without exposing sensitive validation details
            logger.warn("User registration failed validation for user: {}, error code: {}",
                       sanitizedUsername, e.getErrorCode());

        } catch (SecurityException e) {
            // GOOD: Security incidents logged with careful information control
            logger.error("SECURITY_ALERT: Registration security violation for user: {}, violation type: {}",
                        sanitizeForLogging(username), e.getViolationType());
            // Don't log the actual violation details that might help attackers

        } catch (Exception e) {
            // GOOD: Generic error logging without sensitive context
            logger.error("User registration failed for user: {} due to system error",
                        sanitizedUsername, e);
        }
    }

    public void processPayment(String userId, PaymentRequest request) {
        String correlationId = generateSecureCorrelationId();

        try {
            // GOOD: Log business operation with masked sensitive data
            logger.info("Processing payment - user: {}, amount: {}, correlation: {}",
                       userId, request.getAmount(), correlationId);

            // GOOD: Validate and log without exposing card details
            validatePaymentMethod(request.getPaymentMethod());
            logger.debug("Payment method validated for correlation: {}", correlationId);

            ChargeResult result = processCharge(request);

            // GOOD: Log success with minimal necessary information
            logger.info("Payment processed successfully - correlation: {}, transaction: {}",
                       correlationId, result.getTransactionId());

            // GOOD: Business intelligence logging (aggregated, non-sensitive)
            logger.info("METRICS: Payment processed - amount: {}, merchant: {}, timestamp: {}",
                       request.getAmount(), request.getMerchantId(), System.currentTimeMillis());

        } catch (FraudDetectedException e) {
            // GOOD: Security event with controlled information
            logger.error("SECURITY_ALERT: Fraud detected - correlation: {}, risk_score: {}, user: {}",
                        correlationId, e.getRiskScore(), userId);
            // Don't log fraud detection details that could help bypass detection

        } catch (PaymentException e) {
            // GOOD: Business error with safe context
            logger.error("Payment failed - correlation: {}, error_type: {}",
                        correlationId, e.getErrorType(), e);
        }
    }

    // Secure data masking utilities
    private String sanitizeForLogging(String input) {
        if (input == null) return "[null]";

        // Remove potentially dangerous characters for log injection protection
        String sanitized = input.replaceAll("[\r\n\t]", "_")
                               .replaceAll("[<>\"'&]", "*");

        // Limit length to prevent log flooding
        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 97) + "...";
        }

        return sanitized;
    }

    private String maskEmail(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            return "[INVALID_EMAIL]";
        }

        int atIndex = email.indexOf('@');
        if (atIndex < 2) {
            return "**@" + email.substring(atIndex + 1);
        }

        return email.substring(0, 2) + "***@" + email.substring(atIndex + 1);
    }

    private String maskCreditCard(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 8) {
            return "[INVALID_CARD]";
        }

        String digitsOnly = cardNumber.replaceAll("[^\\d]", "");
        if (digitsOnly.length() < 8) {
            return "[INVALID_CARD]";
        }

        return digitsOnly.substring(0, 4) + "****" +
               digitsOnly.substring(digitsOnly.length() - 4);
    }

    private String hashSensitiveData(String data) {
        if (data == null) return "[null]";

        // Use a secure hash for audit purposes (not for security)
        return "HASH_" + Math.abs(data.hashCode());
    }

    private String generateSecureCorrelationId() {
        return "CORR_" + System.currentTimeMillis() + "_" +
               Thread.currentThread().getId();
    }

    // Mock classes and methods
    private void validateUserData(String username, String email, String ssn, String creditCard)
            throws ValidationException {
        if (username == null || username.trim().isEmpty()) {
            throw new ValidationException("INVALID_USERNAME");
        }
    }

    private void createUserAccount(String username, String email) {
        // Account creation logic
    }

    private void validatePaymentMethod(String paymentMethod) {
        // Payment validation logic
    }

    private ChargeResult processCharge(PaymentRequest request) throws PaymentException {
        // Payment processing logic
        return new ChargeResult("TXN_" + System.currentTimeMillis());
    }

    // Mock classes
    private static class PaymentRequest {
        private String amount = "100.00";
        private String paymentMethod = "card";
        private String merchantId = "MERCHANT_123";

        public String getAmount() { return amount; }
        public String getPaymentMethod() { return paymentMethod; }
        public String getMerchantId() { return merchantId; }
    }

    private static class ChargeResult {
        private final String transactionId;
        public ChargeResult(String transactionId) { this.transactionId = transactionId; }
        public String getTransactionId() { return transactionId; }
    }

    private static class ValidationException extends Exception {
        private final String errorCode;
        public ValidationException(String errorCode) {
            super("Validation failed: " + errorCode);
            this.errorCode = errorCode;
        }
        public String getErrorCode() { return errorCode; }
    }

    private static class SecurityException extends Exception {
        private final String violationType;
        public SecurityException(String violationType) {
            super("Security violation: " + violationType);
            this.violationType = violationType;
        }
        public String getViolationType() { return violationType; }
    }

    private static class FraudDetectedException extends Exception {
        private final int riskScore;
        public FraudDetectedException(int riskScore) {
            super("Fraud detected with risk score: " + riskScore);
            this.riskScore = riskScore;
        }
        public int getRiskScore() { return riskScore; }
    }

    private static class PaymentException extends Exception {
        private final String errorType;
        public PaymentException(String errorType) {
            super("Payment error: " + errorType);
            this.errorType = errorType;
        }
        public String getErrorType() { return errorType; }
    }
}
```

**Bad example:**

```java
// AVOID: Insecure logging practices
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InsecureLoggingService {
    private static final Logger logger = LoggerFactory.getLogger(InsecureLoggingService.class);

    public void processUserRegistration(String username, String password, String email,
                                      String ssn, String creditCard) {
        // BAD: Logging passwords - NEVER do this!
        logger.debug("User registration: username={}, password={}, email={}",
                    username, password, email);

        // BAD: Logging full SSN and credit card numbers
        logger.info("Processing registration for SSN: {} with credit card: {}", ssn, creditCard);

        try {
            validateUser(username, password, email, ssn, creditCard);
            createAccount(username, password);

        } catch (ValidationException e) {
            // BAD: Exposing sensitive validation details
            logger.error("Validation failed for user {} with password {} - details: {}",
                        username, password, e.getValidationDetails());

        } catch (SecurityException e) {
            // BAD: Logging detailed security information that could help attackers
            logger.error("Security violation for user {}: attack vector: {}, payload: {}, " +
                        "system_path: {}, internal_error: {}",
                        username, e.getAttackVector(), e.getPayload(),
                        e.getSystemPath(), e.getInternalError());
        }
    }

    public void processPayment(String userId, String cardNumber, String cvv, String amount) {
        // BAD: Logging complete payment card information
        logger.info("Processing payment: user={}, card={}, cvv={}, amount={}",
                   userId, cardNumber, cvv, amount);

        try {
            validateCard(cardNumber, cvv);

            // BAD: Logging sensitive database connection info
            logger.debug("Connecting to payment DB with connection string: {}",
                        getDatabaseConnectionString());

            processTransaction(cardNumber, cvv, amount);

        } catch (FraudException e) {
            // BAD: Exposing fraud detection algorithms and thresholds
            logger.error("Fraud detected for card {}: algorithm={}, threshold={}, " +
                        "risk_factors={}, detection_rules={}",
                        cardNumber, e.getAlgorithm(), e.getThreshold(),
                        e.getRiskFactors(), e.getDetectionRules());

        } catch (PaymentException e) {
            // BAD: Including full stack trace with sensitive system information
            logger.error("Payment failed for card {} with full system details", cardNumber, e);
        }
    }

    public void handleSystemError(String operation, Exception e) {
        // BAD: Logging system internals that could help attackers
        logger.error("System error in operation {}: java_version={}, system_properties={}, " +
                    "environment_variables={}, file_paths={}",
                    operation, System.getProperty("java.version"),
                    System.getProperties(), System.getenv(),
                    System.getProperty("user.dir"));

        // BAD: Full stack trace might expose sensitive system information
        e.printStackTrace(); // Goes to stderr, not controlled by logging config
    }

    public void logUserActivity(String userId, String sessionId, String ipAddress,
                               String userAgent, String requestData) {
        // BAD: Logging PII and potentially sensitive request data
        logger.info("User activity: user={}, session={}, ip={}, userAgent={}, request={}",
                   userId, sessionId, ipAddress, userAgent, requestData);

        // BAD: No data retention consideration
        // This could violate GDPR, CCPA, or other privacy regulations
    }

    public void authenticateUser(String username, String password, String loginToken) {
        // BAD: Logging authentication credentials
        logger.debug("Authentication attempt: username={}, password={}, token={}",
                    username, password, loginToken);

        try {
            boolean success = authenticate(username, password, loginToken);
            if (!success) {
                // BAD: Detailed failure information that could help brute force attacks
                logger.warn("Login failed for {} - password_attempts={}, last_success={}, " +
                           "account_locked={}, failed_reasons={}",
                           username, getPasswordAttempts(username),
                           getLastSuccessfulLogin(username), isAccountLocked(username),
                           getFailureReasons(username));
            }
        } catch (Exception e) {
            // BAD: Logging authentication errors with sensitive context
            logger.error("Authentication system error for user {} with credentials: " +
                        "password={}, token={}", username, password, loginToken, e);
        }
    }

    // Mock methods with security issues
    private String getDatabaseConnectionString() {
        return "jdbc:mysql://prod-db:3306/payments?user=admin&password=secret123";
    }

    private void validateUser(String username, String password, String email, String ssn, String creditCard)
            throws ValidationException { /* ... */ }
    private void createAccount(String username, String password) { /* ... */ }
    private void validateCard(String cardNumber, String cvv) { /* ... */ }
    private void processTransaction(String cardNumber, String cvv, String amount)
            throws PaymentException { /* ... */ }
    private boolean authenticate(String username, String password, String token) { return true; }
    private int getPasswordAttempts(String username) { return 3; }
    private long getLastSuccessfulLogin(String username) { return System.currentTimeMillis(); }
    private boolean isAccountLocked(String username) { return false; }
    private String getFailureReasons(String username) { return "invalid_password"; }

    // Exception classes with sensitive information exposure
    private static class ValidationException extends Exception {
        private final String validationDetails;
        public ValidationException(String details) {
            this.validationDetails = details;
        }
        public String getValidationDetails() { return validationDetails; }
    }

    private static class SecurityException extends Exception {
        private final String attackVector, payload, systemPath, internalError;
        public SecurityException(String attackVector, String payload, String systemPath, String internalError) {
            this.attackVector = attackVector;
            this.payload = payload;
            this.systemPath = systemPath;
            this.internalError = internalError;
        }
        public String getAttackVector() { return attackVector; }
        public String getPayload() { return payload; }
        public String getSystemPath() { return systemPath; }
        public String getInternalError() { return internalError; }
    }

    private static class FraudException extends Exception {
        private final String algorithm, threshold, riskFactors, detectionRules;
        public FraudException(String algorithm, String threshold, String riskFactors, String detectionRules) {
            this.algorithm = algorithm;
            this.threshold = threshold;
            this.riskFactors = riskFactors;
            this.detectionRules = detectionRules;
        }
        public String getAlgorithm() { return algorithm; }
        public String getThreshold() { return threshold; }
        public String getRiskFactors() { return riskFactors; }
        public String getDetectionRules() { return detectionRules; }
    }

    private static class PaymentException extends Exception {
        public PaymentException(String message) { super(message); }
    }
}
```
