package org.partiql.lang.eval.io

import com.amazon.ion.system.IonSystemBuilder
import org.junit.Test
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.eval.CompileOptions
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ThunkOptions
import org.partiql.lang.types.FunctionSignature
import org.partiql.lang.types.StaticType
import kotlin.test.assertTrue

class AlwaysThrowsFunc : ExprFunction {

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        throw IllegalStateException()
    }
    override val signature: FunctionSignature
        get() = FunctionSignature("alwaysthrows", listOf(), returnType = StaticType.ANY)
}

class CustomExceptionHandlerTest {
    @Test
    fun verifyCustomExceptionHandler() {
        var customHandlerWasInvoked = false

        val ion = IonSystemBuilder.standard().build()
        val compilerPipeline = CompilerPipeline.build(ion) {
            addFunction(AlwaysThrowsFunc())
            compileOptions(
                CompileOptions.build {
                    thunkOptions(
                        ThunkOptions.build {
                            handleExceptionForLegacyMode { _, _ ->
                                customHandlerWasInvoked = true
                                throw IllegalStateException()
                            }
                        }
                    )
                }
            )
        }

        val expression = compilerPipeline.compile("alwaysthrows()")

        try {
            expression.eval(EvaluationSession.standard())
        } catch (e: IllegalStateException) {
            assertTrue(customHandlerWasInvoked, "Custom handler must be invoked")
        }
    }

    @Test
    fun verifyCustomExceptionHandlerForJavaBuilder() {
        var customHandlerWasInvoked = false

        val ion = IonSystemBuilder.standard().build()
        val compilerPipeline = CompilerPipeline.builder(ion)
            .addFunction(AlwaysThrowsFunc())
            .compileOptions(
                CompileOptions.builder()
                    .thunkOptions(
                        ThunkOptions.builder()
                            .handleExceptionForLegacyMode { _, _ ->
                                customHandlerWasInvoked = true
                                throw IllegalStateException()
                            }
                            .build()
                    )
                    .build()
            )
            .build()

        val expression = compilerPipeline.compile("alwaysthrows()")

        try {
            expression.eval(EvaluationSession.standard())
        } catch (e: IllegalStateException) {
            assertTrue(customHandlerWasInvoked, "Custom handler must be invoked")
        }
    }
}
