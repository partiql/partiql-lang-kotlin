package org.partiql.runner.test.executor

import com.amazon.ion.IonStruct
import com.amazon.ion.IonValue
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.eval.CompileOptions
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.toIonValue
import org.partiql.runner.ION
import org.partiql.runner.test.TestExecutor
import org.partiql.runner.util.ValueEquals

/**
 * [TestExecutor] which uses the original EvaluatingCompiler APIs.
 *
 * @property pipeline
 * @property session
 */
class LegacyExecutor(
    private val pipeline: CompilerPipeline,
    private val session: EvaluationSession,
) : TestExecutor<ExprValue, IonValue> {

    private val eq = ValueEquals.legacy

    override fun prepare(statement: String): ExprValue = pipeline.compile(statement).eval(session)

    override fun execute(statement: ExprValue): IonValue = statement.toIonValue(ION)

    override fun fromIon(value: IonValue): IonValue = value

    override fun toIon(value: IonValue): IonValue = value

    override fun compare(actual: IonValue, expect: IonValue): Boolean = eq.equals(actual, expect)

    object Factory : TestExecutor.Factory<ExprValue, IonValue> {

        override fun create(env: IonStruct, options: CompileOptions): TestExecutor<ExprValue, IonValue> {
            val pipeline = CompilerPipeline.builder().compileOptions(options).build()
            val globals = ExprValue.of(env).bindings
            val session = EvaluationSession.build { globals(globals) }
            return LegacyExecutor(pipeline, session)
        }
    }
}
