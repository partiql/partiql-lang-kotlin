
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
import com.amazon.ion.system.*
import org.junit.*
import org.junit.Test
import kotlin.test.*

/**
 * rewrites the ast mixing the identifiers case, for example `(id identifier)` is rewritten to `(id IdEnTiFiEr)`.
 *
 * Useful to test case insensitive operations
 */
internal fun IonSexp.mixIdentifierCase(): IonSexp {
    val ion = this.system

    fun IonSymbol.cloneMixingCase(): IonSymbol {
        val mixedCase = stringValue()!!.foldIndexed("") { index, acc, c ->
            acc + when (index % 2 == 0) {
                true  -> c.toUpperCase()
                false -> c.toLowerCase()
            }
        }

        return ion.newSymbol(mixedCase)
    }

    fun IonSexp.isId(): Boolean = size == 2 &&
                                  this[0] is IonSymbol &&
                                  this[0].stringValue() == "id"

    fun IonSexp.copyRewritingNodes(): IonSexp = foldIndexed(ion.newEmptySexp()) { index, newNode, element ->
        val rewritten = when {
            element is IonSymbol && this.isId() && index == 1 -> element.cloneMixingCase()
            element is IonSexp                                -> element.copyRewritingNodes()
            else                                              -> element.clone()
        }

        newNode.add(rewritten)
        newNode
    }

    return this.copyRewritingNodes()
}

internal class AstExtensionsTest {
    @Test
    fun name() {
        val ion = IonSystemBuilder.standard().build()
        val sexp = ion.singleValue("(foo (id bar) bar (+ 1 (id identifier)))") as IonSexp

        assertEquals("(foo (id BaR) bar (+ 1 (id IdEnTiFiEr)))", sexp.mixIdentifierCase().toString())
    }
}
