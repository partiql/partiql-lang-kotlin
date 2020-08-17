package org.partiql.lang.eval.io

import com.amazon.ion.system.IonSystemBuilder
import org.junit.Test
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.eval.*
import java.io.IOException
import java.lang.IllegalStateException
import java.lang.NullPointerException
import java.util.concurrent.ThreadPoolExecutor
import kotlin.test.assertTrue
import kotlin.test.fail

class AlwaysThrowsFunc(): ExprFunction {
    override val name: String
        get() = "alwaysthrows"

    override fun call(env: Environment, args: List<ExprValue>): ExprValue {
        throw IllegalStateException()
    }
}

class CustomExceptionHandlerTest {
    @Test
    fun verifyCustomExceptionHandler() {
        var customHandlerWasInvoked = false

        val ion = IonSystemBuilder.standard().build()
        val compilerPipeline = CompilerPipeline.build(ion) {
            addFunction(AlwaysThrowsFunc())
            compileOptions(CompileOptions.build {
                thunkOptions(ThunkOptions.build {
                    handleException { throwable, sourceLocationMeta ->
                        customHandlerWasInvoked = true
                        throw IllegalStateException()
                    }
                })
            })
        }

        val expression = compilerPipeline.compile("alwaysthrows()")

        try {
            expression.eval(EvaluationSession.standard())
            throw IllegalStateException()
            fail("IllegalStateException was not thrown.")
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
                .compileOptions(CompileOptions.builder()
                        .thunkOptions(ThunkOptions.builder()
                                .handleException { throwable, sourceLocationMeta ->
                                    customHandlerWasInvoked = true
                                    throw IllegalStateException()
                                }
                                .build())
                        .build())
                .build()

        val expression = compilerPipeline.compile("alwaysthrows()")

        try {
            expression.eval(EvaluationSession.standard())
            throw IllegalStateException()
            fail("IllegalStateException was not thrown.")
        } catch (e: IllegalStateException) {
            assertTrue(customHandlerWasInvoked, "Custom handler must be invoked")
        }
    }
}