package org.partiql.examples

import org.partiql.examples.util.Example
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.eval.BaseExprValue
import org.partiql.lang.eval.Bindings
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.namedValue
import java.io.PrintStream

/**
 * Demonstrates how to implement a custom [ExprValue] type.  Custom implementations of [ExprValue] such as this one
 * are used to feed data from any source into the PartiQL interpreter in the form of global variables.
 *
 * This implementation of [ExprValue] represents a single row within a comma-separated value (CSV) file.
 *
 * The first column in the row will be assigned the name `_1`, the second `_2` and so on.
 */
private class CsvRowExprValue(private val rowString: String) : BaseExprValue() {

    /** The Ion type that CsvRowExprValue is must similar to is a struct. */
    override val type: ExprValueType get() = ExprValueType.STRUCT

    /**
     * The lazily constructed set of row values.  Laziness is good practice here because it avoids
     * constructing this map if it isn't needed.
     */
    private val rowValues: Map<String, ExprValue> by lazy {
        rowString.split(',')
            .mapIndexed { i, it ->
                val fieldName = "_${i + 1}"
                // Note that we invoke
                fieldName to ExprValue.newString(it).namedValue(ExprValue.newString(fieldName))
            }.toMap()
    }

    /** An iterator over the values contained in this instance of [ExprValue], if any. */
    override fun iterator() = rowValues.values.iterator()

    private val bindingsInstance by lazy {
        Bindings.ofMap(rowValues)
    }

    override val bindings: Bindings<ExprValue>
        get() = bindingsInstance
}

class CsvExprValueExample(out: PrintStream) : Example(out) {

    private val pipeline = CompilerPipeline.standard()

    private val EXAMPLE_CSV_FILE_CONTENTS = "Cat,Nibbler,F\nCat,Hobbes,M\nDog,Fido,M"

    /** Evaluates the specified [query], using a standard [session] if none was specified. */
    private fun eval(query: String, session: EvaluationSession = EvaluationSession.standard()): ExprValue {
        val e = pipeline.compile(query)
        return e.eval(session)
    }

    override fun run() {
        print("CSV file:", EXAMPLE_CSV_FILE_CONTENTS)

        val globals = Bindings.buildLazyBindings<ExprValue> {
            addBinding("csv_data") {
                // The first time "csv_data" is encountered during query evaluation this closure will be
                // invoked to obtain its value, which will then be cached for later use.

                // [SequenceExprValue] represents a PartiQL bag data type.  It is an implementation of [ExprValue] that
                // contains a Kotlin [Sequence<>] of other [ExprValue] instances.
                ExprValue.newBag(
                    EXAMPLE_CSV_FILE_CONTENTS.split('\n').asSequence()
                        .filter { it.isNotEmpty() }
                        .map {
                            CsvRowExprValue(it)
                        }
                )
            }
        }

        val query = "SELECT _1, _2, _3 FROM csv_data"
        print("PartiQL query:", query)

        val result = eval(query, EvaluationSession.build { globals(globals) })
        print("result:", result)
    }
}
