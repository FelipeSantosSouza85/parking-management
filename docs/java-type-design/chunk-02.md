# java-type-design

> Source: 122-java-type-design.md
> Chunk: 2/2
> Included sections: Examples - Example 7: Use Consistent Type "Weights" (Bold, Regular, Light) | Examples - Example 8: Apply Type Contrast Through Interfaces | Examples - Example 9: Create Type Alignment Through Method Signatures | Examples - Example 10: Design for Clear Type Readability and Comprehension | Examples - Example 11: Use BigDecimal for Precision-Sensitive Calculations | Examples - Example 12: Strategic Type Selection for Methods and Algorithms | Output Format | Safeguards

### Example 7: Use Consistent Type "Weights" (Bold, Regular, Light)

Title: Assign Conceptual Importance to Types
Description: This rule advises assigning conceptual "weights" (like bold, regular, light in typography) to types based on their importance or role in the domain. Core domain objects might be "bold," supporting types "regular," and utility classes "light," helping to convey the architecture.

**Good example:**

```java
// GOOD: Types with appropriate "weight" based on importance
// "Bold" - Core domain objects
public class Customer { /* ... */ }
public class Order { /* ... */ }
public class Product { /* ... */ }

// "Regular" - Supporting types
public class Address { /* ... */ }
public class PaymentDetails { /* ... */ }

// "Light" - Helper/utility classes
public class CustomerFormatter { /* ... */ }
public class OrderValidator { /* ... */ }
```

**Bad example:**

```java
// AVOID: Inconsistent importance signals
public class CustomerStuff { /* ... */ }
public class TheOrderClass { /* ... */ }
public class ProductManager { /* ... */ }
```

### Example 8: Apply Type Contrast Through Interfaces

Title: Separate Contract from Implementation
Description: This rule emphasizes defining clear contracts using interfaces and then providing concrete implementations. This creates "contrast" by separating the "what" (interface) from the "how" (implementation), promoting loose coupling and easier testing and maintenance.

**Good example:**

```java
// GOOD: Clear interface/implementation contrast
public interface PaymentGateway {
    PaymentResult processPayment(Payment payment);
    RefundResult processRefund(Refund refund);
}

public class StripePaymentGateway implements PaymentGateway {
    @Override
    public PaymentResult processPayment(Payment payment) {
        // Stripe-specific implementation
    }

    @Override
    public RefundResult processRefund(Refund refund) {
        // Stripe-specific implementation
    }
}

public class PayPalPaymentGateway implements PaymentGateway {
    @Override
    public PaymentResult processPayment(Payment payment) {
        // PayPal-specific implementation
    }

    @Override
    public RefundResult processRefund(Refund refund) {
        // PayPal-specific implementation
    }
}
```

**Bad example:**

```java
// AVOID: Direct dependencies on implementations
public class StripePaymentProcessor {
    public StripePaymentResult processStripePayment(StripePayment payment) {
        // Stripe-specific implementation
    }

    public StripeRefundResult processStripeRefund(StripeRefund refund) {
        // Stripe-specific implementation
    }
}
```

### Example 9: Create Type Alignment Through Method Signatures

Title: Consistent Signatures for Predictable APIs
Description: This rule advocates for consistency in method signatures (names, parameter types, return types) across related classes or interfaces. Aligned signatures, like aligned text in typography, create a sense of order and predictability, making APIs easier to learn and use.

**Good example:**

```java
// GOOD: Aligned method signatures across related classes
public interface NotificationChannel {
    void send(Notification notification, Recipient recipient);
    boolean canDeliver(NotificationType type);
    DeliveryStatus checkStatus(String notificationId);
}

public class EmailNotificationChannel implements NotificationChannel {
    @Override
    public void send(Notification notification, Recipient recipient) { /* ... */ }

    @Override
    public boolean canDeliver(NotificationType type) { /* ... */ }

    @Override
    public DeliveryStatus checkStatus(String notificationId) { /* ... */ }
}

public class SmsNotificationChannel implements NotificationChannel {
    @Override
    public void send(Notification notification, Recipient recipient) { /* ... */ }

    @Override
    public boolean canDeliver(NotificationType type) { /* ... */ }

    @Override
    public DeliveryStatus checkStatus(String notificationId) { /* ... */ }
}
```

**Bad example:**

```java
// AVOID: Misaligned method signatures
public class EmailSender {
    public void sendEmail(Email email, String recipientAddress) { /* ... */ }
    public boolean supportsEmailType(EmailType type) { /* ... */ }
    public String getEmailDeliveryStatus(UUID emailId) { /* ... */ }
}

public class SmsSender {
    public void send(SmsMessage message, PhoneNumber recipient) { /* ... */ }
    public boolean canSendTo(PhoneNumber number) { /* ... */ }
    public void checkIfDelivered(String messageId) { /* ... */ }
}
```

### Example 10: Design for Clear Type Readability and Comprehension

Title: Self-Documenting Code with Clear Intent
Description: This overarching rule encourages writing self-documenting code with clear, descriptive names for types, methods, and variables. The goal is to make the code's intent immediately obvious, minimizing the need for extensive comments or external documentation.

**Good example:**

```java
// GOOD: Self-documenting code with clear intent
public class OrderService {
    public Order createOrder(Customer customer, List<CartItem> items) {
        if (items.isEmpty()) {
            throw new EmptyCartException("Cannot create order with empty cart");
        }

        if (!customer.hasValidPaymentMethod()) {
            throw new InvalidPaymentException("Customer has no valid payment method");
        }

        Order order = new Order(customer);
        items.forEach(order::addItem);

        orderRepository.save(order);
        eventPublisher.publish(new OrderCreatedEvent(order));

        return order;
    }
}
```

**Bad example:**

```java
// AVOID: Cryptic code that's hard to follow
public class OS {
    public O proc(C c, List<I> i) {
        if (i.size() < 1) throw new Ex1("e1");
        if (!c.hvm()) throw new Ex2("e2");

        O o = new O(c);
        for (I item : i) o.ai(item);

        r.s(o);
        p.p(new E(o));

        return o;
    }
}
```

### Example 11: Use BigDecimal for Precision-Sensitive Calculations

Title: Ensure Accuracy in Financial and Mathematical Operations
Description: This rule emphasizes using `java.math.BigDecimal` for calculations requiring high precision, especially with monetary values or any domain where rounding errors from binary floating-point arithmetic (like `float` or `double`) are unacceptable. Use consistent rounding modes and scale for predictable results.

**Good example:**

```java
// GOOD: Using BigDecimal for financial calculations
import java.math.BigDecimal;
import java.math.RoundingMode;

public class FinancialCalculator {
    private static final int CURRENCY_SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    public static BigDecimal calculateTotalPrice(BigDecimal itemPrice, BigDecimal taxRate, int quantity) {
        validateInputs(itemPrice, taxRate, quantity);

        BigDecimal quantityDecimal = new BigDecimal(quantity);
        BigDecimal subtotal = itemPrice.multiply(quantityDecimal);
        BigDecimal taxAmount = subtotal.multiply(taxRate)
                                      .setScale(CURRENCY_SCALE, ROUNDING_MODE);

        return subtotal.add(taxAmount).setScale(CURRENCY_SCALE, ROUNDING_MODE);
    }

    public static BigDecimal calculateMonthlyPayment(BigDecimal principal,
                                                   BigDecimal annualRate,
                                                   int monthsTotal) {
        validateInputs(principal, annualRate, monthsTotal);

        if (annualRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(new BigDecimal(monthsTotal), CURRENCY_SCALE, ROUNDING_MODE);
        }

        BigDecimal monthlyRate = annualRate.divide(new BigDecimal("12"), 10, ROUNDING_MODE);
        BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
        BigDecimal powResult = onePlusRate.pow(monthsTotal);

        BigDecimal numerator = principal.multiply(monthlyRate).multiply(powResult);
        BigDecimal denominator = powResult.subtract(BigDecimal.ONE);

        return numerator.divide(denominator, CURRENCY_SCALE, ROUNDING_MODE);
    }

    private static void validateInputs(BigDecimal amount, BigDecimal rate, int months) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must be non-negative");
        }
        if (rate == null || rate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Rate must be non-negative");
        }
        if (months <= 0) {
            throw new IllegalArgumentException("Months must be positive");
        }
    }
}
```

**Bad example:**

```java
// AVOID: Using double for financial calculations - precision issues
public class InaccurateFinancialCalculator {
    public static double calculateTotalPrice(double itemPrice, double taxRate, int quantity) {
        if (itemPrice < 0 || taxRate < 0 || quantity <= 0) {
            throw new IllegalArgumentException("Invalid inputs");
        }

        double subtotal = itemPrice * quantity;
        double taxAmount = subtotal * taxRate;

        // Precision issues! 0.1 + 0.2 != 0.3 in floating point
        return subtotal + taxAmount;
    }

    public static double calculateMonthlyPayment(double principal, double annualRate, int months) {
        if (principal < 0 || annualRate < 0 || months <= 0) {
            throw new IllegalArgumentException("Invalid inputs");
        }

        if (annualRate == 0) {
            return principal / months;
        }

        double monthlyRate = annualRate / 12;
        double factor = Math.pow(1 + monthlyRate, months);

        // More precision issues with floating point arithmetic
        return (principal * monthlyRate * factor) / (factor - 1);
    }

    public static void main(String[] args) {
        // This will demonstrate the precision problem
        double result1 = calculateTotalPrice(19.99, 0.075, 3);
        double result2 = calculateTotalPrice(29.99, 0.08, 2);

        System.out.println("Result 1: " + result1); // May show unexpected decimals
        System.out.println("Result 2: " + result2); // May show unexpected decimals

        // Rounding manually is error-prone and inconsistent
        System.out.println("Rounded 1: " + Math.round(result1 * 100.0) / 100.0);
        System.out.println("Rounded 2: " + Math.round(result2 * 100.0) / 100.0);
    }
}
```

### Example 12: Strategic Type Selection for Methods and Algorithms

Title: Choose Appropriate Types for Maximum Clarity and Safety
Description: This rule emphasizes choosing the most appropriate Java types for method parameters, return values, and internal algorithm variables. Considerations include specificity (preferring the most precise type that still allows necessary flexibility), using interfaces over concrete classes for parameters and return types where appropriate, selecting suitable collection types (`List`, `Set`, `Map`, etc.) based on requirements (e.g., ordering, uniqueness, access patterns, performance characteristics), and leveraging types like `Optional` for results that may be absent. It also covers the deliberate choice between primitive types and their wrapper counterparts, especially concerning nullability and collection usage.

**Good example:**

```java
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

// Define specific domain types (can be records or classes)
record ProductId(String id) {}
record Product(ProductId productId, String name, java.math.BigDecimal price) {}
record CustomerId(String id) {}

interface ProductRepository {
    Optional<Product> findById(ProductId productId);
    Set<Product> findByCategory(String category); // Using Set if products in a category are unique and order doesn't matter
}

class ProductService {
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // Using specific types for parameters and Optional for return type
    public Optional<Product> getProductDetails(ProductId productId) {
        if (productId == null || productId.id().trim().isEmpty()) {
            // Consider throwing IllegalArgumentException or returning Optional.empty() based on contract
            return Optional.empty();
        }
        return productRepository.findById(productId);
    }

    // Using Interface for parameter type (Collection) and specific List for return (if order is important)
    public List<Product> getProductsWithMinimumPrice(Set<ProductId> productIds, java.math.BigDecimal minPrice) {
        return productIds.stream()
            .map(productRepository::findById)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .filter(product -> product.price().compareTo(minPrice) >= 0)
            .collect(Collectors.toList()); // Collecting to List, implies order might matter or is at least acceptable
    }
}
```

**Bad example:**

```java
import java.util.ArrayList;
import java.util.HashMap;

// Using generic Object or overly broad types
class BadProductService {
    private HashMap<String, Object> productCache; // Using HashMap directly, and Object for product

    public BadProductService() {
        this.productCache = new HashMap<>();
    }

    // Returning Object, forcing caller to cast and check type. Parameter is just String, not a specific ID type.
    public Object getProduct(String productId) {
        // Potentially returns null if not found, forcing null checks on caller side
        return productCache.get(productId);
    }

    // Using ArrayList (concrete type) as parameter, List of Object for products.
    // What if the algorithm internally needs set-like properties?
    public ArrayList<Object> findAvailableProducts(ArrayList<Object> allProducts, double minimumPrice) {
        ArrayList<Object> available = new ArrayList<>();
        for (Object p : allProducts) {
            // Requires instanceof checks and casting, error-prone
            if (p instanceof HashMap) { // Assuming product is a HashMap - very bad practice
                HashMap<String, Object> productMap = (HashMap<String, Object>) p;
                if (productMap.containsKey("price") && (Double)productMap.get("price") >= minimumPrice) {
                    available.add(p);
                }
            }
        }
        return available; // Returning concrete ArrayList, less flexible
    }
}
```

## Output Format

- **ANALYZE** Java code to identify specific type design issues and categorize them by impact (CRITICAL, MAINTAINABILITY, TYPE_SAFETY, READABILITY) and type design area (naming conventions, type hierarchies, generic usage, primitive obsession, type safety)
- **CATEGORIZE** type design improvements found: Naming Convention Issues (inconsistent patterns vs standardized naming, unclear intent vs domain-driven names), Type Hierarchy Problems (poor organization vs structured hierarchies, missing abstractions vs proper interfaces), Generic Type Opportunities (missing type parameters vs parameterized types, overly broad bounds vs precise constraints), Primitive Obsession (String/int overuse vs domain types, raw primitives vs value objects), Type Safety Gaps (unsafe casts vs type-safe operations, missing validation vs robust checks), and Precision Issues (double/float vs BigDecimal for financial calculations)
- **APPLY** type design best practices directly by implementing the most appropriate improvements for each identified issue: Create domain-specific value objects to replace primitive obsession, introduce proper generic type parameters with appropriate bounds, establish consistent naming conventions following domain terminology, refactor unsafe casts to type-safe operations, implement BigDecimal for financial calculations, and organize type hierarchies with clear abstractions
- **IMPLEMENT** comprehensive type design refactoring using proven patterns: Replace primitive obsession with value objects and records, establish type-safe generic hierarchies with bounded parameters, apply domain-driven naming conventions consistently, create fluent interfaces for complex operations, implement precision-appropriate numeric types (BigDecimal for money, int for counts), and organize type hierarchies following single responsibility and interface segregation principles
- **REFACTOR** code systematically following the type design improvement roadmap: First identify and replace primitive obsession with domain-specific types, then establish proper generic type parameters and bounds, apply consistent naming conventions across the codebase, refactor unsafe type operations to type-safe alternatives, implement appropriate precision types for calculations, and organize type hierarchies with clear abstractions and inheritance relationships
- **EXPLAIN** the applied type design improvements and their benefits: Type safety enhancements through proper generic usage and value objects, maintainability improvements via domain-driven naming and clear hierarchies, readability gains from expressive type names and interfaces, calculation accuracy through precision-appropriate numeric types, and code robustness from eliminated primitive obsession and unsafe operations
- **VALIDATE** that all applied type design refactoring compiles successfully, maintains existing functionality, preserves type safety guarantees, follows established naming conventions, and achieves the intended design improvements through comprehensive testing and verification

## Safeguards

- **CONTINUOUS COMPILATION**: Validate compilation after each type design change to catch issues immediately
- **TYPE COMPATIBILITY**: Ensure new type designs maintain backward compatibility with existing APIs and don't break client code
- **CIRCULAR DEPENDENCY CHECK**: Verify that new type hierarchies don't introduce circular dependencies or inappropriate coupling
- **GENERIC BOUNDS VALIDATION**: Confirm that generic type parameters have appropriate bounds and don't create unchecked warnings
- **TEST COVERAGE MAINTENANCE**: Execute `./mvnw clean verify` to ensure all tests pass after implementing type design improvements
- **FUNCTIONAL REGRESSION CHECK**: Confirm all existing functionality remains intact after applying type design changes
- **ROLLBACK READINESS**: Ensure all type design changes can be easily reverted if they introduce regressions or compilation issues
- **INCREMENTAL VALIDATION**: Apply improvements incrementally, validating each change before proceeding to the next
- **DEPENDENCY IMPACT ANALYSIS**: Verify that type changes don't break existing dependencies, imports, or class relationships
- **FINAL INTEGRATION TEST**: Perform comprehensive project compilation and test execution after completing all type design improvements
