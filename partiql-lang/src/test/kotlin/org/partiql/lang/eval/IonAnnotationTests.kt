package org.partiql.lang.eval

import com.amazon.ion.IonSystem
import com.amazon.ion.system.IonSystemBuilder
import org.junit.Test
import org.partiql.lang.CompilerPipeline

/**
 * The following tests demonstrate the current IonValue annotation behavior and tracks possible regressions in future commits.
 * This behavior is what the Kotlin implementation defines and is subject to change pending resolution of
 * https://github.com/partiql/partiql-spec/issues/63.
 */
class IonAnnotationTests : EvaluatorTestBase() {
    // Round-tripped Ion value with annotation (IonValue -> ExprValue -> IonValue) results in the elided annotation.
    @Test
    fun ionValueWithAnnotationExprValueRoundTrip() {
        val ion: IonSystem = IonSystemBuilder.standard().build()
        val ionValue = ion.singleValue("1")
        val exprValue = ExprValue.of(ion.singleValue("annotation::1"))
        val roundTripped = exprValue.toIonValue(ion)
        assertEquals(ionValue, roundTripped)
    }

    // Evaluated Ion Literal with annotation converted to an IonValue results in the elided annotation.
    @Test
    fun ionLiteralWithAnnotationEvaluation() {
        val pipeline = CompilerPipeline.standard()
        val ionValueAsString = "annotation::{a: 1}"
        val expr = pipeline.compile("`$ionValueAsString`")
        val session = EvaluationSession.standard()
        val result = expr.eval(session)
        val ionValueRoundtripped = result.toIonValue(ion)
        assertEquals(ion.singleValue("{a: 1}"), ionValueRoundtripped)
    }

    // Evaluated Ion value with annotation in an SFW projection. Converting result to an IonValue elides the annotation.
    @Test
    fun ionValueWithAnnotationInSFW() {
        val pipeline = CompilerPipeline.standard()
        val expr = pipeline.compile("SELECT t.a FROM [{'a': `annotation::1`}] AS t")
        val session = EvaluationSession.standard()
        val result = expr.eval(session)
        val ionValueRoundtripped = result.toIonValue(ion)
        assertEquals(ion.singleValue("\$bag::[{a: 1}]"), ionValueRoundtripped)
    }

    // Evaluated Ion values with annotations in an Arithmetic operation results to an Ion Value without annotation.
    @Test
    fun ionValueWithAnnotationInArithmeticOperation() {
        val pipeline = CompilerPipeline.standard()
        val expr = pipeline.compile("`value_1::1` + `value_2::2`")
        val session = EvaluationSession.standard()
        val result = expr.eval(session)
        val ionValueRoundtripped = result.toIonValue(ion)
        assertEquals(ion.singleValue("3"), ionValueRoundtripped)
    }
}
