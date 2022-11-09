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

/**
 * The core PartiQL data types.
 * TODO: Remove this once SqlParser has been removed from this codebase.
 */
sealed class SqlDataType(val typeName: String, open val arityRange: IntRange) {

    companion object {
        /**
         * This API is for backward compatibility as the SqlDataType is moved from enum class to sealed class
         */
        @JvmStatic
        fun values(): Array<SqlDataType> = arrayOf(
            MISSING, NULL, BOOLEAN, SMALLINT, INTEGER4, INTEGER8, INTEGER, FLOAT, REAL, DOUBLE_PRECISION, DECIMAL,
            NUMERIC, DATE, TIME, TIME_WITH_TIME_ZONE, TIMESTAMP, CHARACTER, CHARACTER_VARYING, STRING, SYMBOL, CLOB,
            BLOB, STRUCT, TUPLE, LIST, SEXP, BAG, ANY
        )

        /*
        * Making this object lazy so that any reference to below objects
        * (non-companion) does not lead to initialization of this object.
        * Compiler assigns a NULL value to the object initially accessed and
        * then initializes all the companion objects. If this is not lazy,
        * compiler will also initialize this object in the same call. Since it
        * references the object with a dot, it leads to NullPointerException
        * for any NULL object. See below links for details -
        * https://youtrack.jetbrains.com/issue/KT-8970
        * https://stackoverflow.com/questions/54940944/initializing-companion-object-after-inner-objects/55010004
        * */
        private val DATA_TYPE_NAME_TO_TYPE_LOOKUP by lazy {
            values().map { Pair(it.typeName, it) }.toMap()
        }

        fun forTypeName(typeName: String): SqlDataType? = DATA_TYPE_NAME_TO_TYPE_LOOKUP[typeName]
    }

    object MISSING : SqlDataType("missing", 0..0)
    object NULL : SqlDataType("null", 0..0)
    object BOOLEAN : SqlDataType("boolean", 0..0)
    object SMALLINT : SqlDataType("smallint", 0..0)
    object INTEGER4 : SqlDataType("integer4", 0..0)
    object INTEGER8 : SqlDataType("integer8", 0..0)
    object INTEGER : SqlDataType("integer", 0..0)
    object FLOAT : SqlDataType("float", 0..1)
    object REAL : SqlDataType("real", 0..0)
    object DOUBLE_PRECISION : SqlDataType("double_precision", 0..0)
    object DECIMAL : SqlDataType("decimal", 0..2)
    object NUMERIC : SqlDataType("numeric", 0..2)
    object TIMESTAMP : SqlDataType("timestamp", 0..0)
    object CHARACTER : SqlDataType("character", 0..1)
    object CHARACTER_VARYING : SqlDataType("character_varying", 0..1)
    object STRING : SqlDataType("string", 0..0)
    object SYMBOL : SqlDataType("symbol", 0..0)
    object CLOB : SqlDataType("clob", 0..0)
    object BLOB : SqlDataType("blob", 0..0)
    object STRUCT : SqlDataType("struct", 0..0)
    object TUPLE : SqlDataType("tuple", 0..0)
    object LIST : SqlDataType("list", 0..0)
    object SEXP : SqlDataType("sexp", 0..0)
    object BAG : SqlDataType("bag", 0..0)
    object ANY : SqlDataType("any", 0..0)
    object DATE : SqlDataType("date", 0..0)
    object TIME : SqlDataType("time", 0..1)
    object TIME_WITH_TIME_ZONE : SqlDataType("time_with_time_zone", 0..1)
    /**
     * Custom partiql data type which is an alias for the core PartiQL data type
     */
    data class CustomDataType(val name: String) : SqlDataType(name, 0..0)
}
