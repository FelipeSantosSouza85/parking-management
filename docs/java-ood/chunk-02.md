# java-ood

> Source: 121-java-object-oriented-design.md
> Chunk: 2/6
> Included sections: Examples - Example 9: Design Well-Structured and Maintainable Classes and Interfaces | Examples - Example 10: Effectively Utilize Core Object-Oriented Concepts | Examples - Example 11: Encapsulation | Examples - Example 12: Inheritance | Examples - Example 13: Polymorphism | Examples - Example 14: Recognize and Address Common OOD Code Smells | Examples - Example 15: Large Class / God Class | Examples - Example 16: Feature Envy | Examples - Example 17: Inappropriate Intimacy | Examples - Example 18: Refused Bequest | Examples - Example 19: Shotgun Surgery | Examples - Example 20: Data Clumps | Examples - Example 21: Creating and Destroying Objects | Examples - Example 22: Consider Static Factory Methods Instead of Constructors

### Example 9: Design Well-Structured and Maintainable Classes and Interfaces

Title: Design Well-Structured and Maintainable Classes and Interfaces
Description: Good class and interface design is crucial for building flexible and understandable OOD systems. Favor composition over inheritance, program to interfaces rather than implementations, keep classes small and focused, and design for immutability where appropriate. Use clear, descriptive naming conventions.

**Good example:**

```java
// Interface (Abstraction)
interface Engine {
    void start();
    void stop();
}

// Concrete Implementations
class PetrolEngine implements Engine {
    @Override public void start() { System.out.println("Petrol engine started."); }
    @Override public void stop() { System.out.println("Petrol engine stopped."); }
}

class ElectricEngine implements Engine {
    @Override public void start() { System.out.println("Electric engine silently started."); }
    @Override public void stop() { System.out.println("Electric engine silently stopped."); }
}

// Class using Composition and Programming to an Interface
class Car {
    private final Engine engine; // Depends on Engine interface (abstraction)
    private final String modelName;

    // Engine is injected (composition)
    public Car(String modelName, Engine engine) {
        this.modelName = modelName;
        this.engine = engine;
    }

    public void startCar() {
        System.out.print(modelName + ": ");
        engine.start();
    }

    public void stopCar() {
        System.out.print(modelName + ": ");
        engine.stop();
    }

    public String getModelName(){ return modelName; }
}

public class ClassDesignExample {
    public static void main(String args) {
        Car petrolCar = new Car("SedanX", new PetrolEngine());
        Car electricCar = new Car("EVMax", new ElectricEngine());

        petrolCar.startCar();
        electricCar.startCar();
        petrolCar.stopCar();
        electricCar.stopCar();
    }
}
```

**Bad example:**

```java
// Bad: Tight coupling, not programming to an interface
class BadCar {
    private final BadPetrolEngine engine; // Direct dependency on concrete BadPetrolEngine
    public BadCar() {
        this.engine = new BadPetrolEngine(); // Instantiates concrete class
    }
    public void start() { engine.startPetrol(); }
    // If we want an electric car, this class needs significant changes or a new similar class.
}
class BadPetrolEngine { public void startPetrol() { System.out.println("Bad petrol engine starts."); } }
```

### Example 10: Effectively Utilize Core Object-Oriented Concepts

Title: Effectively Utilize Core Object-Oriented Concepts
Description: Encapsulation, Inheritance, and Polymorphism are the three pillars of object-oriented programming.

### Example 11: Encapsulation

Title: Protect Internal State and Implementation Details
Description: Hide the internal state (fields) and implementation details of an object from the outside world. Expose a well-defined public interface (methods) for interacting with the object. Use access modifiers effectively to control visibility and protect invariants.

**Good example:**

```java
class BankAccount {
    private double balance; // Encapsulated: internal state is private
    private final String accountNumber;

    public BankAccount(String accountNumber, double initialBalance) {
        this.accountNumber = accountNumber;
        if (initialBalance < 0) throw new IllegalArgumentException("Initial balance cannot be negative.");
        this.balance = initialBalance;
    }

    // Public interface to interact with the balance
    public void deposit(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Deposit amount must be positive.");
        this.balance += amount;
        System.out.println("Deposited: " + amount + ", New Balance: " + this.balance);
    }

    public void withdraw(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Withdrawal amount must be positive.");
        if (amount > this.balance) throw new IllegalArgumentException("Insufficient funds.");
        this.balance -= amount;
        System.out.println("Withdrew: " + amount + ", New Balance: " + this.balance);
    }

    public double getBalance() { // Controlled access to balance
        return this.balance;
    }
    public String getAccountNumber() { return this.accountNumber; }
}
```

**Bad example:**

```java
// Bad: Poor encapsulation, exposing internal state
class UnsafeBankAccount {
    public double balance; // Public field: internal state exposed and can be freely modified
    public String accountNumber;

    public UnsafeBankAccount(String accNum, double initial) { this.accountNumber = accNum; this.balance = initial; }
    // No methods to control how balance is changed, invariants can be broken.
}
public class BadEncapsulationExample {
    public static void main(String args) {
        UnsafeBankAccount account = new UnsafeBankAccount("123", 100.0);
        account.balance = -500.0; // Direct modification, potentially breaking business rules
        System.out.println("Unsafe balance: " + account.balance);
    }
}
```

### Example 12: Inheritance

Title: Model "is-a" Relationships and Ensure LSP
Description: Use inheritance to model true "is-a" relationships, where a subclass is a more specific type of its superclass. Ensure that the Liskov Substitution Principle (LSP) is followed: subclasses must be substitutable for their base types without altering the correctness of the program.

**Good example:**

```java
abstract class Animal {
    private String name;
    public Animal(String name) { this.name = name; }
    public String getName() { return name; }
    public abstract void makeSound(); // Abstract method for polymorphism
}

class Dog extends Animal { // Dog IS-A Animal
    public Dog(String name) { super(name); }
    @Override public void makeSound() { System.out.println(getName() + " says: Woof!"); }
    public void fetch() { System.out.println(getName() + " is fetching."); }
}

class Cat extends Animal { // Cat IS-A Animal
    public Cat(String name) { super(name); }
    @Override public void makeSound() { System.out.println(getName() + " says: Meow!"); }
    public void purr() { System.out.println(getName() + " is purring."); }
}

public class InheritanceExample {
    public static void main(String args) {
        Animal myDog = new Dog("Buddy");
        Animal myCat = new Cat("Whiskers");
        myDog.makeSound();
        myCat.makeSound();
        // ((Dog)myDog).fetch(); // Can cast if sure of type to access specific methods
    }
}
```

**Bad example:**

```java
// Bad: Incorrect "is-a" relationship using composition instead
class Window {
    public void open() { System.out.println("Window opened."); }
    public void close() { System.out.println("Window closed."); }
}

class BetterCarDoor {
    private WindowComponent window = new WindowComponent();
    public void openDoor() { System.out.println("Car door opened."); }
    public void closeDoor() { System.out.println("Car door closed."); }
    public void openWindow() { window.open(); }
    public void closeWindow() { window.close(); }
    static class WindowComponent { /* Similar to Window */
        public void open() {System.out.println("Car window rolling down.");}
        public void close() {System.out.println("Car window rolling up.");}
    }
}
```

### Example 13: Polymorphism

Title: Enable Objects to Respond to the Same Message Differently
Description: Polymorphism allows objects of different classes (that share a common superclass or interface) to respond to the same message (method call) in their own specific ways. It simplifies client code, as it can interact with different types of objects through a common interface without needing to know their concrete types.

**Good example:**

```java
interface Drawable {
    void draw();
}

class CircleShape implements Drawable {
    @Override public void draw() { System.out.println("Drawing a Circle: O"); }
}

class SquareShape implements Drawable {
    @Override public void draw() { System.out.println("Drawing a Square: □"); }
}

class TriangleShape implements Drawable {
    @Override public void draw() { System.out.println("Drawing a Triangle: /\\"); }
}

public class PolymorphismExample {
    public static void drawShapes(List<Drawable> shapes) {
        for (Drawable shape : shapes) {
            shape.draw(); // Polymorphic call: actual method executed depends on shape's concrete type
        }
    }
    public static void main(String args) {
        List<Drawable> myShapes = List.of(
            new CircleShape(),
            new SquareShape(),
            new TriangleShape()
        );
        drawShapes(myShapes);
    }
}
```

**Bad example:**

```java
// Bad: Lacking polymorphism, using type checking and casting
class ShapeDrawer {
    public void drawSpecificShape(Object shape) {
        if (shape instanceof CircleShapeBad) {
            ((CircleShapeBad) shape).drawCircle();
        } else if (shape instanceof SquareShapeBad) {
            ((SquareShapeBad) shape).drawSquare();
        } else if (shape instanceof TriangleShapeBad) {
            ((TriangleShapeBad) shape).drawTriangle();
        } else {
            System.out.println("Unknown shape type.");
        }
        // This is not polymorphic. Adding new shapes requires modifying this method.
    }
}

class CircleShapeBad { public void drawCircle() { System.out.println("Drawing Circle (Bad)."); } }
class SquareShapeBad { public void drawSquare() { System.out.println("Drawing Square (Bad)."); } }
class TriangleShapeBad { public void drawTriangle() { System.out.println("Drawing Triangle (Bad)."); } }
```

### Example 14: Recognize and Address Common OOD Code Smells

Title: Recognize and Address Common OOD Code Smells
Description: Code smells are symptoms of potential underlying problems in the design. Recognizing and refactoring them can significantly improve code quality.

### Example 15: Large Class / God Class

Title: A class that knows or does too much.
Description: Such classes violate SRP and are hard to understand, maintain, and test. Consider breaking them down into smaller, more focused classes.

### Example 16: Feature Envy

Title: A method that seems more interested in a class other than the one it actually is in.
Description: This often means the method is using data from another class more than its own. Consider moving the method to the class it's "envious" of, or introduce a new class to mediate.

**Good example:**

```java
class Customer {
    private String name;
    private Address address;
    public Customer(String name, Address address) { this.name = name; this.address = address; }
    public String getFullAddressDetails() { // Method operates on its own Address object
        return address.getStreet() + ", " + address.getCity() + ", " + address.getZipCode();
    }
}
class Address {
    private String street, city, zipCode;
    public Address(String s, String c, String z) { street=s; city=c; zipCode=z; }
    public String getStreet() { return street; }
    public String getCity() { return city; }
    public String getZipCode() { return zipCode; }
}
```

**Bad example:**

```java
class Order {
    private double amount;
    private Customer customer; // Has a Customer
    public Order(double amount, Customer customer) { this.amount = amount; this.customer = customer; }

    // Bad: This method is more interested in Customer's Address than Order itself
    public String getCustomerShippingLabel() {
        Address addr = customer.getAddress(); // Assuming Customer has getAddress()
        return customer.getName() + "\n" + addr.getStreet() +
               "\n" + addr.getCity() + ", " + addr.getZipCode();
        // Better: Move this logic to Customer class as getShippingLabel() or similar.
    }
}
```

### Example 17: Inappropriate Intimacy

Title: Classes that spend too much time delving into each other's private parts.
Description: This indicates tight coupling and poor encapsulation. Classes should interact through well-defined public interfaces, not by accessing internal implementation details of others.

**Bad example:**

```java
class ServiceA {
    public int internalCounter = 0; // Public field, bad
    public void doSomething() { internalCounter++; }
}
class ServiceB {
    public void manipulateServiceA(ServiceA serviceA) {
        // Bad: Directly accessing and modifying internal state of ServiceA
        serviceA.internalCounter = 100;
        System.out.println("ServiceA counter directly set to: " + serviceA.internalCounter);
        // Better: ServiceA should have a method like resetCounter(int value) if this is valid behavior.
    }
}
```

### Example 18: Refused Bequest

Title: A subclass uses only some of the methods and properties inherited from its parents.
Description: This might indicate a violation of LSP or an incorrect inheritance hierarchy. The subclass might not truly be a substitutable type of the superclass.

### Example 19: Shotgun Surgery

Title: When a single conceptual change requires modifications in many different classes.
Description: This often indicates that a single responsibility has been spread too thinly across multiple classes, leading to high coupling and difficulty in making changes.

### Example 20: Data Clumps

Title: Bunches of data items that regularly appear together in multiple places.
Description: These data clumps often represent a missing concept that should be encapsulated into its own object or record.

**Good example:**

```java
// Good: Encapsulating related data into a Range object
record DateRange(LocalDate start, LocalDate end) {
    public DateRange {
        if (start.isAfter(end)) throw new IllegalArgumentException("Start date must be before end date.");
    }
}

class EventScheduler {
    public void scheduleEvent(String eventName, DateRange range) {
        System.out.println("Scheduling " + eventName + " from " + range.start() + " to " + range.end());
    }
    public boolean isDateInRange(LocalDate date, DateRange range) {
        return !date.isBefore(range.start()) && !date.isAfter(range.end());
    }
}
```

**Bad example:**

```java
// Bad: Data clump (startDay, startMonth, startYear, endDay, endMonth, endYear) passed around
class EventSchedulerBad {
    public void scheduleEvent(String eventName,
                              int startDay, int startMonth, int startYear,
                              int endDay, int endMonth, int endYear) {
        // ... logic using these separate date parts ...
        System.out.println("Scheduling event with many date parameters.");
    }
    public boolean checkOverlap(int sDay1, int sMon1, int sYr1, int eDay1, int eMon1, int eYr1,
                              int sDay2, int sMon2, int sYr2, int eDay2, int eMon2, int eYr2) {
        // ... complex logic with many parameters ...
        return false;
    }
    // This pattern of passing around many related date parts is a data clump.
}
```

### Example 21: Creating and Destroying Objects

Title: Best Practices for Object Creation and Destruction
Description: Effective object creation and destruction patterns improve code clarity, performance, and maintainability. These practices help avoid common pitfalls and leverage Java's capabilities effectively.

### Example 22: Consider Static Factory Methods Instead of Constructors

Title: Use static factory methods to provide more flexibility than constructors
Description: Static factory methods offer advantages like descriptive names, ability to return existing instances, and flexibility in return types.

**Good example:**

```java
public class BigInteger {
    // Static factory method with descriptive name
    public static BigInteger valueOf(long val) {
        if (val == 0) return ZERO;  // Return cached instance
        if (val > 0 && val <= MAX_CONSTANT) return posConst[(int) val];
        return new BigInteger(val);
    }

    // Private constructor
    private BigInteger(long val) { /* implementation */ }

    private static final BigInteger ZERO = new BigInteger(0);
    private static final BigInteger[] posConst = new BigInteger[MAX_CONSTANT + 1];
}

// Usage with clear intent
BigInteger zero = BigInteger.valueOf(0);  // Clear what we're creating
BigInteger hundred = BigInteger.valueOf(100);
```

**Bad example:**

```java
public class BigInteger {
    // Only constructor available - less flexible
    public BigInteger(long val) { /* implementation */ }

    // Client code is less clear
    BigInteger zero = new BigInteger(0);  // Not clear this could be cached
    BigInteger hundred = new BigInteger(100);  // Creates new instance every time
}
```
