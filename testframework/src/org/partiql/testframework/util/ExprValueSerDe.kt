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
import org.partiql.lang.eval.*
import org.partiql.lang.util.*

/**
 * Serializes [ExprValue] to an Ion s-expression.
 *
 * Note that this is not present in the interpreter's public API because the format used has only been
 * considered for use as part of test scripts and not for the general case of serializing any [ExprValue].
 *
 * Also, there is no facility for serializing customer provided implementations of [ExprValue].
 */
fun serializeExprValue(value: ExprValue): IonValue {
    val tempContainer = value.ionValue.system.newList()
    value.ionValue.system.newWriter(tempContainer).use {
        IonWriterContext(it).serialize(value)
    }
    return tempContainer.first()
}

private fun IonWriterContext.serialize(value: ExprValue): Unit =
    when (value.type) {
        ExprValueType.MISSING -> sexp { symbol("missing") }

        ExprValueType.NULL,
        ExprValueType.BOOL,
        ExprValueType.INT,
        ExprValueType.FLOAT,
        ExprValueType.DECIMAL,
        ExprValueType.TIMESTAMP,
        ExprValueType.STRING,
        ExprValueType.CLOB,
        ExprValueType.BLOB,
        ExprValueType.SYMBOL  -> value(value.ionValue)

        ExprValueType.BAG     ->
            sexp {
                symbol("bag")
                value.forEach {
                    serialize(it)
                }
            }
        ExprValueType.LIST    ->
            when {
                value.ionValue.isNullValue -> this.nullList()
                else                       ->
                    list {
                        value.forEach {
                            serialize(it)
                        }
                    }
            }
        ExprValueType.SEXP    ->
            when {
                value.ionValue.isNullValue  -> this.nullSexp()
                else                        ->
                    sexp {
                        symbol("sexp")
                        value.forEach {
                            serialize(it)
                        }
                    }
            }
        ExprValueType.STRUCT  ->
            when {
                value.ionValue.isNullValue -> this.nullStruct()
                else                       ->
                    struct {
                        value.forEach {
                            val name = it.name ?: throw IllegalStateException("Member of StructExprValue had no name")
                            setNextFieldName(name.stringValue())
                            serialize(it)
                        }
                    }
            }
    }


/**
 * Can be used to deserialize an [ExprValue] that was serialized with [serializeExprValue].
 *
 * Note that this is not present in the public API because the format used has only been
 * considered for use as part of test scripts and not for the general case of serializing any [ExprValue].
 *
 * Also, there is no facility for serializing customer provided implementations of [ExprValue].
 */
fun deserializeExprValue(ionValue: IonValue, valueFactory: ExprValueFactory): ExprValue =
    when(ionValue.type) {
        IonType.LIST      ->
            when {
                ionValue.isNullValue -> valueFactory.nullValue
                else                 ->
                    valueFactory.newList(
                        (ionValue as IonContainer).map { deserializeExprValue(it, valueFactory) }.asSequence())
            }
        IonType.SEXP      ->
            when {
                ionValue.isNullValue -> valueFactory.nullValue
                else                 -> {
                    val sexp = ionValue as IonSexp
                    if (sexp.isEmpty) {
                        throw ExprValueDeserializationException("Encountered an empty s-expression")
                    }

                    val tag = sexp[0] as? IonSymbol
                              ?: throw ExprValueDeserializationException("First element of s-expression was not a symbol")

                    val tagHandler = tagHandlers[tag.stringValue()]
                                     ?: throw ExprValueDeserializationException("Unknown s-exp tag: $tag")

                    tagHandler(sexp.drop(1), valueFactory)
                }
            }


        IonType.STRUCT    ->
            when {
                ionValue.isNullValue -> valueFactory.nullValue
                else                 ->
                    valueFactory.newStruct(
                        (ionValue as IonContainer).map {
                            deserializeExprValue(it, valueFactory).namedValue(valueFactory.newSymbol(it.fieldName!!))
                        }.asSequence(),
                        StructOrdering.UNORDERED)
            }
        IonType.DATAGRAM  -> throw ExprValueDeserializationException("Cannot deserialize an Ion Datagram")
        null              -> throw ExprValueDeserializationException("value.type was null for some reason")
        else ->
            valueFactory.newFromIonValue(ionValue)
    }


/**
 * Thrown when en error condition is encountered during deserializaiton.
 */
class ExprValueDeserializationException(message: String) : RuntimeException(message)

private val tagHandlers: Map<String, (List<IonValue>, ExprValueFactory) -> ExprValue> = mapOf(
    "missing" to { args, valueFactory ->
        if(args.isNotEmpty()) {
            throw ExprValueDeserializationException("missing should have zero arguments")
        }
        valueFactory.missingValue
    },
    "sexp" to { args, valueFactory ->
        valueFactory.newSexp(
            args.map { deserializeExprValue(it, valueFactory) }.asSequence())
    },
    "bag" to { args, valueFactory ->
        valueFactory.newBag(
            args.map { deserializeExprValue(it, valueFactory) }.asSequence())
    }
)