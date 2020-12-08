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

import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.eval.visitors.IDENTITY_VISITOR_TRANSFORM
import org.partiql.lang.eval.visitors.basicVisitorTransforms


/**
 * Defines the behavior when a non-existent variable is referenced.
 *
 * When `ERROR` any reference to a non-existent variable results in an [EvaluationException].
 *
 * When 'MISSING`, any reference to a non-existent variable results in an Ion `MISSING` value.]
 */
enum class UndefinedVariableBehavior {
    ERROR, MISSING
}

/**
 * Controls the behavior of [ExprValue.iterator] in the projection result.
 * For the query `Select a,b,c From <<{a:null, c:3}>>`;
 * * [FILTER_MISSING] will iterate over `[null,3]`
 * * [UNFILTERED] will iterate over `[null, missing, 3]`
 */
enum class ProjectionIterationBehavior {
    FILTER_MISSING, UNFILTERED
}

/**
 * Controls the behavior of intrinsic AST visitor transforms with [EvaluatingCompiler.compile].
 *
 * Most users will want [DEFAULT], which does the built-in visitor transforms for them, while
 * users wanting full control of the visitor transform process should use [NONE].
 */
enum class VisitorTransformMode {
    DEFAULT {
        override fun createVisitorTransform() = basicVisitorTransforms()
    },
    NONE {
        override fun createVisitorTransform() = IDENTITY_VISITOR_TRANSFORM
    };

    internal abstract fun createVisitorTransform(): PartiqlAst.VisitorTransform
}

/**
 * Specifies options that effect the behavior of the PartiQL compiler.
 */
data class CompileOptions private constructor (
        val undefinedVariable: UndefinedVariableBehavior,
        val projectionIteration: ProjectionIterationBehavior = ProjectionIterationBehavior.FILTER_MISSING,
        val visitorTransformMode: VisitorTransformMode = VisitorTransformMode.DEFAULT,
        val thunkOptions: ThunkOptions = ThunkOptions.standard()
) {

    companion object {

        /**
         * Creates a java style builder that will choose the default values for any unspecified options.
         */
        @JvmStatic
        fun builder() = Builder()

        /**
         * Kotlin style builder that will choose the default values for any unspecified options.
         */
        fun build(block: Builder.() -> Unit) = Builder().apply(block).build()

        /**
         * Creates a [CompileOptions] instance with the standard values.
         */
        @JvmStatic
        fun standard() = Builder().build()
    }

    /**
     * Builds a [CompileOptions] instance.
     */
    class Builder {
        private var options = CompileOptions(UndefinedVariableBehavior.ERROR)

        fun undefinedVariable(value: UndefinedVariableBehavior) = set { copy(undefinedVariable = value) }
        fun projectionIteration(value: ProjectionIterationBehavior) = set { copy(projectionIteration = value) }
        fun visitorTransformMode(value: VisitorTransformMode) = set { copy(visitorTransformMode = value) }
        fun thunkOptions(value: ThunkOptions) = set { copy(thunkOptions = value)}
        private inline fun set(block: CompileOptions.() -> CompileOptions) : Builder {
            options = block(options)
            return this
        }

        fun build() = options
    }
}
