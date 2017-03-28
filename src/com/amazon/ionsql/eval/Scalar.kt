/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

import com.amazon.ion.Timestamp

/**
 * Represents a scalar view over an [ExprValue].
 */
abstract class Scalar {
    companion object {
        private val EMPTY = object : Scalar() {}

        /** Returns the *empty* scalar, one that returns `null` for all methods. */
        fun empty(): Scalar = EMPTY
    }

    /**
     * Returns this value as a [Boolean] or `null` if not applicable.
     * This operation is only applicable for [ExprValueType.BOOL]
     */
    open fun booleanValue(): Boolean? = null

    /**
     * Returns this value as a [Long], [Double], [BigDecimal], or `null` if not applicable.
     * This operation is only applicable for [ExprValueType.isNumber]
     */
    open fun numberValue(): Number? = null

    /**
     * Returns this value as a [Timestamp] or `null` if not applicable.
     * This operation is only applicable for [ExprValueType.TIMESTAMP]
     */
    open fun timestampValue(): Timestamp? = null

    /**
     * Returns this value as a [String] or `null` if not applicable.
     * This operation is only applicable for [ExprValueType.isText]
     */
    open fun stringValue(): String? = null

    /**
     * Returns this value as a [ByteArray], or `null` if not applicable.
     * This operation is only applicable for [ExprValueType.isLob]
     */
    open fun bytesValue(): ByteArray? = null
}
