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

package org.partiql.cli.functions

import com.amazon.ion.*
import org.partiql.lang.eval.*

internal abstract class BaseFunction(val valueFactory: ExprValueFactory) : ExprFunction {
    protected fun optionsStruct(requiredArity: Int,
                                args: List<ExprValue>,
                                optionsIndex: Int = requiredArity): IonStruct = when (args.size) {
        requiredArity     -> valueFactory.ion.newEmptyStruct()
        requiredArity + 1 -> extractOptVal(args, optionsIndex)
        else              -> throw IllegalArgumentException("Bad number of arguments: ${args.size}")
    }

    private fun extractOptVal(args: List<ExprValue>, optionsIndex: Int): IonStruct {
        val optVal = args[optionsIndex].ionValue
        return when (optVal) {
            is IonStruct -> optVal
            else         -> throw IllegalArgumentException("Invalid option: $optVal")
        }
    }
}

