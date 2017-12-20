package com.amazon.ionsql.eval.builtins

import com.amazon.ion.*
import com.amazon.ionsql.errors.*
import com.amazon.ionsql.eval.*
import com.amazon.ionsql.util.*

/**
 * Built in function to return the size of a container type, i.e. size of Lists, Structs and Bags. This function
 * propagates null and missing values as described in docs/Functions.md
 *
 * syntax: `size(<container>)` where container can be a BAG, STRUCT or LIST.
 */
internal class SizeExprFunction(ion: IonSystem) : NullPropagatingExprFunction("size", 1, ion) {
    override fun eval(env: Environment, args: List<ExprValue>): ExprValue {
        val collection = args.first()

        return when (collection.type) {
            ExprValueType.LIST, ExprValueType.BAG, ExprValueType.STRUCT -> {
                val ionCol = collection.ionValue as IonContainer

                ionCol.size.exprValue(ion)
            }
            else                                                        -> {
                val errorContext = PropertyValueMap()
                errorContext[Property.EXPECTED_ARGUMENT_TYPES] = "LIST or BAG or STRUCT"
                errorContext[Property.ACTUAL_ARGUMENT_TYPES] = collection.type.name
                errorContext[Property.FUNCTION_NAME] = "size"

                err(message = "invalid argument type for size",
                    errorCode = ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_FUNC_CALL,
                    errorContext = errorContext,
                    internal = false)
            }
        }
    }
}