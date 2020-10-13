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

package org.partiql.lang.eval.visitors

import com.amazon.ionelement.api.emptyMetaContainer
import com.amazon.ionelement.api.ionInt
import org.junit.Test
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.ast.metaContainerOf
import org.partiql.lang.ast.toIonElementMetaContainer
import org.partiql.lang.domains.PartiqlAst
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SubstitutionVisitorTransformTest {
    private val metasWithSourceLocation = metaContainerOf(SourceLocationMeta(100, 101)).toIonElementMetaContainer()

    private fun litInt(value: Long) = PartiqlAst.build { lit(ionInt(value), emptyMetaContainer()) }

    private val pairs = listOf(SubstitutionPair(litInt(1), litInt(2))).associateBy { it.target }

    private val transformer = SubstitutionVisitorTransform(pairs)

    @Test
    fun matchingTargetIsReplaced() {
        val original = litInt(1)
        val transformedExpr = transformer.transformExpr(original)

        val transformedLiteral = transformedExpr.toIonElement().values[1]

        // (lit 1) should be replaced with (lit 2)
        assertEquals(2, transformedLiteral.longValue)

        // because [original] has no [SourceLocationMeta]:
        assertNull(transformedExpr.metas[SourceLocationMeta.TAG])
    }

    @Test
    fun matchingTargetIsReplacedAndSourceLocationIsCopiedToTarget() {
        val original = PartiqlAst.build { lit(ionInt(1), metasWithSourceLocation) }
        val transformedExpr = transformer.transformExpr(original)

        val transformedLiteral = transformedExpr.toIonElement().values[1].longValue

        // (lit 1) should be replaced with (lit 2)
        assertEquals(2, transformedLiteral)

        val tag = transformedExpr.metas[SourceLocationMeta.TAG] as SourceLocationMeta
        // and the original [SourceLocationMeta] should be copied to the replacement.
        assertEquals(100, tag.lineNum)
        assertEquals(101, tag.charOffset)
    }

    @Test
    fun nonMatchingTargetIsNotReplaced() {
        val original = litInt(3)
        val transformedExpr = transformer.transformExpr(original)

        // (lit 3) should still be (lit 3)
        assertEquals(3, transformedExpr.toIonElement().values[1].longValue)
    }

}
