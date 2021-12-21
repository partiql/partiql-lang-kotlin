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

import org.partiql.lang.eval.ArityCheckingTrait
import org.partiql.lang.eval.Environment
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.exprEquals

/**
 * NullIf built in function. Takes in two arguments, expr1 and expr2, returns null if expr1 = expr2 otherwise returns expr1
 *
 * ```
 * NULLIF(EXPRESSION, EXPRESSION)
 * ```
 */
internal class NullIfExprFunction(private val valueFactory: ExprValueFactory) : ArityCheckingTrait, ExprFunction {
    override val name: String = "nullif"
    override val arity: IntRange = (2..2)

    override fun call(env: Environment, args: List<ExprValue>): ExprValue {
        checkArity(args)

        return when {
            args[0].exprEquals(args[1]) -> valueFactory.nullValue
            else                        -> args[0]
        }
    }
}

