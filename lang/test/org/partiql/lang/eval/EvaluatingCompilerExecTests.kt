package org.partiql.lang.eval

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.errors.PropertyValueMap
import org.partiql.lang.eval.builtins.storedprocedure.StoredProcedure
import org.partiql.lang.eval.builtins.storedprocedure.StoredProcedureSignature
import org.partiql.lang.util.ArgumentsProviderBase
import org.partiql.lang.util.softAssert
import org.partiql.lang.util.to

private fun createWrongSProcErrorContext(arg: ExprValue, expectedArgTypes: String, procName: String): PropertyValueMap {
    val errorContext = PropertyValueMap()
    errorContext[Property.EXPECTED_ARGUMENT_TYPES] = expectedArgTypes
    errorContext[Property.ACTUAL_ARGUMENT_TYPES] = arg.type.name
    errorContext[Property.FUNCTION_NAME] = procName
    return errorContext
}

/**
 * Simple stored procedure that takes no arguments and outputs 0.
 */
private class ZeroArgProcedure(val valueFactory: ExprValueFactory): StoredProcedure {
    override val signature = StoredProcedureSignature("zero_arg_procedure", 0)

    override fun call(session: EvaluationSession, args: List<ExprValue>): ExprValue {
        return valueFactory.newInt(0)
    }
}

/**
 * Simple stored procedure that takes no arguments and outputs -1. Used to show that added stored procedures of the
 * same name will be overridden.
 */
private class OverriddenZeroArgProcedure(val valueFactory: ExprValueFactory): StoredProcedure {
    override val signature = StoredProcedureSignature("zero_arg_procedure", 0)

    override fun call(session: EvaluationSession, args: List<ExprValue>): ExprValue {
        return valueFactory.newInt(-1)
    }
}

/**
 * Simple stored procedure that takes one integer argument and outputs that argument back.
 */
private class OneArgProcedure(val valueFactory: ExprValueFactory): StoredProcedure {
    override val signature = StoredProcedureSignature("one_arg_procedure", 1)

    override fun call(session: EvaluationSession, args: List<ExprValue>): ExprValue {
        val arg = args.first()
        if (arg.type != ExprValueType.INT) {
            val errorContext = createWrongSProcErrorContext(arg, "INT", signature.name)
            throw EvaluationException("invalid first argument",
                ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_PROCEDURE_CALL,
                errorContext,
                internal = false)
        }
        return arg
    }
}

/**
 * Simple stored procedure that takes two integer arguments and outputs the args as a string separated by
 * a space.
 */
private class TwoArgProcedure(val valueFactory: ExprValueFactory): StoredProcedure {
    override val signature = StoredProcedureSignature("two_arg_procedure", 2)

    override fun call(session: EvaluationSession, args: List<ExprValue>): ExprValue {
        val arg1 = args.first()
        if (arg1.type != ExprValueType.INT) {
            val errorContext = createWrongSProcErrorContext(arg1, "INT", signature.name)
            throw EvaluationException("invalid first argument",
                ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_PROCEDURE_CALL,
                errorContext,
                internal = false)
        }

        val arg2 = args[1]
        if (arg2.type != ExprValueType.INT) {
            val errorContext = createWrongSProcErrorContext(arg2, "INT", signature.name)
            throw EvaluationException("invalid second argument",
                ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_PROCEDURE_CALL,
                errorContext,
                internal = false)
        }
        return valueFactory.newString("$arg1 $arg2")
    }
}

/**
 * Simple stored procedure that takes one string argument and checks if the binding (case-insensitive) is in the
 * current session's global bindings. If so, returns the value associated with that binding. Otherwise, returns missing.
 */
private class OutputBindingProcedure(val valueFactory: ExprValueFactory): StoredProcedure {
    override val signature = StoredProcedureSignature("output_binding", 1)

    override fun call(session: EvaluationSession, args: List<ExprValue>): ExprValue {
        val arg = args.first()
        if (arg.type != ExprValueType.STRING) {
            val errorContext = createWrongSProcErrorContext(arg, "STRING", signature.name)
            throw EvaluationException("invalid first argument",
                ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_PROCEDURE_CALL,
                errorContext,
                internal = false)
        }
        val bindingName = BindingName(arg.stringValue(), BindingCase.INSENSITIVE)
        return when(val value = session.globals[bindingName]) {
            null -> valueFactory.missingValue
            else -> value
        }
    }
}

class EvaluatingCompilerExecTest : EvaluatorTestBase() {
    private val session = mapOf("A" to "[ { id : 1 } ]").toSession()

    /**
     * Custom [CompilerPipeline] w/ additional stored procedures
     */
    private val pipeline = CompilerPipeline.build(ion) {
        addProcedure(OverriddenZeroArgProcedure(valueFactory))
        addProcedure(ZeroArgProcedure(valueFactory))
        addProcedure(OneArgProcedure(valueFactory))
        addProcedure(TwoArgProcedure(valueFactory))
        addProcedure(OutputBindingProcedure(valueFactory))
    }

    /**
     * Runs the given [query] with the provided [session] using the custom [CompilerPipeline] with additional stored
     * procedures to query.
     */
    private fun evalSProc(query: String, session: EvaluationSession): ExprValue {
        val e = pipeline.compile(query)
        return e.eval(session)
    }

    /**
     * Similar to [EvaluatorTestBase]'s [runTestCase], but evaluates using a [CompilerPipeline] with added stored
     * procedures.
     */
    private fun runSProcTestCase(tc: EvaluatorTestCase, session: EvaluationSession) {
        val queryExprValue = evalSProc(tc.sqlUnderTest, session)
        val expectedExprValue = evalSProc(tc.expectedSql, session)

        if(!expectedExprValue.exprEquals(queryExprValue)) {
            println("Expected ionValue : ${expectedExprValue.ionValue}")
            println("Actual ionValue   : ${queryExprValue.ionValue}")

            fail("Expected and actual ExprValue instances are not equivalent")
        }
    }

    /**
     * Similar to [EvaluatorTestBase]'s [checkInputThrowingEvaluationException], but evaluates using a
     * [CompilerPipeline] with added stored procedures.
     */
    private fun checkInputThrowingEvaluationExceptionSProc(tc: EvaluatorErrorTestCase, session: EvaluationSession) {
        softAssert {
            try {
                val result = evalSProc(tc.sqlUnderTest, session = session).ionValue;
                fail("Expected EvaluationException but there was no Exception.  " +
                     "The unexpected result was: \n${result.toPrettyString()}")
            }
            catch (e: EvaluationException) {
                if (tc.cause != null) assertThat(e).hasRootCauseExactlyInstanceOf(tc.cause.java)
                checkErrorAndErrorContext(tc.errorCode, e, tc.expectErrorContextValues)
            }
            catch (e: Exception) {
                fail("Expected EvaluationException but a different exception was thrown:\n\t  $e")
            }
        }
    }

    private class ArgsProviderValid : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            // OverriddenZeroArgProcedure w/ same name as ZeroArgProcedure overridden
            EvaluatorTestCase(
                "EXEC zero_arg_procedure",
                "0"),
            EvaluatorTestCase(
                "EXEC one_arg_procedure 1",
                "1"),
            EvaluatorTestCase(
                "EXEC two_arg_procedure 1, 2",
                "'1 2'"),
            EvaluatorTestCase(
                "EXEC output_binding 'A'",
                "[{'id':1}]"),
            EvaluatorTestCase(
                "EXEC output_binding 'B'",
                "MISSING"))
    }

    @ParameterizedTest
    @ArgumentsSource(ArgsProviderValid::class)
    fun validTests(tc: EvaluatorTestCase) = runSProcTestCase(tc, session)


    private class ArgsProviderError : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            // call function that is not a stored procedure
            EvaluatorErrorTestCase(
                "EXEC utcnow",
                ErrorCode.EVALUATOR_NO_SUCH_PROCEDURE,
                mapOf(
                    Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 6L,
                    Property.PROCEDURE_NAME to "utcnow")),
            // call function that is not a stored procedure, w/ args
            EvaluatorErrorTestCase(
                "EXEC substring 0, 1, 'foo'",
                ErrorCode.EVALUATOR_NO_SUCH_PROCEDURE,
                mapOf(
                    Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 6L,
                    Property.PROCEDURE_NAME to "substring")),
            // invalid # args to sproc (too many)
            EvaluatorErrorTestCase(
                "EXEC zero_arg_procedure 1",
                ErrorCode.EVALUATOR_INCORRECT_NUMBER_OF_ARGUMENTS_TO_PROCEDURE_CALL,
                mapOf(
                    Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 6L,
                    Property.EXPECTED_ARITY_MIN to 0,
                    Property.EXPECTED_ARITY_MAX to 0)),
            // invalid # args to sproc (too many)
            EvaluatorErrorTestCase(
                "EXEC two_arg_procedure 1, 2, 3",
                ErrorCode.EVALUATOR_INCORRECT_NUMBER_OF_ARGUMENTS_TO_PROCEDURE_CALL,
                mapOf(
                    Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 6L,
                    Property.EXPECTED_ARITY_MIN to 2,
                    Property.EXPECTED_ARITY_MAX to 2)),
            // invalid # args to sproc (too few)
            EvaluatorErrorTestCase(
                "EXEC one_arg_procedure",
                ErrorCode.EVALUATOR_INCORRECT_NUMBER_OF_ARGUMENTS_TO_PROCEDURE_CALL,
                mapOf(
                    Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 6L,
                    Property.EXPECTED_ARITY_MIN to 1,
                    Property.EXPECTED_ARITY_MAX to 1)),
            // invalid first arg type
            EvaluatorErrorTestCase(
                "EXEC one_arg_procedure 'foo'",
                ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_PROCEDURE_CALL,
                mapOf(
                    Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 6L,
                    Property.EXPECTED_ARGUMENT_TYPES to "INT",
                    Property.ACTUAL_ARGUMENT_TYPES to "STRING",
                    Property.FUNCTION_NAME to "one_arg_procedure")),
            // invalid second arg type
            EvaluatorErrorTestCase(
                "EXEC two_arg_procedure 1, 'two'",
                ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_PROCEDURE_CALL,
                mapOf(
                    Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 6L,
                    Property.EXPECTED_ARGUMENT_TYPES to "INT",
                    Property.ACTUAL_ARGUMENT_TYPES to "STRING",
                    Property.FUNCTION_NAME to "two_arg_procedure"))
        )
    }

    @ParameterizedTest
    @ArgumentsSource(ArgsProviderError::class)
    fun errorTests(tc: EvaluatorErrorTestCase) = checkInputThrowingEvaluationExceptionSProc(tc, session)
}
