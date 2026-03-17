# java-type-design

> Source: 122-java-type-design.md
> Chunk: 1/2
> Included sections: intro | Role | Goal | Constraints | Examples | Examples - Table of contents | Examples - Example 1: Establish a Clear Type Hierarchy | Examples - Example 2: Use Consistent Naming Conventions | Examples - Example 3: Embrace Whitespace (Kerning and Leading) | Examples - Example 4: Create Type-Safe Wrappers | Examples - Example 5: Leverage Generic Type Parameters | Examples - Example 6: Create Domain-Specific Languages (Typography with Character)

---
author: Juan Antonio Breña Moral
version: 0.12.0-SNAPSHOT
---
# Type Design Thinking in Java

## Role

You are a Senior software engineer with extensive experience in Java software development

## Goal

Type design thinking in Java applies typography principles to code structure and organization. Just as typography creates readable, accessible text, thoughtful type design in Java produces maintainable, comprehensible code.

### Implementing These Principles

1.  **Start with domain modeling**: Sketch your type system before coding.
2.  **Create a type style guide**: Document naming conventions and patterns.
3.  **Review for type consistency**: Periodically check for style adherence.
4.  **Refactor toward clearer type expressions**: Improve existing code.
5.  **Use tools to enforce style**: Configure linters and static analyzers.

Remember, good type design in Java is about communication - making your code's intent clear both to the compiler and to other developers.

## Constraints

Before applying any recommendations, ensure the project is in a valid state by running Maven compilation. Compilation failure is a BLOCKING condition that prevents any further processing.

- **MANDATORY**: Run `./mvnw compile` or `mvn compile` before applying any change
- **PREREQUISITE**: Project must compile successfully and pass basic validation checks before any optimization
- **CRITICAL SAFETY**: If compilation fails, IMMEDIATELY STOP and DO NOT CONTINUE with any recommendations
- **BLOCKING CONDITION**: Compilation errors must be resolved by the user before proceeding with any object-oriented design improvements
- **NO EXCEPTIONS**: Under no circumstances should design recommendations be applied to a project that fails to compile

## Examples

### Table of contents

- Example 1: Establish a Clear Type Hierarchy
- Example 2: Use Consistent Naming Conventions
- Example 3: Embrace Whitespace (Kerning and Leading)
- Example 4: Create Type-Safe Wrappers
- Example 5: Leverage Generic Type Parameters
- Example 6: Create Domain-Specific Languages (Typography with Character)
- Example 7: Use Consistent Type "Weights" (Bold, Regular, Light)
- Example 8: Apply Type Contrast Through Interfaces
- Example 9: Create Type Alignment Through Method Signatures
- Example 10: Design for Clear Type Readability and Comprehension
- Example 11: Use BigDecimal for Precision-Sensitive Calculations
- Example 12: Strategic Type Selection for Methods and Algorithms

### Example 1: Establish a Clear Type Hierarchy

Title: Organize Classes and Interfaces into Logical Structure
Description: This rule focuses on organizing classes and interfaces into a logical structure using inheritance and composition. A clear hierarchy makes the relationships between types explicit, improving code navigation and understanding. It often involves using nested static classes for closely related types.

**Good example:**

```java
// GOOD: Clear type hierarchy with descriptive names
public class OrderManagement {
    public static class Order {
        private List<OrderItem> items;
        private Customer customer;
        private OrderStatus status;

        public Order(Customer customer) {
            this.customer = customer;
            this.items = new ArrayList<>();
            this.status = OrderStatus.PENDING;
        }

        public void addItem(OrderItem item) { items.add(item); }
        public List<OrderItem> getItems() { return Collections.unmodifiableList(items); }
        public Customer getCustomer() { return customer; }
        public OrderStatus getStatus() { return status; }
    }

    public static class OrderItem {
        private Product product;
        private int quantity;
        private BigDecimal unitPrice;

        public OrderItem(Product product, int quantity, BigDecimal unitPrice) {
            this.product = product;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }

        public Product getProduct() { return product; }
        public int getQuantity() { return quantity; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public BigDecimal getTotalPrice() { return unitPrice.multiply(BigDecimal.valueOf(quantity)); }
    }

    public enum OrderStatus {
        PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
    }
}
```

**Bad example:**

```java
// AVOID: Flat structure with ambiguous names
public class Order {
    private List<Item> items; // What kind of item?
    private User user; // Is this a customer, admin, or something else?
    private int status; // What do the numbers mean?
    // ...
}

public class Item { // Too generic - what kind of item?
    private Thing thing; // What is a "thing"?
    private int count;
    // ...
}

public class User { // Too generic - could be any type of user
    private String data; // What kind of data?
    // ...
}
```

### Example 2: Use Consistent Naming Conventions

Title: Apply Uniform Patterns for Naming (Your Type's "Font Family")
Description: This rule emphasizes using uniform patterns for naming classes, interfaces, methods, and variables. Consistency in naming acts like a consistent font family in typography, making the code easier to read, predict, and maintain across the entire project.

**Good example:**

```java
// GOOD: Consistent naming patterns
interface PaymentProcessor {
    PaymentResult process(Payment payment);
}

interface ShippingCalculator {
    BigDecimal calculate(ShippingRequest request);
}

interface TaxProvider {
    Tax calculateTax(TaxableItem item, Address address);
}

// Implementation classes follow consistent naming
class StripePaymentProcessor implements PaymentProcessor {
    @Override
    public PaymentResult process(Payment payment) {
        // Stripe-specific implementation
        return new PaymentResult(true, "Payment processed successfully");
    }
}

class StandardShippingCalculator implements ShippingCalculator {
    @Override
    public BigDecimal calculate(ShippingRequest request) {
        // Standard shipping calculation logic
        return request.getWeight().multiply(new BigDecimal("0.50"));
    }
}
```

**Bad example:**

```java
// AVOID: Inconsistent naming patterns
interface PaymentProcessor {
    void handlePayment(Payment p); // Different method naming style
}

interface ShipCalc { // Inconsistent interface naming
    BigDecimal getShippingCost(Order o); // Different parameter naming
}

interface TaxSystem { // Different naming convention
    Tax lookupTaxRate(Address addr); // Abbreviated parameter name
}

// Implementation classes also inconsistent
class PaymentHandler implements PaymentProcessor { // Handler vs Processor
    @Override
    public void handlePayment(Payment p) {
        // Implementation
    }
}

class ShippingCostCalculatorImpl implements ShipCalc { // Too verbose
    @Override
    public BigDecimal getShippingCost(Order o) {
        // Implementation
    }
}
```

### Example 3: Embrace Whitespace (Kerning and Leading)

Title: Strategic Use of Spacing for Readability
Description: This rule advocates for the strategic use of blank lines and spacing within code, analogous to kerning and leading in typography. Proper whitespace improves readability by visually separating logical blocks of code, making it easier to scan and comprehend.

**Good example:**

```java
// GOOD: Proper spacing for readability
public Order processOrder(Cart cart, Customer customer) {
    // Validate inputs
    validateCart(cart);
    validateCustomer(customer);

    // Create order
    Order order = new Order(customer);
    cart.getItems().forEach(item ->
        order.addItem(item.getProduct(), item.getQuantity())
    );

    // Calculate totals
    order.calculateSubtotal();
    order.calculateTax();

    return order;
}
```

**Bad example:**

```java
// AVOID: Dense, difficult to parse code
public Order processOrder(Cart cart,Customer customer){
    validateCart(cart);validateCustomer(customer);
    Order order=new Order(customer);
    cart.getItems().forEach(item->order.addItem(item.getProduct(),item.getQuantity()));
    order.calculateSubtotal();order.calculateTax();
    return order;
}
```

### Example 4: Create Type-Safe Wrappers

Title: Use Types as Communication Tools
Description: This rule encourages wrapping primitive types or general-purpose types (like String) in domain-specific types. These wrapper types enhance type safety by enforcing invariants at compile-time and clearly communicate the intended meaning and constraints of data.

**Good example:**

```java
// GOOD: Type-safe wrappers communicate intent
public class EmailAddress {
    private final String value;

    public EmailAddress(String email) {
        if (!isValid(email)) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }
        this.value = email;
    }

    public String getValue() {
        return value;
    }

    private boolean isValid(String email) {
        return email != null &&
               email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$") &&
               email.length() <= 254; // RFC 5321 limit
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        EmailAddress that = (EmailAddress) obj;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

public class Money {
    private final BigDecimal amount;
    private final Currency currency;

    public Money(BigDecimal amount, Currency currency) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be null or negative");
        }
        this.amount = amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
        this.currency = Objects.requireNonNull(currency, "Currency cannot be null");
    }

    public BigDecimal getAmount() { return amount; }
    public Currency getCurrency() { return currency; }

    public Money add(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add different currencies");
        }
        return new Money(amount.add(other.amount), currency);
    }
}

// Usage - type safety prevents errors
void processPayment(EmailAddress customerEmail, Money paymentAmount) {
    // We know email is valid and amount is positive with proper currency
    paymentService.charge(customerEmail.getValue(), paymentAmount);
}
```

**Bad example:**

```java
// AVOID: Primitive obsession
void processPayment(String email, double amount, String currency) {
    // Need to validate every time - error prone
    if (email == null || !email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
        throw new IllegalArgumentException("Invalid email");
    }
    if (amount <= 0) {
        throw new IllegalArgumentException("Amount must be positive");
    }
    if (currency == null || currency.length() != 3) {
        throw new IllegalArgumentException("Invalid currency code");
    }

    // Still risky - what if someone passes parameters in wrong order?
    paymentService.charge(email, amount, currency);
}

// Easy to make mistakes:
// processPayment("USD", 100.0, "john@example.com"); // Wrong parameter order!
// processPayment("invalid-email", -50.0, "XXX"); // Invalid values
```

### Example 5: Leverage Generic Type Parameters

Title: Create Flexible and Reusable Types (Responsive Typography)
Description: This rule promotes the use of generics to create flexible and reusable types and methods that can operate on objects of various types while maintaining type safety. This is akin to responsive typography that adapts to different screen sizes, as generics adapt to different data types.

**Good example:**

```java
// GOOD: Generic types adapt to different contexts
public class Repository<T extends Entity> {
    private final Class<T> entityClass;
    private final EntityManager entityManager;

    public Repository(Class<T> entityClass, EntityManager entityManager) {
        this.entityClass = entityClass;
        this.entityManager = entityManager;
    }

    public Optional<T> findById(Long id) {
        T entity = entityManager.find(entityClass, id);
        return Optional.ofNullable(entity);
    }

    public List<T> findAll() {
        CriteriaQuery<T> query = entityManager.getCriteriaBuilder()
                                              .createQuery(entityClass);
        query.select(query.from(entityClass));
        return entityManager.createQuery(query).getResultList();
    }

    public T save(T entity) {
        if (entity.getId() == null) {
            entityManager.persist(entity);
            return entity;
        } else {
            return entityManager.merge(entity);
        }
    }

    public void delete(T entity) {
        entityManager.remove(entity);
    }
}

// Usage for different entity types with full type safety
Repository<Customer> customerRepo = new Repository<>(Customer.class, em);
Repository<Product> productRepo = new Repository<>(Product.class, em);

// Type-safe operations
Optional<Customer> customer = customerRepo.findById(1L);
List<Product> products = productRepo.findAll();
```

**Bad example:**

```java
// AVOID: Multiple similar classes with duplicated logic
public class CustomerRepository {
    private final EntityManager entityManager;

    public CustomerRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<Customer> findById(Long id) {
        Customer customer = entityManager.find(Customer.class, id);
        return Optional.ofNullable(customer);
    }

    public List<Customer> findAll() {
        // Duplicated logic
        CriteriaQuery<Customer> query = entityManager.getCriteriaBuilder()
                                                    .createQuery(Customer.class);
        query.select(query.from(Customer.class));
        return entityManager.createQuery(query).getResultList();
    }

    public Customer save(Customer customer) {
        // Duplicated logic
        if (customer.getId() == null) {
            entityManager.persist(customer);
            return customer;
        } else {
            return entityManager.merge(customer);
        }
    }
}

public class ProductRepository {
    // Exact same code but for Product - massive duplication!
    private final EntityManager entityManager;

    public ProductRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<Product> findById(Long id) {
        Product product = entityManager.find(Product.class, id);
        return Optional.ofNullable(product);
    }

    // ... more duplicated methods
}
```

### Example 6: Create Domain-Specific Languages (Typography with Character)

Title: Design Fluent Interfaces for Domain Expression
Description: This rule suggests designing fluent interfaces or using builder patterns to create mini "languages" specific to a domain. This makes code more expressive and readable, similar to how typography with distinct character adds personality and clarity to text.

**Good example:**

```java
// GOOD: Fluent interfaces that read like natural language
Order order = new OrderBuilder()
    .forCustomer(customer)
    .withItems(items)
    .withShippingAddress(address)
    .withPaymentMethod(paymentMethod)
    .deliverBy(LocalDate.now().plusDays(3))
    .build();
```

**Bad example:**

```java
// AVOID: Complex constructor calls or setters
Order order = new Order();
order.setCustomer(customer);
order.setItems(items);
order.setShippingAddress(address);
order.setPaymentMethod(paymentMethod);
order.setDeliveryDate(LocalDate.now().plusDays(3));
```
