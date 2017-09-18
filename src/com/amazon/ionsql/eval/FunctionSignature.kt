/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

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
