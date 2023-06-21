/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.  All rights reserved.
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

package org.partiql.types

/**
 * PartiQL Type Names
 */
public enum class PartiQLValueType {
    BOOL,
    INT8,
    INT16,
    INT32,
    INT64,
    INT,
    DECIMAL,
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
    NULL, // null.null
    MISSING, // missing
    NULLABLE_BOOL, // null.bool
    NULLABLE_INT8, // null.int8
    NULLABLE_INT16, // null.int16
    NULLABLE_INT32, // null.int32
    NULLABLE_INT64, // null.int64
    NULLABLE_INT, // null.int
    NULLABLE_DECIMAL, // null.decimal
    NULLABLE_FLOAT32, // null.float32
    NULLABLE_FLOAT64, // null.float64
    NULLABLE_CHAR, // null.char
    NULLABLE_STRING, // null.string
    NULLABLE_SYMBOL, // null.symbol
    NULLABLE_BINARY, // null.binary
    NULLABLE_BYTE, // null.byte
    NULLABLE_BLOB, // null.blob
    NULLABLE_CLOB, // null.clob
    NULLABLE_DATE, // null.date
    NULLABLE_TIME, // null.time
    NULLABLE_TIMESTAMP, // null.timestamp
    NULLABLE_INTERVAL, // null.interval
    NULLABLE_BAG, // null.bag
    NULLABLE_LIST, // null.list
    NULLABLE_SEXP, // null.sexp
    NULLABLE_STRUCT, // null.struct
}
