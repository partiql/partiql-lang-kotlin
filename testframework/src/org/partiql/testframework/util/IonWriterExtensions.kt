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

package org.partiql.testframework.util

import com.amazon.ion.*
import java.math.*

// Some extensions to give IonWriter a Kotlin style builder. Assumes values are being written inside an IonStruct

/**
 * Writes a new struct with the given [fieldName].
 *
 * @param fieldName new struct field name
 * @param block struct builder
 *
 * @throws IllegalStateException if the current container isn't a struct,
 * that is, if {@link #isInStruct()} is false.
 */
internal fun IonWriter.struct(fieldName: String, block: IonWriter.() -> Unit) {
    setFieldName(fieldName)
    stepIn(IonType.STRUCT)
    apply(block)
    stepOut()
}


/**
 * Writes a new value from a reader setting its given [fieldName]
 *
 * @param fieldName new struct field name
 * @param reader used to read the value to be written
 *
 * @throws IllegalStateException if the current container isn't a struct,
 * that is, if {@link #isInStruct()} is false.
 */
internal fun IonWriter.writeNextFieldFromReader(fieldName: String, reader: IonReader) {
    reader.next()
    setFieldName(fieldName)
    writeValue(reader)
}

/**
 * Writes a new Int value setting its given [fieldName]
 *
 * @param fieldName new struct field name
 * @param value value to be written
 *
 * @throws IllegalStateException if the current container isn't a struct,
 * that is, if {@link #isInStruct()} is false.
 */
internal fun IonWriter.integer(fieldName: String, value: Long) {
    setFieldName(fieldName)
    writeInt(value)
}

/**
 * Writes a new Decimal value setting its given [fieldName]
 *
 * @param fieldName new struct field name
 * @param value value to be written
 *
 * @throws IllegalStateException if the current container isn't a struct,
 * that is, if {@link #isInStruct()} is false.
 */
internal fun IonWriter.decimal(fieldName: String, value: BigDecimal) {
    setFieldName(fieldName)
    writeDecimal(value)
}

/**
 * Writes a new String value setting its given [fieldName]
 *
 * @param fieldName new struct field name
 * @param value value to be written
 *
 * @throws IllegalStateException if the current container isn't a struct,
 * that is, if {@link #isInStruct()} is false.
 */
internal fun IonWriter.string(fieldName: String, value: String) {
    setFieldName(fieldName)
    writeString(value)
}
