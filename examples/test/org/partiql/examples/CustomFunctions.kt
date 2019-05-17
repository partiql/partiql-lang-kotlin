package org.partiql.examples

import org.junit.*
import com.amazon.ion.system.*
import org.partiql.lang.*
import org.partiql.lang.eval.*
import kotlin.test.*

/** A simple fibonacci calculator. */
private fun calcFib(n: Long): Long = when(n) {
    0L -> 0L
    1L -> 1L
    else -> calcFib(n - 1L) + calcFib(n - 2L)
}

/**
 * A simple custom function that calculates the value of the nth position in the
 * fibonacci sequence.  This is analogous to a scalar-valued function in a traditional
 * SQL implementation.
 *
 * This example derives from [NullPropagatingExprFunction].  Null-propogation causes
 * the function to return `NULL` if any of its arguments are `NULL` or `MISSING`.
 *
 * If the arguments of the function should *not* trigger null-propagation (e.g.
 * `COALESCE`), the [ExprFunction] interface should be implemented directly.
 */
class FibScalarExprFunc(valueFactory: ExprValueFactory) : NullPropagatingExprFunction("fib_scalar", 1, valueFactory) {
    override fun eval(env: Environment, args: List<ExprValue>): ExprValue {

        // [NullPropagatingExprFunction] also checks arity of the function call, so
        // there is no need to ensure [args] is the correct size.
        // However, at the moment there is no facility for ensuring that the arguments are
        // of the correct type, so each function must still be responsible for that.

        val argN = args.first()
        if(argN.type != ExprValueType.INT) {
            // The exception thrown is not flagged as an internal error message because
            // it is caused by user input (either by the query or by the data).
            throw EvaluationException("Argument to $name was not an integer", internal = false)
        }

        val n = argN.scalar.numberValue()!!.toLong()

        return valueFactory.newInt(calcFib(n))
    }
}

/**
 * A simple custom function that returns a list of structs with fields containing
 * the first `n` members of the fibonacci sequence.
 *
 * This is similar to [FibScalarExprFunc].  However, returning a list of structs in this
 * fashion demonstrates how one could implement what would be known as a table-valued
 * function in a traditional SQL implementation.
 */
class FibListExprFunc(valueFactory: ExprValueFactory) : NullPropagatingExprFunction("fib_list", 1, valueFactory) {
    override fun eval(env: Environment, args: List<ExprValue>): ExprValue {

        val argN = args.first()
        if(argN.type != ExprValueType.INT) {
            throw EvaluationException("Argument to $name was not an integer", internal = false)
        }

        // `!!` is safe here because of the type check above.
        val n = argN.scalar.numberValue()!!.toLong()

        val listElements = (0L..n).map { i ->
            // Due to the call to .asSequence() below, this closure will be lazily executed
            // to fetch one element at a time as they are needed.
            val fieldValue = valueFactory.newInt(calcFib(i)).namedValue(valueFactory.newString("n"))
            valueFactory.newStruct(sequenceOf(fieldValue), StructOrdering.UNORDERED)
        }.asSequence()

        return valueFactory.newList(listElements)
    }
}

/**
 * Demonstrates the use of [FibScalarExprFunc] and [FibListExprFunc] in queries.
 *
 * The important parts of this are how [CustomFunctionsTest.compiler] is instantiated and
 * the two custom functions: [FibScalarExprFunc] and [FibListExprFunc] are implemented.
 */
class CustomFunctionsTest {

    private val ion = IonSystemBuilder.standard().build()

    /**
     * To make custom functions available to the PartiQL being executed, they must be passed to
     * [CompilerPipeline.Builder.addFunction].
     */
    val pipeline = CompilerPipeline.build(ion) {
        addFunction(FibScalarExprFunc(valueFactory))
        addFunction(FibListExprFunc(valueFactory))
    }

    /** Evaluates the given [query] with as standard [EvaluationSession]. */
    private fun eval(query: String): ExprValue {
       val e = pipeline.compile(query)
       return e.eval(EvaluationSession.standard())
    }

    @Test
    fun testFibScalar() {
        runTest("fib_scalar(NULL)", "NULL")
        runTest("fib_scalar(MISSING)", "MISSING")
        runTest("fib_scalar(0)", "0")
        runTest("fib_scalar(1)", "1")
        runTest("fib_scalar(2)", "1")
        runTest("fib_scalar(3)", "2")
        runTest("fib_scalar(4)", "3")
        runTest("fib_scalar(5)", "5")
        runTest("fib_scalar(6)", "8")
        runTest("fib_scalar(7)", "13")
        runTest("fib_scalar(8)", "21")
    }

    @Test
    fun testFibList() {
        runTest("fib_list(NULL)", "NULL")
        runTest("fib_list(MISSING)", "MISSING")
        runTest("fib_list(0)", "[{ 'n': 0 }]")
        runTest("fib_list(1)", "[{ 'n': 0 }, { 'n': 1 }]")
        runTest("fib_list(2)", "[{ 'n': 0 }, { 'n': 1 }, { 'n': 1 }]")
        runTest("fib_list(3)", "[{ 'n': 0 }, { 'n': 1 }, { 'n': 1 }, { 'n': 2 }]")
        runTest("fib_list(4)", "[{ 'n': 0 }, { 'n': 1 }, { 'n': 1 }, { 'n': 2 }, { 'n': 3 }]")
        runTest("fib_list(5)", "[{ 'n': 0 }, { 'n': 1 }, { 'n': 1 }, { 'n': 2 }, { 'n': 3 }, { 'n': 5 }]")
        runTest("fib_list(6)", "[{ 'n': 0 }, { 'n': 1 }, { 'n': 1 }, { 'n': 2 }, { 'n': 3 }, { 'n': 5 }, { 'n': 8 }]")
        runTest("fib_list(7)", "[{ 'n': 0 }, { 'n': 1 }, { 'n': 1 }, { 'n': 2 }, { 'n': 3 }, { 'n': 5 }, { 'n': 8 }, { 'n': 13 }]")
        runTest("fib_list(8)", "[{ 'n': 0 }, { 'n': 1 }, { 'n': 1 }, { 'n': 2 }, { 'n': 3 }, { 'n': 5 }, { 'n': 8 }, { 'n': 13 }, { 'n': 21 }]")
    }

    fun runTest(query: String, expectedResult: String) {
        val expectedExprValue = eval(expectedResult)
        val actualExprValue = eval(query)

        // [DEFAULT_COMPARATOR] is an implementation of [kotlin.Comparator<ExprValue>] that
        // can be used to compare instances of [ExprValue] according to the same rules used
        // by [EvaluatingCompiler].
        assertEquals(0, DEFAULT_COMPARATOR.compare(expectedExprValue, actualExprValue),
                     "The query's result must match the expected value")
    }
}
