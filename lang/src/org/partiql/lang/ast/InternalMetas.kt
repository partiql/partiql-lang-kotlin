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

package org.partiql.lang.ast

import com.amazon.ion.*

/**
 * Base class for [Meta] implementations which are used internally by [org.partiql.lang.eval.EvaluatingCompiler]
 * during compilation and should never be serialized.
 */
open class InternalMeta(override val tag: String): Meta {
    override fun serialize(writer: IonWriter) {
        throw UnsupportedOperationException(
            "${this.javaClass} is meant for internal use only and cannot be serialized.")
    }
}

/**
 * Contains the register index allocated to a [CallAgg].
 */
data class AggregateRegisterIdMeta(val registerId: Int) : InternalMeta(TAG) {
    companion object {
        const val TAG = "\$aggregate_register_id"
    }
}

/**
 * Represents the unique name given to certain variables--allows alpha rename transforms to avoid capturing
 * variables unintentionally.
 */
data class UniqueNameMeta(val uniqueName: String) : InternalMeta(TAG) {
    companion object {
        const val TAG = "\$unique_name"
    }
}

/**
 * Attached to [SymbolicName] instances that were synthetically calculated based on context
 * because they were not explicitly specified.
 */
class IsSyntheticNameMeta private constructor() : InternalMeta(TAG) {
    companion object {
        const val TAG = "\$is_synthetic_name"

        val instance = IsSyntheticNameMeta()
    }
}