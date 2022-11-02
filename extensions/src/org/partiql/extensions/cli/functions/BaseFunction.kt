/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package org.partiql.extensions.cli.functions

import com.amazon.ion.IonStruct
import com.amazon.ion.IonSystem
import com.amazon.ion.system.IonSystemBuilder
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.toIonValue

abstract class BaseFunction : ExprFunction {
    protected val ion: IonSystem = IonSystemBuilder.standard().build()

    protected fun optionsStruct(
        requiredArity: Int,
        args: List<ExprValue>,
        optionsIndex: Int = requiredArity
    ): IonStruct = when (args.size) {
        requiredArity -> ion.newEmptyStruct()
        requiredArity + 1 -> extractOptVal(args, optionsIndex)
        else -> throw IllegalArgumentException("Bad number of arguments: ${args.size}")
    }

    private fun extractOptVal(args: List<ExprValue>, optionsIndex: Int): IonStruct {
        return when (val optVal = args[optionsIndex].toIonValue(ion)) {
            is IonStruct -> optVal
            else -> throw IllegalArgumentException("Invalid option: $optVal")
        }
    }
}
