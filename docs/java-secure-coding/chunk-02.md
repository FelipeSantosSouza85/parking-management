# java-secure-coding

> Source: 124-java-secure-coding.md
> Chunk: 2/2
> Included sections: Examples - Example 4: Use Strong Cryptography | Examples - Example 5: Handle Exceptions Securely | Examples - Example 6: Secrets Management | Examples - Example 7: Safe Deserialization | Examples - Example 8: Prevent Cross-Site Scripting (XSS) | Output Format | Safeguards

### Example 4: Use Strong Cryptography

Title: Employ Current and Robust Cryptographic Algorithms
Description: Use well-vetted, industry-standard cryptographic libraries and algorithms for hashing, encryption, and digital signatures. Avoid deprecated or weak algorithms (e.g., MD5, SHA1 for passwords, DES). Keep cryptographic keys secure and manage them properly.

**Good example:**

```java
// GOOD: Strong cryptographic practices
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class SecureCryptoUtils {
    private static final String AES_ALGORITHM = "AES";
    private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 16;
    private static final int GCM_IV_LENGTH = 12;

    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom;

    public SecureCryptoUtils() {
        this.passwordEncoder = new BCryptPasswordEncoder(12); // Strong cost factor
        this.secureRandom = new SecureRandom();
    }

    /**
     * Generates a secure AES-256 key
     */
    public SecretKey generateAESKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance(AES_ALGORITHM);
        keyGen.init(256, secureRandom); // Use AES-256
        return keyGen.generateKey();
    }

    /**
     * Encrypts data using AES-GCM (authenticated encryption)
     */
    public EncryptedData encrypt(String plaintext, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);

        // Generate random IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        secureRandom.nextBytes(iv);

        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);

        byte[] encryptedData = cipher.doFinal(plaintext.getBytes("UTF-8"));

        return new EncryptedData(encryptedData, iv);
    }

    /**
     * Hashes password using BCrypt with salt
     */
    public String hashPassword(String plainTextPassword) {
        return passwordEncoder.encode(plainTextPassword);
    }

    /**
     * Verifies password against BCrypt hash
     */
    public boolean verifyPassword(String plainTextPassword, String hashedPassword) {
        return passwordEncoder.matches(plainTextPassword, hashedPassword);
    }

    /**
     * Generates cryptographically secure random token
     */
    public String generateSecureToken(int length) {
        byte[] tokenBytes = new byte[length];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getEncoder().encodeToString(tokenBytes);
    }

    public static class EncryptedData {
        private final byte[] ciphertext;
        private final byte[] iv;

        public EncryptedData(byte[] ciphertext, byte[] iv) {
            this.ciphertext = ciphertext;
            this.iv = iv;
        }

        // Getters...
    }
}
```

**Bad example:**

```java
// AVOID: Weak cryptographic practices
import java.security.MessageDigest;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Random;

public class WeakCryptoUtils {

    /**
     * BAD: Using MD5 for password hashing
     */
    public String hashPasswordMD5(String password) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(password.getBytes());
        byte[] digest = md.digest();
        return bytesToHex(digest);
    }

    /**
     * BAD: Using SHA1 for password hashing (without salt)
     */
    public String hashPasswordSHA1(String password) throws Exception {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        sha1.update(password.getBytes());
        return bytesToHex(sha1.digest());
    }

    /**
     * BAD: Using DES encryption (weak algorithm)
     */
    public byte[] encryptDES(String plaintext, String password) throws Exception {
        SecretKeySpec key = new SecretKeySpec(password.getBytes(), "DES");
        Cipher cipher = Cipher.getInstance("DES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(plaintext.getBytes());
    }

    /**
     * BAD: Hardcoded encryption key
     */
    private static final String HARDCODED_KEY = "mySecretKey123";

    public byte[] encryptWithHardcodedKey(String data) throws Exception {
        // TERRIBLE: Hardcoded key in source code
        SecretKeySpec key = new SecretKeySpec(HARDCODED_KEY.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(data.getBytes());
    }

    /**
     * BAD: Using weak random number generator
     */
    public String generateWeakToken() {
        Random random = new Random(); // NOT cryptographically secure
        return String.valueOf(random.nextLong());
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}

// Additional bad practices:
// - Storing passwords in plain text
// - Using ECB mode for encryption
// - Not using authenticated encryption
// - Reusing IVs/nonces
// - Using weak key derivation functions
```

### Example 5: Handle Exceptions Securely

Title: Avoid Exposing Sensitive Information
Description: Catch exceptions appropriately, but do not reveal sensitive system details or stack traces to users in production. Log detailed error information server-side for debugging, but provide generic error messages to the client.

**Good example:**

```java
// GOOD: Secure exception handling
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecureExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(SecureExceptionHandler.class);

    public void performSensitiveOperation(String userId, String sensitiveData)
            throws ServiceException {
        try {
            validateUser(userId);
            processData(sensitiveData);

        } catch (UserNotFoundException e) {
            // Log detailed error for debugging
            logger.error("User not found during sensitive operation: userId={}", userId, e);
            // Return generic error to client
            throw new ServiceException("Operation failed", ErrorCode.INVALID_REQUEST);

        } catch (DataProcessingException e) {
            // Log technical details
            logger.error("Data processing failed: userId={}, dataLength={}",
                        userId, sensitiveData.length(), e);
            // Don't expose internal details
            throw new ServiceException("Processing error occurred", ErrorCode.INTERNAL_ERROR);

        } catch (Exception e) {
            // Catch unexpected errors
            logger.error("Unexpected error in sensitive operation: userId={}", userId, e);
            throw new ServiceException("System error", ErrorCode.INTERNAL_ERROR);
        }
    }
}

// Secure error codes without sensitive details
public enum ErrorCode {
    INVALID_REQUEST("Invalid request"),
    UNAUTHORIZED("Unauthorized"),
    FORBIDDEN("Forbidden"),
    NOT_FOUND("Not found"),
    INTERNAL_ERROR("Internal server error");

    private final String message;

    ErrorCode(String message) {
        this.message = message;
    }

    public String getMessage() { return message; }
}
```

**Bad example:**

```java
// AVOID: Exposing sensitive information
import java.io.StringWriter;
import java.io.PrintWriter;

public class InsecureExceptionHandler {

    public void performSensitiveOperation(String userId, String sensitiveData)
            throws Exception {
        try {
            validateUser(userId);
            processData(sensitiveData);
        } catch (Exception e) {
            // BAD: Exposing full exception details to caller
            throw new Exception("Database connection failed: " + e.getMessage() +
                              " at host: db.internal.company.com:5432", e);
        }
    }

    // BAD: Exposing stack traces to users
    public String handleError(Exception e) {
        // TERRIBLE: Exposing full stack trace to client
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);

        return "Error occurred:\n" + sw.toString();
    }

    // BAD: Logging sensitive data
    public void processPayment(String cardNumber, String cvv) {
        try {
            // Process payment...
        } catch (Exception e) {
            // TERRIBLE: Logging sensitive data
            System.out.println("Payment failed for card: " + cardNumber + ", CVV: " + cvv);
            e.printStackTrace(); // Exposing stack trace in logs
        }
    }

    // BAD: Different error messages reveal system information
    public void login(String username, String password) throws Exception {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new Exception("User '" + username + "' does not exist in database");
        }

        if (!user.getPassword().equals(password)) {
            throw new Exception("Invalid password for user '" + username + "'");
        }

        // This tells attackers which usernames exist vs which don't
    }
}
```

### Example 6: Secrets Management

Title: Avoid Hardcoded Secrets and Prevent Leakage
Description: Never hardcode credentials or sensitive tokens. Load secrets at runtime from secure sources (environment variables, secret stores, container secrets), validate presence, and avoid logging them. Enforce least-privilege credentials and rotate regularly.

**Good example:**

```java
// GOOD: Load secrets securely at runtime
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class SecretLoader {
    public static String requireEnv(String name) {
        String value = System.getenv(name);
        if (Objects.isNull(value) || value.isBlank()) {
            throw new IllegalStateException("Missing required secret: " + name);
        }
        return value;
    }

    public static String readDockerSecret(Path mountPath) throws Exception {
        if (!Files.isRegularFile(mountPath)) {
            throw new IllegalStateException("Secret file not found: " + mountPath);
        }
        // Files.readString avoids exposing content in logs; caller must not log it
        return Files.readString(mountPath).trim();
    }

    public static DbConfig buildDbConfig() throws Exception {
        // Prefer env or mounted secrets; never hardcode
        String username = requireEnv("DB_USERNAME");
        String password = readDockerSecret(Path.of("/run/secrets/db_password"));
        String url = requireEnv("DB_URL");
        return new DbConfig(url, username, password);
    }
}

final class DbConfig {
    final String url;
    final String user;
    final String pass;
    DbConfig(String url, String user, String pass) { this.url = url; this.user = user; this.pass = pass; }
}
```

**Bad example:**

```java
// AVOID: Hardcoded and leaked secrets
public class InsecureSecrets {
    // TERRIBLE: committed to VCS
    private static final String DB_PASSWORD = "P@ssw0rd!";

    public void connect() {
        String url = "jdbc:postgresql://db.internal:5432/app";
        String user = "appuser";
        // BAD: Printing secrets to logs
        System.out.println("Connecting with password: " + DB_PASSWORD);
        // ... use DB_PASSWORD ...
    }
}
```

### Example 7: Safe Deserialization

Title: Avoid Native Serialization; Use Strict DTO Binding
Description: Do not deserialize untrusted data using Java native serialization. Prefer JSON/XML binding to explicit DTOs and avoid permissive polymorphic typing. If polymorphism is needed, enforce an allow-list of subtypes and validate inputs before use.

**Good example:**

```java
// GOOD: Bind to explicit DTO without default typing
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SafeJsonBinder {
    private final ObjectMapper mapper = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public UserProfile parseProfile(String json) throws Exception {
        // Bind to a concrete DTO; no polymorphic/DefaultTyping enabled
        return mapper.readValue(json, UserProfile.class);
    }
}

final class UserProfile {
    public String name;
    public String email;
}
```

**Bad example:**

```java
// AVOID: Insecure deserialization
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

public class InsecureDeserializer {
    public Object fromBytes(byte[] data) throws Exception {
        // DANGEROUS: Native deserialization of untrusted data
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {
            return ois.readObject();
        }
    }
}

// Also dangerous: enabling permissive default typing in JSON mappers
// ObjectMapper mapper = new ObjectMapper();
// mapper.enableDefaultTyping(); // deprecated and unsafe for untrusted data
```

### Example 8: Prevent Cross-Site Scripting (XSS)

Title: Use Output Encoding
Description: Always encode user-controlled data when outputting to HTML, JavaScript, or other contexts to prevent XSS attacks.

**Good example:**

```java
// GOOD: Output encoding to prevent XSS
import org.apache.commons.text.StringEscapeUtils;

public class SecureOutput {
    public String renderUserInput(String input) {
        String encoded = StringEscapeUtils.escapeHtml4(input);
        return "<p>" + encoded + "</p>";
    }
}
```

**Bad example:**

```java
// AVOID: No encoding, vulnerable to XSS
public class InsecureOutput {
    public String renderUserInput(String input) {
        return "<p>" + input + "</p>";
    }
}
```

## Output Format

- **ANALYZE** Java code to identify specific security vulnerabilities and categorize them by severity (CRITICAL, HIGH, MEDIUM, LOW) and vulnerability type (injection, authentication, authorization, cryptography, data exposure, secrets management, deserialization, configuration, supply-chain)
- **CATEGORIZE** security improvements found: Input Validation Issues (missing validation vs robust input checking, insufficient sanitization vs comprehensive filtering), Injection Vulnerabilities (dynamic SQL vs parameterized queries, command injection vs safe execution, XSS risks vs proper encoding), Authentication/Authorization Gaps (weak permissions vs principle of least privilege, excessive access vs role-based control), Cryptographic Weaknesses (deprecated algorithms vs modern cryptography, weak key management vs secure key handling), Exception Handling Problems (information disclosure vs secure error messages, sensitive data exposure vs protected logging), Secrets and Configuration Issues (hardcoded credentials vs secret managers, debug modes off in prod), and Deserialization Risks (native Java serialization vs DTO binding)
- **APPLY** secure coding best practices directly by implementing the most appropriate security improvements for each identified vulnerability: Implement comprehensive input validation with whitelisting and sanitization, replace dynamic SQL with parameterized queries, establish proper authentication and authorization controls, upgrade to modern cryptographic algorithms and secure key management, implement secure exception handling that prevents information disclosure, and add proper logging and monitoring for security events
- **IMPLEMENT** comprehensive security hardening using proven patterns: Establish defense-in-depth security layers (input validation, output encoding, access controls), apply principle of least privilege throughout the application, upgrade cryptographic implementations to current standards (AES-256, SHA-256, secure random generation), implement secure error handling and logging practices, and integrate security testing and validation frameworks
- **REFACTOR** code systematically following the security improvement roadmap: First address critical injection vulnerabilities through parameterized queries and input validation, then strengthen authentication and authorization mechanisms, upgrade cryptographic implementations to modern algorithms, implement secure exception handling and logging practices, apply principle of least privilege to access controls, and integrate comprehensive security testing and monitoring
- **EXPLAIN** the applied security improvements and their benefits: Vulnerability elimination through proper input validation and parameterized queries, access control strengthening via authentication and authorization improvements, data protection enhancement through modern cryptography and secure key management, information security gains from secure exception handling and logging, and overall security posture improvement through defense-in-depth implementation
- **VALIDATE** that all applied security changes compile successfully, maintain existing functionality, eliminate identified vulnerabilities, follow security best practices, and do not introduce new security risks through comprehensive testing and security verification
- **SUPPLY-CHAIN** include dependency and build pipeline considerations: pin and verify dependency versions, remove unused dependencies, scan for known CVEs (e.g., OWASP Dependency-Check), and verify artifact integrity (checksums/signatures).
- **CONFIGURE** secure defaults: ensure debug features and detailed error pages are disabled in production, and protect management interfaces with authentication and network policies.

## Safeguards

- **BLOCKING SAFETY CHECK**: ALWAYS run `./mvnw compile` before ANY security recommendations to ensure project stability
- **CRITICAL VALIDATION**: Execute `./mvnw clean verify` to ensure all tests pass after each security improvement
- **MANDATORY VERIFICATION**: Confirm all existing functionality remains intact through comprehensive test execution
- **ROLLBACK REQUIREMENT**: Ensure all security changes can be easily reverted using version control checkpoints
- **INCREMENTAL SAFETY**: Apply security improvements incrementally, validating after each modification to isolate potential issues
- **SECURITY VALIDATION**: Verify that security improvements don't introduce new vulnerabilities or break existing security controls
- **DEPENDENCY SAFETY**: Validate that any new security libraries or dependencies don't conflict with existing project requirements
- **PERFORMANCE IMPACT**: Monitor that security enhancements don't significantly degrade application performance
- **SECRET HYGIENE**: Verify no secrets are committed to VCS, run a secrets scanner, and confirm logs do not expose sensitive values.
- **SUPPLY-CHAIN CHECKS**: Scan dependencies for CVEs and ensure pinned/signed artifacts in CI before release.
- **PRODUCTION CONFIG**: Ensure debug features, verbose errors, and open management interfaces are disabled or properly secured in production.
