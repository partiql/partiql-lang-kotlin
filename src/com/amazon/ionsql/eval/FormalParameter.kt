/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

/**
 * Represents a function parameter
 */
sealed class FormalParameter {

    /**
     * [StaticType] that this parameter accepts
     */
    abstract val type: StaticType
}

/**
 * Represents a single function parameter
 *
 * @param type [StaticType] that this parameter accepts
 */
data class SingleFormalParameter(override val type: StaticType) : FormalParameter() {
    override fun toString() = type.toString()
}

/**
 * Represents a variable number of arguments function parameter. Varargs are monomorpic, i.e. all elements are of
 * the **same** type
 *
 * @param type [StaticType] that this parameter accepts
 */
data class VarargFormalParameter(override val type: StaticType) : FormalParameter() {
    override fun toString(): String = "$type..."
}