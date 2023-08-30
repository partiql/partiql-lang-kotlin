# PartiQL Code Coverage

PartiQL has released a way for you to write unit tests and gather code coverage statistics for your PartiQL source
under `org.partiql.coverage`!

## Writing Tests

In order to write tests, you'll need to use `@PartiQLTest` in combination with `PartiQLTestProvider` and `PartiQLTestCase`. Here's
an example:

```kotlin
/**
 * Here's an example of a test method. This method will get loaded up by JUnit 5's
 * test framework and be executed.
 *
 * Note that each test method annotated with @PartiQLTest needs to take in an implementation of a PartiQLTestCase and a PartiQLResult.
 * Note that PartiQLResult is a sealed interface -- you can opt to assume the variant that will be returned, or you can
 * just take in a PartiQLResult.
 */
@PartiQLTest(provider = SimpleTestProvider::class)
fun simpleTest(tc: SimpleTestCase, result: PartiQLResult.Value) {
    val exprValue = result.value
    assertEquals(ExprValueType.BOOL, exprValue.type)
    assertEquals(tc.expected, exprValue.booleanValue())
}

/**
 * PartiQLTestProviders represent a set of tests aimed at testing a single query.
 */
class SimpleTestProvider : PartiQLTestProvider {
    override val statement: String = "EXISTS(SELECT * FROM << x >> AS t WHERE t < 7)"
    override fun getTestCases(): List<PartiQLTestCase> = listOf(
        SimpleTestCase(x = 6, expected = true),
        SimpleTestCase(x = 8, expected = false)
    )
}

/**
 * An implementation of a PartiQLTestCase. Note that we pass in
 */
class SimpleTestCase(private val x: Int, val expected: Boolean) : PartiQLTestCase {
    override val session: EvaluationSession = EvaluationSession.builder {
        globals(mapOf("x" to x))
    }
}
```

Note that, similar to how it is written above, all test methods using `@PartiQLTest` need two parameters (the first being
an implementation of `PartiQLTestCase` and the second being `PartiQLResult` or one of its variants).

## JUnit Integration

`@PartiQL` integrates seamlessly with JUnit 5, and therefore, you can run tests directly from your IDE and use other JUnit 5
annotations such as `@Ignore`, `@Disabled`, `@Timeout`, and more, 

## Other Features

PartiQL Code Coverage allows you to generate LCOV reports for branch and branch-condition coverage, generate HTML reports,
and fail builds on configured thresholds.

## Getting Started

To learn more, please refer to [PartiQL Code Coverage's README](https://github.com/partiql/partiql-lang-kotlin/tree/main/partiql-coverage)!
