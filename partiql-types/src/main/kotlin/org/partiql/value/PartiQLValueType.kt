/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package org.partiql.value

/**
 * PartiQL Type Names
 */
public enum class PartiQLValueType {
    ANY,
    BOOL,
    INT8,
    INT16,
    INT32,
    INT64,
    INT,
    // For now, just distinguish between fixed precision and arbitrary precision
    DECIMAL, // TODO: Simple enum class does not have the power to express the parameterized type
    DECIMAL_ARBITRARY, // arbitrary precision decimal
    FLOAT32,
    FLOAT64,
    CHAR,
    STRING,
    SYMBOL,
    BINARY,
    BYTE,
    BLOB,
    CLOB,
    DATE,
    TIME,
    TIMESTAMP,
    INTERVAL,
    BAG,
    LIST,
    SEXP,
    STRUCT,

    @Deprecated(
        message = "This will be removed in a future major-version bump.",
        replaceWith = ReplaceWith("ANY")
    )
    NULL,

    @Deprecated(
        message = "This will be removed in a future major-version bump.",
        replaceWith = ReplaceWith("ANY")
    )
    MISSING,
}
