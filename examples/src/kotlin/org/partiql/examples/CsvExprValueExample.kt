package org.partiql.examples

import com.amazon.ion.*
import com.amazon.ion.system.*
import org.partiql.examples.util.Example
import org.partiql.lang.*
import org.partiql.lang.eval.*
import org.partiql.lang.util.*
import java.io.PrintStream

/**
 * Demonstrates how to implement a custom [ExprValue] type.  Custom implementations of [ExprValue] such as this one
 * are used to feed data from any source into the PartiQL interpreter in the form of global variables.
 *
 * This implementation of [ExprValue] represents a single row within a comma-separated value (CSV) file.
 *
 * The first column in the row will be assigned the name `_1`, the second `_2` and so on.
 */
private class CsvRowExprValue(private val valueFactory: ExprValueFactory, private val rowString: String): BaseExprValue() {

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
                fieldName to valueFactory.newString(it).namedValue(valueFactory.newString(fieldName))
            }.toMap()
    }

    /** Laziness is even more important here because [ionValue] is likely to never be called. */
    private val lazyIonValue by lazy {
        valueFactory.ion.newEmptyStruct().apply {
            rowValues.map { kvp ->
                add(kvp.key, valueFactory.ion.newString(kvp.value.stringValue()))
            }
            makeReadOnly()
        }
    }

    /** An iterator over the values contained in this instance of [ExprValue], if any. */
    override fun iterator() = rowValues.values.iterator()

    /** The Ion representation of the current value. */
    override val ionValue: IonValue
        get() = lazyIonValue

    private val bindingsInstance by lazy {
        Bindings.ofMap(rowValues)
    }

    override val bindings: Bindings<ExprValue>
        get() = bindingsInstance
}

class CsvExprValueExample(out: PrintStream) : Example(out) {

    private val ion = IonSystemBuilder.standard().build()
    private val valueFactory = ExprValueFactory.standard(ion)
    private val pipeline = CompilerPipeline.standard(valueFactory)

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
                valueFactory.newBag(
                    EXAMPLE_CSV_FILE_CONTENTS.split('\n').asSequence()
                        .filter { it.isNotEmpty() }
                        .map {
                            CsvRowExprValue(pipeline.valueFactory, it)
                        })
            }
        }

        val query = "SELECT _1, _2, _3 FROM csv_data"
        print("PartiQL query:", query)

        val result = eval(query, EvaluationSession.build { globals(globals) })
        print("result:", result)
    }
}
