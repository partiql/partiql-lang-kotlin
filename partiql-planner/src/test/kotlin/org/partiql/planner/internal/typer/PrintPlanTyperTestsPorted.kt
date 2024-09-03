package org.partiql.planner.internal.typer

import com.amazon.ion.IonType
import com.amazon.ion.IonWriter
import com.amazon.ion.system.IonTextWriterBuilder
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.partiql.planner.TyperTestBuilder
import org.partiql.types.PType
import java.io.OutputStream
import kotlin.streams.toList

/**
 * This serializes the [PlanTyperTestsPorted].
 * TODO: Remove this once we have a better way to serialize errors.
 *
 * @see TyperTests
 */
class PrintPlanTyperTestsPorted {

    /**
     * This "test" serializes the [PlanTyperTestsPorted] as Ion. It is copied and pasted into the resource folder.
     */
    @Test
    @Disabled
    fun print() {
        val other = PlanTyperTestsPorted.TestProvider().provideArguments(null).map { it.get()[0] as PlanTyperTestsPorted.TestCase }.toList()
        val testCases =
            other + PlanTyperTestsPorted.collections() + PlanTyperTestsPorted.decimalCastCases() + PlanTyperTestsPorted.selectStar() + PlanTyperTestsPorted.sessionVariables() + PlanTyperTestsPorted.bitwiseAnd() + PlanTyperTestsPorted.unpivotCases() + PlanTyperTestsPorted.joinCases() + PlanTyperTestsPorted.excludeCases() + PlanTyperTestsPorted.orderByCases() + PlanTyperTestsPorted.tupleUnionCases() + PlanTyperTestsPorted.aggregationCases() + PlanTyperTestsPorted.scalarFunctions() + PlanTyperTestsPorted.distinctClauseCases() + PlanTyperTestsPorted.pathExpressions() + PlanTyperTestsPorted.caseWhens() + PlanTyperTestsPorted.nullIf() + PlanTyperTestsPorted.coalesce() + PlanTyperTestsPorted.subqueryCases() + PlanTyperTestsPorted.dynamicCalls() + PlanTyperTestsPorted.scanCases() + PlanTyperTestsPorted.pivotCases() + PlanTyperTestsPorted.isTypeCases() + PlanTyperTestsPorted.castCases()
        testCases.forEachIndexed { index, it ->
            it.print(index)
        }
    }

    private fun PlanTyperTestsPorted.TestCase.print(index: Int) {
        val writer = IonTextWriterBuilder.pretty().withLongStringThreshold(50).build(System.out as OutputStream)
        when (this) {
            is PlanTyperTestsPorted.TestCase.SuccessTestCase -> writer.print(this, index) {
                it.setFieldName(TyperTestBuilder.FIELD_STATUS)
                it.writeSymbol(TyperTestBuilder.STATUS_SUCCESS)
                it.setFieldName(TyperTestBuilder.FIELD_EXPECTED)
                it.writeType(this.expected)
            }

            is PlanTyperTestsPorted.TestCase.ErrorTestCase -> writer.print(this, index) {
                it.setFieldName(TyperTestBuilder.FIELD_STATUS)
                it.writeSymbol(TyperTestBuilder.STATUS_FAILURE)
                if (this.problemHandler != null) {
                    it.writeProblemHandler(this.problemHandler)
                }
            }

            is PlanTyperTestsPorted.TestCase.ThrowingExceptionTestCase -> error("Not implemented")
        }
    }

    private fun IonWriter.print(tc: PlanTyperTestsPorted.TestCase, index: Int, expectedFields: (IonWriter) -> Unit) {
        this.addTypeAnnotation("test")
        this.stepIn(IonType.STRUCT)

        // Write name
        this.setFieldName("name")
        this.writeString(tc.name ?: "no-name-found-$index")

        // Write type
        this.setFieldName("type")
        this.writeString("type")

        // Write payload
        this.setFieldName("body")
        this.stepIn(IonType.STRUCT)

        // Write statement
        this.setFieldName(TyperTestBuilder.FIELD_STATEMENT)
        writeStatement(this, tc)

        // Session
        this.setFieldName(TyperTestBuilder.FIELD_SESSION)
        writeSession(this, tc)

        // Expectation
        expectedFields.invoke(this)
        this.stepOut()
        this.stepOut()
        this.flush()
    }

    private fun writeStatement(writer: IonWriter, tc: PlanTyperTestsPorted.TestCase) {
        if (tc.query != null) {
            writer.writeString(tc.query)
        }
        val key = tc.key
        if (key != null) {
            writer.stepIn(IonType.SEXP)
            writer.writeSymbol("@")
            writer.writeString("\$inputs")
            writer.writeString(key.group)
            writer.writeString(key.name)
            writer.stepOut()
        }
    }

    private fun writeSession(writer: IonWriter, tc: PlanTyperTestsPorted.TestCase) {
        writer.stepIn(IonType.STRUCT)
        writer.setFieldName(TyperTestBuilder.FIELD_CATALOG)
        writer.writeString(tc.catalog)
        writer.setFieldName(TyperTestBuilder.FIELD_CWD)
        writer.stepIn(IonType.LIST)
        tc.catalogPath.forEach { writer.writeString(it) }
        writer.stepOut()
        writer.stepOut()
    }

    private fun IonWriter.writeType(type: PType) {
        when (val kind = type.kind) {
            // No params
            PType.Kind.DYNAMIC, PType.Kind.BOOL, PType.Kind.TINYINT, PType.Kind.SMALLINT, PType.Kind.INTEGER, PType.Kind.BIGINT, PType.Kind.REAL, PType.Kind.DOUBLE, PType.Kind.DATE, PType.Kind.STRUCT, PType.Kind.UNKNOWN, PType.Kind.STRING, PType.Kind.SYMBOL, PType.Kind.NUMERIC -> this.writeSymbol(
                kind.name
            )

            // Override name
            PType.Kind.DECIMAL_ARBITRARY -> this.writeSymbol("DECIMAL")

            // Precision and scale
            PType.Kind.DECIMAL -> {
                this.stepIn(IonType.SEXP)
                this.writeSymbol(kind.name)
                this.writeInt(type.precision.toLong())
                this.writeInt(type.scale.toLong())
                this.stepOut()
            }

            // Uses Length
            PType.Kind.VARCHAR, PType.Kind.BLOB, PType.Kind.CLOB, PType.Kind.CHAR -> {
                this.stepIn(IonType.SEXP)
                this.writeSymbol(kind.name)
                this.writeInt(type.length.toLong())
                this.stepOut()
            }

            // Uses type param
            PType.Kind.BAG, PType.Kind.SEXP, PType.Kind.ARRAY -> {
                this.stepIn(IonType.SEXP)
                this.writeSymbol(kind.name)
                this.writeType(type.typeParameter)
                this.stepOut()
            }

            // Precision
            PType.Kind.TIME, PType.Kind.TIMEZ, PType.Kind.TIMESTAMP, PType.Kind.TIMESTAMPZ -> {
                this.stepIn(IonType.SEXP)
                this.writeSymbol(kind.name)
                this.writeInt(type.precision.toLong())
                this.stepOut()
            }

            // Row
            PType.Kind.ROW -> {
                this.stepIn(IonType.SEXP)
                this.writeSymbol(kind.name)
                type.fields.forEach { field ->
                    this.writeString(field.name)
                    this.writeType(field.type)
                }
                this.stepOut()
            }
        }
    }

    // TODO: We need to make error reporting more flexible. Potentially create error codes to assert on with
    //  attributes attached.
    private fun IonWriter.writeProblemHandler(handler: PlanTyperTestsPorted.Companion.AssertProblemExists) {
        this.setFieldName(TyperTestBuilder.FIELD_ASSERT_PROBLEM_EXISTS)
        this.stepIn(IonType.SEXP)
        this.writeSymbol(handler.problem.details.severity.name)
        this.writeString(handler.problem.details.message)
        this.stepOut()
    }
}
