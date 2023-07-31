package org.partiql.lang.eval

import org.assertj.core.api.Assertions
import org.junit.Test
import org.partiql.errors.ErrorCode
import org.partiql.errors.PropertyValueMap
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.ast.passes.SemanticException
import org.partiql.lang.types.FunctionSignature
import org.partiql.types.StaticType

/**
 * A class to test if the wrapping of internal exceptions are correct.
 */
class ExceptionWrappingTest {
    private val throwIllegalStateExceptionExprFunction = object : ExprFunction {
        override val signature: FunctionSignature
            get() = FunctionSignature("throw_illegal_state_exception", listOf(), returnType = StaticType.ANY)

        override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
            throw IllegalStateException("Intentionally throw an IllegalStateException")
        }
    }

    private val throwSemanticExceptionExprFunction = object : ExprFunction {
        override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
            throw SemanticException("Intentionally throw a SemanticException", ErrorCode.SEMANTIC_AMBIGUOUS_BINDING, PropertyValueMap())
        }

        override val signature: FunctionSignature
            get() = FunctionSignature("wrap_exception", listOf(), returnType = StaticType.ANY)
    }

    /**
     * To make custom functions available to the PartiQL being executed, they must be passed to
     * [CompilerPipeline.Builder.addFunction].
     */
    private val pipeline = CompilerPipeline.build {
        addFunction(throwIllegalStateExceptionExprFunction)
        addFunction(throwSemanticExceptionExprFunction)

        compileOptions {
            thunkOptions(
                ThunkOptions.build {
                    handleExceptionForLegacyMode { ex, srcLocation ->
                        if (ex is SemanticException) {
                            DEFAULT_EXCEPTION_HANDLER_FOR_LEGACY_MODE(ex, srcLocation)
                        } else {
                            throw ex
                        }
                    }
                }
            )
        }
    }

    /** Evaluates the given [query] with as standard [EvaluationSession]. */
    private fun eval(query: String): ExprValue {
        val e = pipeline.compile(query)
        return e.eval(EvaluationSession.standard())
    }

    @Test
    fun shouldNotWrapIllegalStateException() {
        Assertions.assertThatThrownBy { eval("throw_illegal_state_exception()") }
            .hasMessage("Intentionally throw an IllegalStateException")
            .isExactlyInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun shouldWrapSemanticException() {
        Assertions.assertThatThrownBy { eval("wrap_exception()") }
            .isExactlyInstanceOf(EvaluationException::class.java)
    }
}
