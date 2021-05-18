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

import com.amazon.ion.Timestamp
import org.partiql.lang.eval.time.Time
import java.time.LocalDate

/**
 * Represents a scalar view over an [ExprValue].
 */
interface Scalar {
    companion object {
        @JvmField
        val EMPTY = object : Scalar { }
    }

    /**
     * Returns this value as a [Boolean] or `null` if not applicable.
     * This operation is only applicable for [ExprValueType.BOOL]
     */
    fun booleanValue(): Boolean? = null

    /**
     * Returns this value as a [Long], [Double], [BigDecimal], or `null` if not applicable.
     * This operation is only applicable for [ExprValueType.isNumber]
     */
    fun numberValue(): Number? = null

    /**
     * Returns this value as a [Timestamp] or `null` if not applicable.
     * This operation is only applicable for [ExprValueType.TIMESTAMP]
     */
    fun timestampValue(): Timestamp? = null

    /**
     * Returns this value as a [LocalDate] or `null` if not applicable.
     * This operation is only applicable for [ExprValueType.DATE]
     */
    fun dateValue(): LocalDate? = null

    /**
     * Returns this value as a [Time] or `null` if not applicable.
     * This operation is only applicable for [ExprValueType.TIME].
     */
    fun timeValue(): Time? = null

    /**
     * Returns this value as a [String] or `null` if not applicable.
     * This operation is only applicable for [ExprValueType.isText]
     */
    fun stringValue(): String? = null

    /**
     * Returns this value as a [ByteArray], or `null` if not applicable.
     * This operation is only applicable for [ExprValueType.isLob]
     */
    fun bytesValue(): ByteArray? = null
}
