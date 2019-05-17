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

package org.partiql.lang.util

import com.amazon.ion.*

/**
 * A simple wrapper for writing Ion from Kotlin.
 * This is not implemented as a set of extension methods because our coding standards disallow non-private extension
 * methods.
 */
class IonWriterContext(val writer: IonWriter) {

    fun setNextFieldName(fieldName: String) {
        if(!writer.isInStruct) {
            throw IllegalStateException("Cannot set field name while not in a struct")
        }

        writer.setFieldName(fieldName)
    }



    fun sexp(block: IonWriterContext.() -> Unit) {
        writer.stepIn(IonType.SEXP)
        block(this)
        writer.stepOut()
    }

    fun sexp(fieldName: String, block: IonWriterContext.() -> Unit) {
        setNextFieldName(fieldName)
        sexp(block)
    }

    fun list(block: IonWriterContext.() -> Unit) {
        writer.stepIn(IonType.LIST)
        block()
        writer.stepOut()
    }
    fun list(fieldName: String, block: IonWriterContext.() -> Unit) {
        setNextFieldName(fieldName)
        list(block)
    }

    fun struct(block: IonWriterContext.() -> Unit) {
        writer.stepIn(IonType.STRUCT)
        block()
        writer.stepOut()
    }

    fun struct(fieldName: String, block: IonWriterContext.() -> Unit) {
        setNextFieldName(fieldName)
        struct(block)
    }

    fun string(str: String) {
        writer.writeString(str)
    }

    fun string(fieldName: String, str: String) {
        setNextFieldName(fieldName)
        string(str)
    }

    fun symbol(str: String) {
        writer.writeSymbol(str)
    }

    fun symbol(fieldName: String, str: String) {
        setNextFieldName(fieldName)
        symbol(str)
    }

    fun int(value: Long) {
        writer.writeInt(value)
    }

    fun int(fieldName: String, value: Long) {
        setNextFieldName(fieldName)
        int(value)
    }

    fun bool(value: Boolean) {
        writer.writeBool(value)
    }

    fun bool(fieldName: String, value: Boolean) {
        setNextFieldName(fieldName)
        bool(value)
    }

    fun untypedNull() {
        writer.writeNull()
    }

    fun untypedNull(fieldName: String) {
        setNextFieldName(fieldName)
        writer.writeNull()
    }

    fun nullBool() = writer.writeNull(IonType.BOOL)
    fun nullBool(fieldName: String) {
        setNextFieldName(fieldName)
        nullBool()
    }

    fun nullInt() = writer.writeNull(IonType.INT)
    fun nullInt(fieldName: String) {
        setNextFieldName(fieldName)
        nullInt()
    }

    fun nullFloat() = writer.writeNull(IonType.FLOAT)
    fun nullFloat(fieldName: String) {
        setNextFieldName(fieldName)
        nullFloat()
    }

    fun nullDecimal() = writer.writeNull(IonType.DECIMAL)
    fun nullDecimal(fieldName: String) {
        setNextFieldName(fieldName)
        nullDecimal()
    }

    fun nullSymbol() = writer.writeNull(IonType.SYMBOL)
    fun nullSymbol(fieldName: String) {
        setNextFieldName(fieldName)
        nullSymbol()
    }

    fun nullString() = writer.writeNull(IonType.STRING)
    fun nullString(fieldName: String) {
        setNextFieldName(fieldName)
        nullString()
    }

    fun nullClob() = writer.writeNull(IonType.CLOB)
    fun nullClob(fieldName: String) {
        setNextFieldName(fieldName)
        nullClob()
    }

    fun nullBlob() = writer.writeNull(IonType.BLOB)
    fun nullBlob(fieldName: String) {
        setNextFieldName(fieldName)
        nullBlob()
    }

    fun nullList() = writer.writeNull(IonType.LIST)
    fun nullList(fieldName: String) {
        setNextFieldName(fieldName)
        nullList()
    }

    fun nullSexp() = writer.writeNull(IonType.SEXP)
    fun nullSexp(fieldName: String) {
        setNextFieldName(fieldName)
        nullSexp()
    }

    fun nullStruct() = writer.writeNull(IonType.STRUCT)
    fun nullStruct(fieldName: String) {
        setNextFieldName(fieldName)
        nullStruct()
    }

    fun value(value: IonValue) {
        value.system.newReader(value).use {
            it.next()
            writer.writeValue(it)
        }
    }

    fun value(fieldName: String, value:IonValue) {
        setNextFieldName(fieldName)
        value(value)
    }
}