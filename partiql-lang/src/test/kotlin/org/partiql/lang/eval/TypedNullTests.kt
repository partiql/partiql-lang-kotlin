package org.partiql.lang.eval

import com.amazon.ion.IonSystem
import com.amazon.ion.system.IonSystemBuilder
import org.junit.Test
import org.partiql.lang.CompilerPipeline

/**
 * The following tests demonstrate the current typed null behavior and tracks possible regressions in future commits.
 * This behavior is what the Kotlin implementation defines and is subject to change pending resolution of
 * https://github.com/partiql/partiql-spec/issues/61.
 */
class TypedNullTests : EvaluatorTestBase() {
    // Round-tripped typed nulls (IonValue -> ExprValue -> IonValue) results in same typed null value
    @Test
    fun typedNullIonValueExprValueRoundTrip() {
        val ion: IonSystem = IonSystemBuilder.standard().build()
        val ionValue = ion.singleValue("null.int")
        val exprValue = ExprValue.of(ionValue)
        val roundTripped = exprValue.toIonValue(ion)
        assertEquals(ionValue, roundTripped)
    }

    // Evaluated typed nulls converted to an IonValue preserve the typed null.
    @Test
    fun typedNullIonLiteralEvaluation() {
        val pipeline = CompilerPipeline.standard()
        val ionValueAsString = "null.int"
        val expr = pipeline.compile("`$ionValueAsString`")
        val session = EvaluationSession.standard()
        val result = expr.eval(session)
        val ionValueRoundtripped = result.toIonValue(ion)
        assertEquals(ion.singleValue(ionValueAsString), ionValueRoundtripped)
    }

    // Evaluated typed nulls in an SFW projection. Converting result to an IonValue preserves the typed null.
    @Test
    fun typedNullInSFW() {
        val pipeline = CompilerPipeline.standard()
        val expr = pipeline.compile("SELECT t.a FROM [{'a': `null.int`}] AS t")
        val session = EvaluationSession.standard()
        val result = expr.eval(session)
        val ionValueRoundtripped = result.toIonValue(ion)
        assertEquals(ion.singleValue("\$bag::[{a: null.int}]"), ionValueRoundtripped)
    }

    // Evaluated typed nulls in an arithmetic expression will NOT preserve the type when converting back to IonValue.
    @Test
    fun typedNullInArithmeticOperation() {
        val pipeline = CompilerPipeline.standard()
        val expr = pipeline.compile("`null.int` + `null.int`")
        val session = EvaluationSession.standard()
        val result = expr.eval(session)
        val ionValueRoundtripped = result.toIonValue(ion)
        assertEquals(ion.singleValue("null"), ionValueRoundtripped)
    }
}
