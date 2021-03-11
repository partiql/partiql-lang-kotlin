package org.partiql.lang.ast

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

import com.amazon.ion.system.*
import org.junit.*
import org.partiql.lang.syntax.SqlParser

class IsIonLiteralMetaTest {

    @Test
    fun testIonLiteralMetaPreserved() {
        val ion = IonSystemBuilder.standard().build()
        val ionLiteral = SqlParser(ion).parseExprNode("`1.0`")
        Assert.assertTrue(ionLiteral.metas.hasMeta(IsIonLiteralMeta.TAG))
        val roundTrippedIonLiteral = ionLiteral.toAstStatement().toExprNode(ion)
        Assert.assertTrue(roundTrippedIonLiteral.metas.hasMeta(IsIonLiteralMeta.TAG))
    }
}