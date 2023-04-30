package org.partiql.lang.eval.builtins

import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.eval.expectedArgTypeErrorMsg
import org.partiql.lang.types.StaticTypeUtils.getRuntimeType
import org.partiql.lang.types.StaticTypeUtils.getTypeDomain
import org.partiql.lang.types.StaticTypeUtils.isSubTypeOf
import org.partiql.lang.util.propertyValueMapOf
import org.partiql.types.BagType
import org.partiql.types.BlobType
import org.partiql.types.BoolType
import org.partiql.types.ClobType
import org.partiql.types.DateType
import org.partiql.types.DecimalType
import org.partiql.types.FloatType
import org.partiql.types.GraphType
import org.partiql.types.IntType
import org.partiql.types.ListType
import org.partiql.types.MissingType
import org.partiql.types.NullType
import org.partiql.types.SexpType
import org.partiql.types.SingleType
import org.partiql.types.StaticType
import org.partiql.types.StringType
import org.partiql.types.StructType
import org.partiql.types.SymbolType
import org.partiql.types.TimeType
import org.partiql.types.TimestampType
import java.lang.StringBuilder

/**
 * This class is used to represent each argument in ExprFunctions for argument type checking.
 * @param argPosition is the position of argument in an ExprFunction
 * @param expectedTypes is expected StaticType of the argument. As we exclude the cases of NULL and MISSING for type checking, this variable should not contain any of them.
 * @param delimiter is the substring between this argument and the next argument. If this argument is the last one, it represents the substring after this argument.
 */
data class Argument(
    val argPosition: Int,
    val expectedTypes: StaticType,
    val delimiter: String
)

private fun SingleType.getExample() = when (this) {
    is BoolType -> "TRUE"
    is IntType -> "0"
    is FloatType -> "`0e0`"
    is DecimalType -> "0."
    is DateType -> "DATE '2012-12-12'"
    is TimestampType -> "`2017T`"
    is TimeType -> "TIME '23:12:59.128'"
    is SymbolType -> "`a`"
    is StringType -> "'a'"
    is ClobType -> "`{{ \"HelloWorld\" }}`"
    is BlobType -> "`{{ aGVsbG8= }}`"
    is ListType -> "[]"
    is SexpType -> "sexp()"
    is StructType -> "{}"
    is BagType -> "<<>>"
    is GraphType -> "graph{}" // TODO: something better after we have concrete syntax for graphs
    is MissingType,
    is NullType -> throw Exception("NULL or MISSING should be the problem of permissive mode, not type checking.")
}

private fun StaticType.getOneExample() = (allTypes.first() as SingleType).getExample()

/**
 * This class is used to test invalid argument types for ExprFunctions, which has error code as EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_FUNC_CALL.
 * The idea here is to automatically generate queries to test ExprFunctions with different invalid argument types.
 * Also note that we exclude the cases of NULL and MISSING as argument types, as they should be the concern of permissive mode for ExprFunctions.
 */
// TODO: Get rid of `EvaluatorTestBase`
class InvalidArgTypeChecker : EvaluatorTestBase() {
    /**
     * Generates one test for each invalid type for each argument of the ExprFunction
     *
     * @param funcName is the name of the ExprFunction.
     * @param syntaxSuffix is the substring after function name and before the first argument tested.
     * @param args is the list of acceptable arguments for an ExprFunction.
     */
    fun checkInvalidArgType(funcName: String, syntaxSuffix: String = "(", args: List<Argument>) {
        val size = args.size
        val delimiters = args.map { it.delimiter }
        val expectedTypes = args.map { it.expectedTypes } // expectedExprValueTypes[i] is expected StaticType of args[i]
        val curArgTypeExamples = expectedTypes.map { it.getOneExample() }.toMutableList() // curArgTypeExamples[i] is one example of args[i]
        // In each argument position, for each SingleType, we first check if it is an invalid argument type.
        // If it is, we put the example of it in the current argument position and compose the query, then catch the error,
        expectedTypes.forEachIndexed { index, expectedType ->
            StaticType.ALL_TYPES.filter { it != StaticType.NULL && it != StaticType.MISSING }.forEach { singleType ->
                if (!isSubTypeOf(singleType, expectedType)) {
                    curArgTypeExamples[index] = singleType.getExample()
                    val query = composeQuery("$funcName$syntaxSuffix", delimiters, curArgTypeExamples, size)
                    assertThrowsInvalidArgType(
                        query,
                        funcName,
                        args[index].argPosition,
                        expectedType,
                        singleType
                    )
                }
                curArgTypeExamples[index] = expectedType.getOneExample()
            }
        }
    }

    private fun composeQuery(
        startStr: String,
        delimiters: List<String>,
        curArgTypeExamples: List<String>,
        size: Int
    ): String {
        val sb = StringBuilder(startStr)
        for (i in 0 until size) {
            sb.append(curArgTypeExamples[i])
            sb.append(delimiters[i])
        }
        return sb.toString()
    }

    private fun assertThrowsInvalidArgType(
        source: String,
        funcName: String,
        argPosition: Int,
        expectedTypes: StaticType,
        actualType: SingleType
    ) =
        runEvaluatorErrorTestCase(
            query = source,
            expectedErrorCode = ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_FUNC_CALL,
            expectedErrorContext = propertyValueMapOf(
                1, 1,
                Property.FUNCTION_NAME to funcName,
                Property.ARGUMENT_POSITION to argPosition,
                Property.EXPECTED_ARGUMENT_TYPES to expectedArgTypeErrorMsg(getTypeDomain(expectedTypes).toList()),
                Property.ACTUAL_ARGUMENT_TYPES to getRuntimeType(actualType).toString()
            ),
            expectedPermissiveModeResult = "MISSING"
        )
}
