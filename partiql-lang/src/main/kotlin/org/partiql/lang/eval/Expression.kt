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

package org.partiql.lang.eval

/**
 * An expression that can be evaluated to [ExprValue].
 */
interface Expression {

    /**
     * Static Coverage Statistics
     */
    val coverageStructure: CoverageStructure?

    /**
     * Evaluates the expression with the given Session
     */
    @Deprecated("To be removed in the next release.", replaceWith = ReplaceWith("evaluate"))
    fun eval(session: EvaluationSession): ExprValue

    /**
     * Evaluates the expression with the given Session
     */
    fun evaluate(session: EvaluationSession): PartiQLResult
}
