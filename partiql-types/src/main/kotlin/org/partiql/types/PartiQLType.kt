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
public enum class PartiQLType {
    NULL,
    MISSING,
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
    BIT,
    BINARY,
    BYTE,
    BLOB,
    DATE,
    TIME,
    TIMESTAMP,
    INTERVAL,
    BAG,
    ARRAY,
    SEXP,
    STRUCT,
}
