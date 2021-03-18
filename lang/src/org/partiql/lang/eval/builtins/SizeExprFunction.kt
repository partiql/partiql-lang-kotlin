/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.lang.eval.builtins

import com.amazon.ion.*
import org.partiql.lang.errors.*
import org.partiql.lang.eval.*
import org.partiql.lang.util.*

/**
 * Built in function to return the size of a container type, i.e. size of Lists, Structs and Bags. This function
 * propagates null and missing values as described in docs/Functions.md
 *
 * syntax: `size(<container>)` where container can be a BAG, STRUCT or LIST.
 */
internal class SizeExprFunction(valueFactory: ExprValueFactory) : NullPropagatingExprFunction("size", 1, valueFactory) {
    override fun eval(env: Environment, args: List<ExprValue>): ExprValue {
        val collection = args.first()

        return when (collection.type) {
            ExprValueType.LIST, ExprValueType.BAG, ExprValueType.STRUCT, ExprValueType.SEXP -> {
                val ionCol = collection.ionValue as IonContainer

                valueFactory.newInt(ionCol.size)
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