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

/** Pre-defined sets of compilation options for use with [EvaluatorTestCase]. */
enum class CompOptions(
    val optionsBlock: CompileOptions.Builder.() -> Unit
) {
    STANDARD({ /* intentionally empty. */ }),

    UNDEF_VAR_MISSING(
        {
            undefinedVariable(UndefinedVariableBehavior.MISSING)
        }
    ),

    PROJECT_UNFILTERED_UNDEF_VAR_MISSING(
        {
            projectionIteration(ProjectionIterationBehavior.UNFILTERED)
            undefinedVariable(UndefinedVariableBehavior.MISSING)
        }
    ),

    PROJECT_UNFILTERED(
        {
            projectionIteration(ProjectionIterationBehavior.UNFILTERED)
        }
    ),

    TYPED_OP_BEHAVIOR_HONOR_PARAMS(
        {
            typedOpBehavior(TypedOpBehavior.HONOR_PARAMETERS)
        }
    ),

    PERMISSIVE(
        {
            this.typingMode(TypingMode.PERMISSIVE)
        }
    );

    val options get() = CompileOptions.build { optionsBlock() }

    companion object {
        /** Only those options from [CompOptions] which have [UndefinedVariableBehavior.MISSING]. */
        val onlyUndefinedVariableBehaviorMissing = listOf(UNDEF_VAR_MISSING, PROJECT_UNFILTERED_UNDEF_VAR_MISSING)

        /** Only those options from [CompOptions] which have [ProjectionIterationBehavior.UNFILTERED] set. */
        val onlyProjectIterationBehaviorFilterMissing = listOf(STANDARD, UNDEF_VAR_MISSING)
    }
}
