package org.partiql.lang.pts

import com.amazon.ion.IonSequence
import com.amazon.ion.IonStruct
import com.amazon.ion.IonValue
import com.amazon.ion.system.IonSystemBuilder
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.SqlException
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.name
import org.partiql.lang.eval.stringValue
import org.partiql.testscript.compiler.AppendedTestExpression
import org.partiql.testscript.compiler.ExpectedError
import org.partiql.testscript.compiler.ExpectedSuccess
import org.partiql.testscript.compiler.SkippedTestExpression
import org.partiql.testscript.compiler.TestExpression
import org.partiql.testscript.compiler.TestScriptExpression
import org.partiql.testscript.evaluator.Evaluator
import org.partiql.testscript.evaluator.PtsEquality
import org.partiql.testscript.evaluator.TestFailure
import org.partiql.testscript.evaluator.TestResult
import org.partiql.testscript.evaluator.TestResultSuccess

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
            is ExpectedSuccess -> TestFailure(test, e.generateMessage(), TestFailure.FailureReason.UNEXPECTED_ERROR)
        }
    }

    private fun verifyTestResult(test: TestExpression, actualResult: IonValue): TestResult =
        when (val expected = test.expected) {
            is ExpectedError -> TestFailure(test, actualResult.toIonText(), TestFailure.FailureReason.EXPECTED_ERROR_NOT_THROWN)
            is ExpectedSuccess -> {
                if (equality.areEqual(expected.expected, actualResult)) {
                    TestResultSuccess(test)
                } else {
                    TestFailure(test, actualResult.toIonText(), TestFailure.FailureReason.ACTUAL_DIFFERENT_THAN_EXPECTED)
                }
            }
        }

    private fun ExprValue.toPtsIon(): IonValue {

        fun <S : IonSequence> ExprValue.foldToIonSequence(initial: S): S =
            this.fold(initial) { seq, el -> seq.apply { add(el.toPtsIon()) } }

        return when (this.type) {
            ExprValueType.MISSING -> missing
            ExprValueType.NULL,
            ExprValueType.BOOL,
            ExprValueType.INT,
            ExprValueType.FLOAT,
            ExprValueType.DECIMAL,
            ExprValueType.DATE,
            ExprValueType.TIME,
            ExprValueType.TIMESTAMP,
            ExprValueType.SYMBOL,
            ExprValueType.STRING,
            ExprValueType.CLOB,
            ExprValueType.BLOB -> this.ionValue.clone()
            ExprValueType.LIST -> this.foldToIonSequence(ion.newEmptyList())
            ExprValueType.SEXP -> this.foldToIonSequence(ion.newEmptySexp())
            ExprValueType.STRUCT -> this.fold(ion.newEmptyStruct()) { struct, el ->
                struct.apply { add(el.name!!.stringValue(), el.toPtsIon()) }
            }
            ExprValueType.BAG -> {
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
