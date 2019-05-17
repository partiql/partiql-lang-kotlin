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
 * Represents a function signature
 */
sealed class FunctionSignature {

    /**
     * function name
     */
    abstract val name: String

    /**
     * list of parameters for a function
     */
    abstract val formalParameters: List<FormalParameter>

    /**
     * function return type
     */
    abstract val returnType: StaticType
}

/**
 * Simulating untyped version of a function signature by using [StaticType.ANY] in its parameters and return type
 *
 * TODO a typed version of it
 */
data class UntypedFunctionSignature(override val name: String) : FunctionSignature() {
    companion object {
        // In the companion to reuse the same instances across different UntypedFunctionSignature instances
        private val formalParameters = listOf(VarargFormalParameter(StaticType.ANY))
        private val returnType = StaticType.ANY
    }

    override val formalParameters = Companion.formalParameters
    override val returnType = Companion.returnType

    override fun toString() = "$name(${formalParameters.joinToString { it.toString() }}): $returnType"
}
