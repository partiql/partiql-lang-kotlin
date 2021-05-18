package org.partiql.lang.pts

import com.amazon.ion.IonSequence
import com.amazon.ion.IonStruct
import com.amazon.ion.IonValue
import com.amazon.ion.system.IonSystemBuilder
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.SqlException
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType.*
import org.partiql.lang.eval.name
import org.partiql.lang.eval.stringValue
import org.partiql.testscript.compiler.*
import org.partiql.testscript.evaluator.*
import org.partiql.testscript.evaluator.TestFailure.FailureReason.*

/**
 * Reference implementation PTS Evaluator.
 */
class PartiQlPtsEvaluator(equality: PtsEquality) : Evaluator(equality) {
    private val ion = IonSystemBuilder.standard().build()
    private val missing = ion.singleValue("missing::null")

    private val compilerPipeline = CompilerPipeline.standard(ion)

    override fun evaluate(testExpressions: List<TestScriptExpression>): List<TestResult> =
            testExpressions.map {
                when (it) {
                    is SkippedTestExpression -> TestResultSuccess(it)
                    is TestExpression -> runTest(it)
                    // the reference implementation doesn't need any appended information currently
                    // to run a test
                    is AppendedTestExpression -> runTest(it.original) 
                }
            }

    private fun runTest(test: TestExpression): TestResult = try {
        // recreate the environment struct using the evaluator ion system
        val ionStruct = ion.newValue(ion.newReader(test.environment).apply { next() }) as IonStruct
        
        
        val globals = compilerPipeline.valueFactory.newFromIonValue(ionStruct).bindings
        val session = EvaluationSession.build { globals(globals) }
        val expression = compilerPipeline.compile(test.statement)
        val actualResult = expression.eval(session).toPtsIon()

        verifyTestResult(test, actualResult)

    } catch (e: SqlException) {
        when (test.expected) {
            is ExpectedError -> TestResultSuccess(test)
            is ExpectedSuccess -> TestFailure(test, e.generateMessage(), UNEXPECTED_ERROR)
        }
    }

    private fun verifyTestResult(test: TestExpression, actualResult: IonValue): TestResult =
            when (val expected = test.expected) {
                is ExpectedError -> TestFailure(test, actualResult.toIonText(), EXPECTED_ERROR_NOT_THROWN)
                is ExpectedSuccess -> {
                    if (equality.areEqual(expected.expected, actualResult)) {
                        TestResultSuccess(test)
                    } else {
                        TestFailure(test, actualResult.toIonText(), ACTUAL_DIFFERENT_THAN_EXPECTED)
                    }
                }
            }
    
    private fun ExprValue.toPtsIon(): IonValue {

        fun <S : IonSequence> ExprValue.foldToIonSequence(initial: S): S =
                this.fold(initial) { seq, el -> seq.apply { add(el.toPtsIon()) } }

        return when (this.type) {
            MISSING -> missing
            NULL, BOOL, INT, FLOAT, DECIMAL, DATE, TIME, TIMESTAMP, SYMBOL, STRING, CLOB, BLOB -> this.ionValue.clone()
            LIST -> this.foldToIonSequence(ion.newEmptyList())
            SEXP -> this.foldToIonSequence(ion.newEmptySexp())
            STRUCT -> this.fold(ion.newEmptyStruct()) { struct, el ->
                struct.apply { add(el.name!!.stringValue(), el.toPtsIon()) }
            }
            BAG -> {
                val bag = ion.newEmptySexp().apply { add(ion.newSymbol("bag")) }

                this.foldToIonSequence(bag)
            }
        }
    }
}

private fun IonValue.toIonText(): String {
    val sb = StringBuilder()
    this.system.newTextWriter(sb).use { w -> this.writeTo(w) }

    return sb.toString()
}

