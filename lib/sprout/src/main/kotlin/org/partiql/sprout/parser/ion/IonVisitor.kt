/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.partiql.sprout.parser.ion

import com.amazon.ion.IonBlob
import com.amazon.ion.IonBool
import com.amazon.ion.IonClob
import com.amazon.ion.IonDatagram
import com.amazon.ion.IonDecimal
import com.amazon.ion.IonFloat
import com.amazon.ion.IonInt
import com.amazon.ion.IonList
import com.amazon.ion.IonLob
import com.amazon.ion.IonNull
import com.amazon.ion.IonNumber
import com.amazon.ion.IonSequence
import com.amazon.ion.IonSexp
import com.amazon.ion.IonString
import com.amazon.ion.IonStruct
import com.amazon.ion.IonSymbol
import com.amazon.ion.IonText
import com.amazon.ion.IonTimestamp
import com.amazon.ion.IonValue

/**
 * Not using ValueVisitor here since it does not have a parameterized return type or visitor context.
 */
interface IonVisitor<R, C> {

    /*
     * This could be `<R,C> IonValue.accept(v: Visitor<R, C>, ctx: C): R`, but this is slightly cleaner.
     */
    fun visit(v: IonValue, ctx: C): R = when (v) {
        is IonBlob -> visit(v, ctx)
        is IonBool -> visit(v, ctx)
        is IonClob -> visit(v, ctx)
        is IonDatagram -> visit(v, ctx)
        is IonDecimal -> visit(v, ctx)
        is IonFloat -> visit(v, ctx)
        is IonInt -> visit(v, ctx)
        is IonList -> visit(v, ctx)
        is IonLob -> visit(v, ctx)
        is IonNull -> visit(v, ctx)
        is IonNumber -> visit(v, ctx)
        is IonSexp -> visit(v, ctx)
        is IonSequence -> visit(v, ctx)
        is IonString -> visit(v, ctx)
        is IonStruct -> visit(v, ctx)
        is IonSymbol -> visit(v, ctx)
        is IonText -> visit(v, ctx)
        is IonTimestamp -> visit(v, ctx)
        else -> throw IllegalArgumentException("unknown IonValue $v")
    }

    fun visit(v: IonBool, ctx: C): R = defaultVisit(v, ctx)

    fun visit(v: IonBlob, ctx: C): R = defaultVisit(v, ctx)

    fun visit(v: IonClob, ctx: C): R = defaultVisit(v, ctx)

    fun visit(v: IonDatagram, ctx: C): R = defaultVisit(v, ctx)

    fun visit(v: IonDecimal, ctx: C): R = defaultVisit(v, ctx)

    fun visit(v: IonFloat, ctx: C): R = defaultVisit(v, ctx)

    fun visit(v: IonInt, ctx: C): R = defaultVisit(v, ctx)

    fun visit(v: IonList, ctx: C): R = defaultVisit(v, ctx)

    fun visit(v: IonLob, ctx: C): R = defaultVisit(v, ctx)

    fun visit(v: IonNull, ctx: C): R = defaultVisit(v, ctx)

    fun visit(v: IonNumber, ctx: C): R = defaultVisit(v, ctx)

    fun visit(v: IonSexp, ctx: C): R = defaultVisit(v, ctx)

    fun visit(v: IonSequence, ctx: C): R = defaultVisit(v, ctx)

    fun visit(v: IonString, ctx: C): R = defaultVisit(v, ctx)

    fun visit(v: IonStruct, ctx: C): R = defaultVisit(v, ctx)

    fun visit(v: IonSymbol, ctx: C): R = defaultVisit(v, ctx)

    fun visit(v: IonText, ctx: C): R = defaultVisit(v, ctx)

    fun visit(v: IonTimestamp, ctx: C): R = defaultVisit(v, ctx)

    fun defaultVisit(v: IonValue, ctx: C): R
}
