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

import com.amazon.ion.*
import org.partiql.lang.errors.*
import org.partiql.lang.util.*

/*
 * WARNING: This whole file is intended as a non intrusive way to preserve the meta nodes information during
 * evaluation so we can include line number and column number in EvaluationExceptions. This is not a replacement for
 * properly populating the error context
 */

/**
 * Holds the expression line number and column number.
 */
data class NodeMetadata(val line: Long, val column: Long) {
    constructor(struct: IonStruct) : this(struct["line"].longValue(), struct["column"].longValue())

    /**
     * Fill existing errorContext with information present in metadata if that information is not present in the error
     * context already
     *
     * @param errorContext to be filled
     * @return passed in errorContext
     */
    fun fillErrorContext(errorContext: PropertyValueMap): PropertyValueMap {
        if (errorContext[Property.LINE_NUMBER] == null && errorContext[Property.COLUMN_NUMBER] == null) {
            errorContext[Property.LINE_NUMBER] = this.line
            errorContext[Property.COLUMN_NUMBER] = this.column
        }

        return errorContext
    }

    /**
     * creates and fills a new error context with this metadata information
     */
    fun toErrorContext(): PropertyValueMap? {
        return fillErrorContext(PropertyValueMap())
    }

    /**
     * Adds line and column number to the given [PropertyValueMap]
     */
    fun toErrorContext(properties: PropertyValueMap): PropertyValueMap {
        return fillErrorContext(properties)
    }
}
