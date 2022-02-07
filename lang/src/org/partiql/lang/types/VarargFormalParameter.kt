/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package org.partiql.lang.types

/**
 * Represents a variable number of arguments function parameter. Varargs are monomorpic, i.e. all elements are of
 * the **same** type
 *
 * @param type [StaticType] that this parameter accepts
 */
data class VarargFormalParameter(
    val type: StaticType,
    val arityRange: IntRange
) {
    constructor(type: StaticType, minCount: Int) : this(type, minCount..Int.MAX_VALUE)
    override fun toString(): String = "$type..."
}
