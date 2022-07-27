package org.partiql.lang.util.testdsl

import com.amazon.ion.IonValue
import org.junit.jupiter.api.fail
import org.partiql.lang.ION
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.mockdb.MockDb
import org.partiql.lang.util.createPartiqlIonSchemaSystem

/**
 * Defines an entire test suite.
 *
 * [globals] define the global variables on which the tests depend.
 * [groups] a list of all the groups containing the individual tests.
 * [factoryBlock] because we do not have an [ExprValueFactory] instance when the test suite is defined,
 * this is a lambda in order to defer execution to a time in which we do.
 */
data class IonResultTestSuite(
    val globals: Map<String, IonValue>,
    val groups: List<IonResultTestGroup>,
    val factoryBlock: (ExprValueFactory) -> List<ExprValue>
) {

    /**
     * Gets all the tests that are not in the specified skip list.
     *
     * [failingTestNames] contains a list of test names that are known to be failing.  These tests
     * are returned with [IonResultTestCase.expectFailure] set to `true`.  Depending on the current
     * use of the test suite they can either be a) filtered out, or b) run anyway, but with an
     * assertion that an exception is thrown.  The latter is a means to ensure that passing tests
     * are kept out of the fail list.
     *
     * Additionally, if [failingTestNames] includes a non-existent test, a message is printed
     * to the console and an exception is thrown.
     */
    fun getAllTests(
        failingTestNames: Set<String> = emptySet()
    ): List<IonResultTestCase> {
        val allTests = groups.flatMap { category -> category.tests.map { it.copy(group = category.name) } }
        // find any names in the skip list that are not actual tests (to help keep the skip list clean and sane)
        val invalidFailListNames = (failingTestNames.filter { failEntry -> allTests.none { it.name == failEntry } })
        if (invalidFailListNames.any()) {
            println("The following failing test names entries do not match the name of any test:")
            invalidFailListNames.forEach {
                println("\"$it\"")
            }
            fail("invalid failing test names found, see console")
        }

        return allTests.map {
            it.copy(expectFailure = failingTestNames.contains(it.name))
        }
    }

    /**
     * Calls [getAllTests] and then maps the result to a [List<ExprNodeTestCase>].
     */
    fun allTestsAsExprNodeTestCases(failingTestNames: Set<String> = emptySet()) = getAllTests(failingTestNames)
        .filter { !it.expectFailure }
        .map { it.toExprNodeTestCase() }

    /** Invokes [factoryBlock] to create the parameters needed for the tests in the suite. */
    fun createParameters(vf: ExprValueFactory) = factoryBlock(vf)

    /** Instantiates an instance of [MockDb] with values and types instantiated using [globals]. */
    fun mockDb(valueFactory: ExprValueFactory): MockDb =
        MockDb(globals, valueFactory, createPartiqlIonSchemaSystem(ION))
}

/**
 * Defines a simple API of defining a suite of evaluation tests as data.
 *
 * For now we just concern ourselves with the success case where the result is the same for both typing modes.
 *
 * However, in the future we will need to be able to express different types of expected result:
- Most have the same result for both typing modes
- May have a different result for each typing mode where a "result" is defined as:
- an Ion value resulting from successful query execution
- an error code with col, line and other properties.
 */
internal fun defineTestSuite(block: SuiteBuilder.() -> Unit): IonResultTestSuite =
    SuiteBuilderImpl().apply(block).build()
