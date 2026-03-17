# java-unit-testing

> Source: 131-java-unit-testing.md
> Chunk: 2/2
> Included sections: Examples - Example 8: Utilize Mocking for Dependencies (Mockito) | Examples - Example 9: Consider Test Coverage, But Don't Obsess | Examples - Example 10: Test Scopes | Examples - Example 11: Code Splitting Strategies | Examples - Example 12: Anti-patterns and Code Smells | Examples - Example 13: State Management | Examples - Example 14: Error Handling | Examples - Example 15: Leverage JSpecify for Null Safety | Examples - Example 16: Key Questions to Guide Test Creation (RIGHT-BICEP) | Examples - Example 17: Characteristics of Good Tests (A-TRIP) | Examples - Example 18: Verifying CORRECT Boundary Conditions | Output Format | Safeguards

### Example 8: Utilize Mocking for Dependencies (Mockito)

Title: Isolate the unit under test using mocking frameworks like Mockito
Description: Unit tests should focus solely on the logic of the class being tested (System Under Test - SUT), not its dependencies (database, network services, other classes). Use mocking frameworks like Mockito to create mock objects that simulate the behavior of these dependencies. This ensures tests are fast, reliable, and truly test the unit in isolation.

**Good example:**

```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("Should return user when found by id")
    void findUserById_Success() {
        // Given
        User expectedUser = new User("123", "John Doe");
        when(userRepository.findById("123")).thenReturn(Optional.of(expectedUser));

        // When
        Optional<User> actualUser = userService.findUserById("123");

        // Then
        assertThat(actualUser).isPresent().contains(expectedUser);
        verify(userRepository, times(1)).findById("123");
        verifyNoMoreInteractions(userRepository);
    }
}
```

**Bad example:**

```java
import org.junit.jupiter.api.Test;

class UserServiceTestBad {

    @Test
    void findUserById() {
        // Bad: Using real dependencies instead of mocks
        DatabaseConnection connection = new DatabaseConnection("localhost", 5432);
        UserRepository userRepository = new PostgresUserRepository(connection);
        UserService userService = new UserService(userRepository);

        // This test depends on database availability and state
        Optional<User> user = userService.findUserById("123");

        // Test is slow, brittle, and doesn't isolate the unit under test
        assertThat(user).isPresent();
    }
}
```

### Example 9: Consider Test Coverage, But Don't Obsess

Title: Use code coverage as a guide, not a definitive quality metric
Description: Tools like JaCoCo can measure which lines of code are executed by your tests (code coverage). Aiming for high coverage (e.g., >80% line/branch coverage) is generally good practice, as it indicates most code paths are tested. However, 100% coverage doesn't guarantee bug-free code or high-quality tests. Focus on writing meaningful tests for critical logic and edge cases rather than solely chasing coverage numbers. A test might cover a line but not actually verify its correctness effectively.

### Example 10: Test Scopes

Title: Package-private visibility for test classes and methods
Description: Test classes should have package-private visibility. There is no need for them to be public. Test methods should also have package-private visibility. There is no need for them to be public.

### Example 11: Code Splitting Strategies

Title: Organize test code effectively
Description: - **Small Test Methods:** Keep test methods small and focused on testing a single behavior. - **Helper Methods:** Use helper methods to avoid code duplication in test setup and assertions. - **Parameterized Tests:** Utilize JUnit's parameterized tests to test the same logic with different input values.

### Example 12: Anti-patterns and Code Smells

Title: Common testing mistakes to avoid
Description: - **Testing Implementation Details:** Avoid testing implementation details that might change, leading to brittle tests. Focus on testing behavior and outcomes. - **Hard-coded Values:** Avoid hard-coding values in tests. Use constants or test data to make tests more maintainable. - **Complex Test Logic:** Keep test logic simple and avoid complex calculations or conditional statements within tests. - **Ignoring Edge Cases:** Don't ignore edge cases or boundary conditions. Ensure tests cover a wide range of inputs, including invalid or unexpected values. - **Slow Tests:** Avoid slow tests that discourage developers from running them frequently. - **Over-reliance on Mocks:** Mock judiciously; too many mocks can obscure the actual behavior and make tests less reliable. - **Ignoring Test Failures:** Never ignore failing tests. Investigate and fix them promptly.

### Example 13: State Management

Title: Managing test state effectively
Description: - **Isolated State:** Ensure each test has its own isolated state to avoid interference between tests. Use `@BeforeEach` to reset the state before each test. - **Immutable Objects:** Prefer immutable objects to simplify state management and avoid unexpected side effects. - **Stateless Components:** Design stateless components whenever possible to reduce the need for state management in tests.

### Example 14: Error Handling

Title: Testing exception scenarios effectively
Description: - **Expected Exceptions:** Use AssertJ's `assertThatThrownBy` to verify that a method throws the expected exception under specific conditions. - **Exception Messages:** Assert the exception message to ensure the correct error is being thrown with helpful context. - **Graceful Degradation:** Test how the application handles errors and gracefully degrades when dependencies are unavailable.

### Example 15: Leverage JSpecify for Null Safety

Title: Utilize JSpecify annotations for explicit nullness contracts
Description: Employ JSpecify annotations (`org.jspecify.annotations.*`) such as `@NullMarked`, `@Nullable`, and `@NonNull` to clearly define the nullness expectations of method parameters, return types, and fields within your tests and the code under test. This practice enhances code clarity, enables static analysis tools to catch potential `NullPointerExceptions` early, and improves the overall robustness of your tests and application code.

**Good example:**

```java
@NullMarked
@ExtendWith(MockitoExtension.class)
class MyProcessorTest {

    @Mock
    private DataService mockDataService;

    private MyProcessor myProcessor;

    @Test
    void should_handleNullData_when_serviceReturnsNull() {
        // Given
        myProcessor = new MyProcessor(mockDataService);
        String key = "testKey";
        when(mockDataService.getData(key)).thenReturn(null);
        when(mockDataService.processData(null)).thenReturn("processed:null");

        // When
        String result = myProcessor.execute(key);

        // Then
        assertThat(result).isEqualTo("processed:null");
    }
}
```

**Bad example:**

```java
// No JSpecify annotations, nullness is ambiguous
class MyProcessorTest {
    @Test
    void testProcessing() {
        // Ambiguity: if getData returns null, this test might pass or fail unexpectedly
        when(mockDataService.getData(key)).thenReturn("someData");
        when(mockDataService.processData("SOMEDATA")).thenReturn("processed:SOMEDATA");

        String result = myProcessor.execute(key);

        assertThat(result).isEqualTo("processed:SOMEDATA");
    }
}
```

### Example 16: Key Questions to Guide Test Creation (RIGHT-BICEP)

Title: Comprehensive testing approach using RIGHT-BICEP
Description: - If the code ran correctly, how would I know? - How am I going to test this? - What else can go wrong? - Could this same kind of problem happen anywhere else? - What to Test: Use Your RIGHT-BICEP - Are the results **Right**? - Are all the **Boundary** conditions CORRECT? - Can you check **Inverse** relationships? - Can you **Cross-check** results using other means? - Can you force **Error** conditions to happen? - Are **Performance** characteristics within bounds?

**Good example:**

```java
public class CalculatorTest {

    private final Calculator calculator = new Calculator();

    // R - Right results
    @Test
    void add_simplePositiveNumbers_returnsCorrectSum() {
        assertThat(calculator.add(2, 3)).isEqualTo(5);
    }

    // B - Boundary conditions
    @Test
    void add_numberAndZero_returnsNumber() {
        assertThat(calculator.add(5, 0)).isEqualTo(5);
    }

    @Test
    void add_nearMaxInteger_returnsCorrectSum() {
        assertThat(calculator.add(Integer.MAX_VALUE - 1, 1)).isEqualTo(Integer.MAX_VALUE);
    }

    // C - Cross-check (commutative property)
    @Test
    void add_commutativeProperty_holdsTrue() {
        assertThat(calculator.add(2, 3)).isEqualTo(calculator.add(3, 2));
    }

    // E - Error conditions (overflow)
    @Test
    void add_integerOverflow_throwsArithmeticException() {
        assertThatThrownBy(() -> calculator.add(Integer.MAX_VALUE, 1))
            .isInstanceOf(ArithmeticException.class)
            .hasMessageContaining("overflow");
    }
}
```

**Bad example:**

```java
// Test only covers one simple case
public class CalculatorTestPoor {
    private final Calculator calculator = new Calculator();

    @Test
    void add_basicTest() {
        assertThat(calculator.add(2, 2)).isEqualTo(4); // Only testing one happy path
    }
}
```

### Example 17: Characteristics of Good Tests (A-TRIP)

Title: Ensuring tests follow A-TRIP principles
Description: Good tests are A-TRIP: - **Automatic**: Tests should run without human intervention. - **Thorough**: Test everything that could break; cover edge cases. - **Repeatable**: Tests should produce the same results every time, in any environment. - **Independent**: Tests should not rely on each other or on the order of execution. - **Professional**: Test code is real code; keep it clean, maintainable, and well-documented.

**Good example:**

```java
public class OrderProcessorTest {

    private OrderProcessor processor;

    // Automatic: Part of JUnit test suite, runs with build tools.
    // Independent: Each test sets up its own state.
    @BeforeEach
    void setUp() {
        processor = new OrderProcessor(); // Fresh instance for each test
    }

    // Thorough: Testing adding valid items.
    @Test
    void addItem_validItem_increasesCount() {
        processor.addItem("Laptop");
        assertThat(processor.getItemCount()).isEqualTo(1);
        processor.addItem("Mouse");
        assertThat(processor.getItemCount()).isEqualTo(2);
    }

    // Thorough: Testing an edge case (adding null).
    @Test
    void addItem_nullItem_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> processor.addItem(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // Professional: Code is clean, uses meaningful names, follows conventions.
}
```

**Bad example:**

```java
public class BadOrderProcessorTest {
    // Violates Independent: static processor shared between tests
    private static OrderProcessor sharedProcessor = new OrderProcessor();

    @Test
    void test1_addItem() {
        // Assumes this runs first or that sharedProcessor is empty.
        sharedProcessor.addItem("Book");
        assertThat(sharedProcessor.getItemCount()).isEqualTo(1); // Might fail if other tests run first
    }

    @Test
    void test2_addAnotherItem() {
        sharedProcessor.addItem("Pen");
        // The expected count depends on whether test1_addItem ran and succeeded.
        assertThat(sharedProcessor.getItemCount()).isGreaterThan(0); // Weak assertion
    }
}
```

### Example 18: Verifying CORRECT Boundary Conditions

Title: Comprehensive boundary condition testing using CORRECT
Description: Ensure your tests check the following boundary conditions (CORRECT): - **Conformance**: Does the value conform to an expected format? - **Ordering**: Is the set of values ordered or unordered as appropriate? - **Range**: Is the value within reasonable minimum and maximum values? - **Reference**: Does the code reference anything external that isn't under direct control? - **Existence**: Does the value exist? (e.g., is non-null, non-zero, present in a set) - **Cardinality**: Are there exactly enough values? - **Time**: Is everything happening in order? At the right time? In time?

**Good example:**

```java
public class UserValidationTest {
    private final UserValidation validator = new UserValidation();

    // Testing Range for age
    @Test
    void isAgeValid_ageAtLowerBound_returnsTrue() {
        assertThat(validator.isAgeValid(18)).isTrue();
    }

    @Test
    void isAgeValid_ageAtUpperBound_returnsTrue() {
        assertThat(validator.isAgeValid(120)).isTrue();
    }

    @Test
    void isAgeValid_ageBelowLowerBound_returnsFalse() {
        assertThat(validator.isAgeValid(17)).isFalse();
    }

    // Testing Conformance for email
    @ParameterizedTest
    @ValueSource(strings = {"user@example.com", "user.name@sub.example.co.uk"})
    void isValidEmailFormat_validEmails_returnsTrue(String email) {
        assertThat(validator.isValidEmailFormat(email)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"userexample.com", "user@", "@example.com"})
    void isValidEmailFormat_invalidEmails_returnsFalse(String email) {
        assertThat(validator.isValidEmailFormat(email)).isFalse();
    }

    // Testing Existence for username
    @Test
    void processUsername_nullUsername_returnsFalse() {
        assertThat(validator.processUsername(null)).isFalse();
    }

    @Test
    void processUsername_emptyUsername_returnsFalse() {
        assertThat(validator.processUsername("")).isFalse();
    }
}
```

**Bad example:**

```java
// Only testing one happy path for age validation, ignoring boundaries.
public class UserValidationPoorTest {
    private final UserValidation validator = new UserValidation();

    @Test
    void isAgeValid_typicalAge_returnsTrue() {
        assertThat(validator.isAgeValid(25)).isTrue(); // Only one value tested
    }

    @Test
    void isValidEmailFormat_typicalEmail_returnsTrue() {
        assertThat(validator.isValidEmailFormat("test@example.com")).isTrue(); // No invalid formats, no nulls
    }
}
```

## Output Format

- **ANALYZE** Java test code to identify specific unit testing issues and categorize them by impact (CRITICAL, MAINTAINABILITY, PERFORMANCE, COVERAGE, RELIABILITY) and testing area (framework usage, assertion style, test structure, test independence, coverage gaps)
- **CATEGORIZE** unit testing improvements found: Framework Issues (JUnit 4 vs JUnit 5 modern features, outdated annotations vs current testing capabilities), Assertion Problems (basic JUnit assertions vs expressive AssertJ fluent assertions, unclear error messages vs descriptive failure descriptions), Test Structure Issues (poor naming vs descriptive test names, disorganized tests vs Given-When-Then structure, missing documentation vs clear test intent), Test Independence Problems (shared state issues vs isolated test execution, test order dependencies vs independent test methods), and Coverage Gaps (missing boundary conditions vs comprehensive edge case testing, untested error scenarios vs complete exception handling validation, insufficient parameterized testing vs thorough input validation)
- **APPLY** unit testing best practices directly by implementing the most appropriate improvements for each identified issue: Migrate to JUnit 5 with modern annotations and features, adopt AssertJ for expressive and readable assertions, implement Given-When-Then structure with descriptive test naming, ensure test independence through proper setup and teardown, eliminate shared state between tests, implement comprehensive boundary testing using RIGHT-BICEP principles, add parameterized tests for thorough input validation, and establish proper mocking strategies with Mockito for external dependencies
- **IMPLEMENT** comprehensive unit testing refactoring using proven patterns: Establish modern JUnit 5 test structure with @Test, @BeforeEach, @AfterEach, and lifecycle annotations, integrate AssertJ assertions for fluent and expressive test validation, apply Given-When-Then methodology with clear test organization and descriptive naming, implement test independence through proper resource management and state isolation, create comprehensive boundary testing covering RIGHT-BICEP scenarios (Right results, Inverse relationships, Cross-checks, Error conditions, Performance, Existence), and integrate parameterized testing for thorough input validation and edge case coverage
- **REFACTOR** test code systematically following the unit testing improvement roadmap: First migrate test framework to JUnit 5 with modern annotations and capabilities, then adopt AssertJ for expressive assertions and better error messages, restructure tests using Given-When-Then methodology with descriptive naming, ensure test independence by eliminating shared state and order dependencies, implement comprehensive boundary testing and edge case coverage, integrate parameterized testing for thorough validation, and establish proper mocking strategies for external dependencies and complex interactions
- **EXPLAIN** the applied unit testing improvements and their benefits: Test maintainability enhancements through JUnit 5 modern features and clear test structure, readability improvements via AssertJ expressive assertions and Given-When-Then organization, reliability gains from test independence and proper state management, coverage improvements through comprehensive boundary testing and parameterized validation, and debugging capabilities enhancement through descriptive test names and detailed assertion messages
- **VALIDATE** that all applied unit testing refactoring compiles successfully, maintains existing test functionality, improves test reliability and maintainability, achieves comprehensive test coverage, and follows established testing best practices through comprehensive verification and test execution

## Safeguards

- **BLOCKING SAFETY CHECK**: ALWAYS run `./mvnw compile` before ANY testing recommendations to ensure project stability
- **CRITICAL VALIDATION**: Execute `./mvnw clean verify` to ensure all existing tests pass before implementing new test strategies
- **MANDATORY VERIFICATION**: Confirm all existing functionality remains intact after applying any test improvements
- **ROLLBACK REQUIREMENT**: Ensure all test changes can be easily reverted if they introduce compilation or runtime issues
- **INCREMENTAL SAFETY**: Apply test improvements incrementally, validating compilation and test execution after each modification
- **DEPENDENCY VALIDATION**: Verify that any new testing dependencies (AssertJ, Mockito extensions) are properly configured and compatible
- **TEST ISOLATION VERIFICATION**: Ensure new tests don't introduce dependencies between test methods or classes
- **PERFORMANCE MONITORING**: Validate that test execution times remain reasonable and don't significantly impact build performance
