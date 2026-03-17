# java-unit-testing

> Source: 131-java-unit-testing.md
> Chunk: 1/2
> Included sections: intro | Role | Goal | Constraints | Examples | Examples - Table of contents | Examples - Example 1: Use JUnit 5 Annotations | Examples - Example 2: Use AssertJ for Assertions | Examples - Example 3: Structure Tests with Given-When-Then | Examples - Example 4: Use Descriptive Test Names | Examples - Example 5: Aim for Single Responsibility in Tests | Examples - Example 6: Ensure Tests are Independent | Examples - Example 7: Use Parameterized Tests for Data Variations

---
author: Juan Antonio Breña Moral
version: 0.12.0-SNAPSHOT
---
# Java Unit testing guidelines

## Role

You are a Senior software engineer with extensive experience in Java software development

## Goal

Effective Java unit testing involves using JUnit 5 annotations and AssertJ for fluent assertions. Tests should follow the Given-When-Then structure with descriptive names for clarity. Each test must have a single responsibility, be independent, and leverage parameterized tests for data variations. Mocking dependencies with frameworks like Mockito is crucial for isolating the unit under test. While code coverage is a useful guide, the focus should be on meaningful tests for critical logic and edge cases. Test classes and methods should typically be package-private. Strategies for code splitting include small test methods and helper functions. Anti-patterns like testing implementation details, hard-coded values, and ignoring failures should be avoided. Proper state management involves isolated state and immutable objects, and error handling should include testing for expected exceptions and their messages.

### Implementing These Principles

These guidelines are built upon the following core principles:

1.  **Clarity and Readability**: Tests should be easy to understand. This is achieved through descriptive names (or `@DisplayName`), a clear Given-When-Then structure, and focused assertions. Readable tests serve as living documentation for the code under test.
2.  **Isolation and Independence**: Each test must be self-contained, not relying on the state or outcome of other tests. Dependencies should be mocked to ensure the unit under test is validated in isolation. This leads to reliable and stable test suites.
3.  **Comprehensive Validation**: Tests should thoroughly verify the behavior of the unit, including its responses to valid inputs, edge cases, boundary conditions, and error scenarios. This involves not just positive paths but also how the code handles failures and exceptions.
4.  **Modern Tooling and Practices**: Leverage modern testing frameworks (JUnit 5), fluent assertion libraries (AssertJ), and mocking tools (Mockito) to write expressive, maintainable, and powerful tests. Utilize features like parameterized tests to reduce boilerplate and improve coverage of data variations.
5.  **Maintainability and Focus**: Tests should be easy to maintain. This means avoiding tests that are too complex, test implementation details, or have multiple responsibilities. A well-written test makes it clear what is being tested and why, simplifying debugging and refactoring efforts.

## Constraints

Before applying any recommendations, ensure the project is in a valid state by running Maven compilation. Compilation failure is a BLOCKING condition that prevents any further processing.

- **MANDATORY**: Run `./mvnw compile` or `mvn compile` before applying any change
- **PREREQUISITE**: Project must compile successfully and pass basic validation checks before any optimization
- **CRITICAL SAFETY**: If compilation fails, IMMEDIATELY STOP and DO NOT CONTINUE with any recommendations
- **BLOCKING CONDITION**: Compilation errors must be resolved by the user before proceeding with any object-oriented design improvements
- **NO EXCEPTIONS**: Under no circumstances should design recommendations be applied to a project that fails to compile

## Examples

### Table of contents

- Example 1: Use JUnit 5 Annotations
- Example 2: Use AssertJ for Assertions
- Example 3: Structure Tests with Given-When-Then
- Example 4: Use Descriptive Test Names
- Example 5: Aim for Single Responsibility in Tests
- Example 6: Ensure Tests are Independent
- Example 7: Use Parameterized Tests for Data Variations
- Example 8: Utilize Mocking for Dependencies (Mockito)
- Example 9: Consider Test Coverage, But Don't Obsess
- Example 10: Test Scopes
- Example 11: Code Splitting Strategies
- Example 12: Anti-patterns and Code Smells
- Example 13: State Management
- Example 14: Error Handling
- Example 15: Leverage JSpecify for Null Safety
- Example 16: Key Questions to Guide Test Creation (RIGHT-BICEP)
- Example 17: Characteristics of Good Tests (A-TRIP)
- Example 18: Verifying CORRECT Boundary Conditions

### Example 1: Use JUnit 5 Annotations

Title: Prefer JUnit 5 annotations over JUnit 4
Description: Utilize annotations from the `org.junit.jupiter.api` package (e.g., `@Test`, `@BeforeEach`, `@AfterEach`, `@DisplayName`, `@Nested`, `@Disabled`) instead of their JUnit 4 counterparts (`@org.junit.Test`, `@Before`, `@After`, `@Ignore`). This ensures consistency and allows leveraging the full capabilities of JUnit 5.

**Good example:**

```java
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("My Service Test")
class MyServiceTest {

    private MyService service;

    @BeforeEach
    void setUp() {
        service = new MyService(); // Setup executed before each test
    }

    @Test
    @DisplayName("should process data correctly")
    void processData() {
        // Given
        String input = "test";

        // When
        String result = service.process(input);

        // Then
        assertThat(result).isEqualTo("PROCESSED:test");
    }
}
```

**Bad example:**

```java
import org.junit.Before; // JUnit 4
import org.junit.Test;   // JUnit 4
import static org.junit.Assert.assertEquals; // JUnit 4 Assert

public class MyServiceTest {

    private MyService service;

    @Before // JUnit 4
    public void setup() {
        service = new MyService();
    }

    @Test // JUnit 4
    public void processData() {
        String input = "test";
        String result = service.process(input);
        assertEquals("PROCESSED:test", result); // JUnit 4 Assert
    }
}
```

### Example 2: Use AssertJ for Assertions

Title: Prefer AssertJ for assertions
Description: Employ AssertJ's fluent API (`org.assertj.core.api.Assertions.assertThat`) for more readable, expressive, and maintainable assertions compared to JUnit Jupiter's `Assertions` class or Hamcrest matchers.

**Good example:**

```java
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssertJExampleTest {

    @Test
    void checkValue() {
        String result = "hello";
        assertThat(result)
            .isEqualTo("hello")
            .startsWith("hell")
            .endsWith("o")
            .hasSize(5); // Chain multiple assertions fluently
    }

    @Test
    void checkException() {
        MyService service = new MyService();
        assertThatThrownBy(() -> service.divide(1, 0)) // Preferred way to test exceptions
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("zero");
    }
}
```

**Bad example:**

```java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JUnitAssertionsExampleTest {

    @Test
    void checkValue() {
        String result = "hello";
        assertEquals("hello", result); // Less fluent
        assertTrue(result.startsWith("hell")); // Separate assertions for each property
        assertTrue(result.endsWith("o"));
        assertEquals(5, result.length());
    }

    @Test
    void checkException() {
        MyService service = new MyService();
        // More verbose exception testing
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.divide(1, 0)
        );
        assertTrue(exception.getMessage().contains("zero")); // Separate assertion for message
    }
}
```

### Example 3: Structure Tests with Given-When-Then

Title: Structure test methods using the Given-When-Then pattern
Description: Organize the logic within test methods into three distinct, clearly separated phases: **Given** (setup preconditions), **When** (execute the code under test), and **Then** (verify the outcome). Use comments or empty lines to visually separate these phases, enhancing readability and understanding of the test's purpose.

**Good example:**

```java
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class GivenWhenThenTest {

    @Test
    void shouldCalculateSumCorrectly() {
        // Given
        Calculator calculator = new Calculator();
        int num1 = 5;
        int num2 = 10;
        int expectedSum = 15;

        // When
        int actualSum = calculator.add(num1, num2);

        // Then
        assertThat(actualSum).isEqualTo(expectedSum);
    }
}
```

**Bad example:**

```java
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class UnstructuredTest {

    @Test
    void testAddition() {
        // Lack of clear separation makes it harder to follow the test flow
        Calculator calculator = new Calculator();
        assertThat(calculator.add(5, 10)).isEqualTo(15); // Combines action and verification
        // Setup might be mixed with action or verification elsewhere
    }
}
```

### Example 4: Use Descriptive Test Names

Title: Write descriptive test method names or use @DisplayName
Description: Test names should clearly communicate the scenario being tested and the expected outcome. Use either descriptive method names (e.g., following the `should_ExpectedBehavior_when_StateUnderTest` pattern) or JUnit 5's `@DisplayName` annotation for more natural language descriptions. This makes test reports easier to understand.

**Good example:**

```java
@Test
void should_throwException_when_divisorIsZero() {
    // Given
    Calculator calculator = new Calculator();

    // When & Then
    assertThatThrownBy(() -> calculator.divide(1, 0))
        .isInstanceOf(ArithmeticException.class);
}

@Test
@DisplayName("Should return the correct sum for positive numbers")
void additionWithPositives() {
     // Given
     Calculator calculator = new Calculator();
     int num1 = 5;
     int num2 = 10;

     // When
     int actualSum = calculator.add(num1, num2);

     // Then
     assertThat(actualSum).isEqualTo(15);
}
```

**Bad example:**

```java
@Test
void testDivide() { // Name is too generic, doesn't explain the scenario
    // ... test logic ...
}

@Test
void test1() { // Uninformative name
    // ... test logic ...
}
```

### Example 5: Aim for Single Responsibility in Tests

Title: Each test method should verify a single logical concept
Description: Avoid testing multiple unrelated things within a single test method. Each test should focus on one specific aspect of the unit's behavior or one particular scenario. This makes tests easier to understand, debug, and maintain. If a test fails, its specific focus makes pinpointing the cause simpler.

**Good example:**

```java
// Separate tests for different validation aspects
@Test
void should_reject_when_emailIsNull() {
    // ... test logic for null email ...
}

@Test
void should_reject_when_emailFormatIsInvalid() {
    // ... test logic for invalid email format ...
}
```

**Bad example:**

```java
@Test
void testUserValidation() { // Tests multiple conditions at once
    // Given user with null email
    // ... assertion for null email ...

    // Given user with invalid email format
    // ... assertion for invalid format ...

    // Given user with valid email
    // ... assertion for valid email ...
}
```

### Example 6: Ensure Tests are Independent

Title: Tests must be independent and runnable in any order
Description: Avoid creating tests that depend on the state left behind by previously executed tests. Each test should set up its own required preconditions (using `@BeforeEach` or within the test method itself) and should not rely on the execution order. This ensures test suite stability and reliability, preventing flickering tests.

**Good example:**

```java
class IndependentTests {
    private MyRepository repository = new InMemoryRepository(); // Or use @BeforeEach

    @Test
    void should_findItem_when_itemExists() {
        // Given
        Item item = new Item("testId", "TestData");
        repository.save(item); // Setup specific to this test

        // When
        Optional<Item> found = repository.findById("testId");

        // Then
        assertThat(found).isPresent();
    }

    @Test
    void should_returnEmpty_when_itemDoesNotExist() {
        // Given - Repository is clean (or re-initialized via @BeforeEach)

        // When
        Optional<Item> found = repository.findById("nonExistentId");

        // Then
        assertThat(found).isNotPresent();
    }
}
```

**Bad example:**

```java
class DependentTests {
    private static MyRepository repository = new InMemoryRepository(); // Shared state
    private static Item savedItem;

    @Test // Test 1 (might run first)
    void testSave() {
        savedItem = new Item("testId", "Data");
        repository.save(savedItem);
        // ... assertions ...
    }

    @Test // Test 2 (depends on Test 1 having run)
    void testFind() {
        // This test fails if testSave() hasn't run or if run order changes
        Optional<Item> found = repository.findById("testId");
        assertThat(found).isPresent();
    }
}
```

### Example 7: Use Parameterized Tests for Data Variations

Title: Use @ParameterizedTest for testing the same logic with different inputs
Description: When testing a method's behavior across various input values or boundary conditions, leverage JUnit 5's parameterized tests (`@ParameterizedTest` with sources like `@ValueSource`, `@CsvSource`, `@MethodSource`). This avoids code duplication and clearly separates the test logic from the test data.

**Good example:**

```java
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.assertThat;

class ParameterizedCalculatorTest {

    private final Calculator calculator = new Calculator();

    @ParameterizedTest(name = "{index} {0} + {1} = {2}") // Clear naming for each case
    @CsvSource({
        "1,  2,  3",
        "0,  0,  0",
        "-5, 5,  0",
        "10, -3, 7"
    })
    void additionTest(int a, int b, int expectedResult) {
        // Given inputs a, b (from @CsvSource)

        // When
        int actualResult = calculator.add(a, b);

        // Then
        assertThat(actualResult).isEqualTo(expectedResult);
    }
}
```

**Bad example:**

```java
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RepetitiveCalculatorTest {

    private final Calculator calculator = new Calculator();

    // Redundant tests for the same logic
    @Test
    void add1and2() {
        assertThat(calculator.add(1, 2)).isEqualTo(3);
    }

    @Test
    void add0and0() {
        assertThat(calculator.add(0, 0)).isEqualTo(0);
    }

    @Test
    void addNegative5and5() {
        assertThat(calculator.add(-5, 5)).isEqualTo(0);
    }

    @Test
    void add10andNegative3() {
        assertThat(calculator.add(10, -3)).isEqualTo(7);
    }
}
```
