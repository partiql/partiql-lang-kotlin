package org.partiql.examples

import com.amazon.ion.system.*
import org.partiql.examples.util.Example
import org.partiql.lang.*
import org.partiql.lang.eval.*
import java.io.PrintStream

/** A simple fibonacci calculator. */
private fun calcFib(n: Long): Long = when (n) {
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
        if (argN.type != ExprValueType.INT) {
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
        if (argN.type != ExprValueType.INT) {
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
class CustomFunctionsExample(out: PrintStream) : Example(out) {

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

    override fun run() {
        listOf(
                "fib_scalar(NULL)",
                "fib_scalar(MISSING)",
                "fib_scalar(0)",
                "fib_scalar(1)",
                "fib_scalar(2)",
                "fib_scalar(3)",
                "fib_scalar(4)",
                "fib_scalar(5)",
                "fib_scalar(6)",
                "fib_scalar(7)",
                "fib_scalar(8)",
                "fib_list(NULL)",
                "fib_list(MISSING)",
                "fib_list(0)",
                "fib_list(1)",
                "fib_list(2)",
                "fib_list(3)",
                "fib_list(4)",
                "fib_list(5)",
                "fib_list(6)",
                "fib_list(7)",
                "fib_list(8)").forEach { query -> print(query, eval(query)) }
    }
}
