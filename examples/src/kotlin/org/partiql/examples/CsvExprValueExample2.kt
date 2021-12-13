package org.partiql.examples

import com.amazon.ion.system.*
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.partiql.examples.util.Example
import org.partiql.lang.*
import org.partiql.lang.eval.*
import java.io.PrintStream
import java.io.StringReader

class CsvExprValueExample2(out: PrintStream) : Example(out) {

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

        val csvParser = CSVParser(StringReader(EXAMPLE_CSV_FILE_CONTENTS), CSVFormat.DEFAULT)

        val globals = Bindings.buildLazyBindings<ExprValue> {
            addBinding("csv_data") {
                // The first time "csv_data" is encountered during query evaluation this closure will be
                // invoked to obtain its value, which will then be cached for later use.

                // [SequenceExprValue] represents a PartiQL bag data type.  It is an implementation of [ExprValue] that
                // contains a Kotlin [Sequence<>] of other [ExprValue] instances.
                valueFactory.newBag(
                    csvParser.asSequence().map { csvRecord ->
                        valueFactory.newStruct(
                            csvRecord.mapIndexed { i, value ->
                                val fieldName = "_${i + 1}"
                                valueFactory.newString(value).namedValue(valueFactory.newString(fieldName))
                            },
                            StructOrdering.ORDERED
                        )
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
