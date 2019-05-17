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

package org.partiql.lang.ast.passes

import com.amazon.ion.*
import com.amazon.ion.system.*
import org.partiql.lang.ast.*
import org.junit.*
import kotlin.test.*

class SubstitutionRewriterTest {
    private val ion = IonSystemBuilder.standard().build()
    private val metasWithSourceLocation = metaContainerOf(SourceLocationMeta(100, 101))

    private fun litInt(value: Int) = Literal(ion.newInt(value), metaContainerOf())

    private val pairs = listOf(SubstitutionPair(litInt(1), litInt(2))).associateBy { it.target }

    private val rewriter = SubstitutionRewriter(pairs)

    @Test
    fun matchingTargetIsReplaced() {
        val original = litInt(1)
        val rewritten = rewriter.rewriteExprNode(original) as Literal

        // (lit 1) should be replaced with (lit 2)
        assertEquals(2, (rewritten.ionValue as IonInt).intValue())

        // because [original] has no [SourceLocationMeta]:
        assertNull(rewritten.metas.sourceLocation)
    }

    @Test
    fun matchingTargetIsReplacedAndSourceLocationIsCopiedToTarget() {
        val original = litInt(1).copy(metas = metasWithSourceLocation)
        val rewritten = rewriter.rewriteExprNode(original) as Literal

        // (lit 1) should be replaced with (lit 2)
        assertEquals(2, (rewritten.ionValue as IonInt).intValue())

        // and the original [SourceLocationMeta] should be copied to the replacement.
        assertEquals(100, rewritten.metas.sourceLocation!!.lineNum)
        assertEquals(101, rewritten.metas.sourceLocation!!.charOffset)
    }

    @Test
    fun nonMatchingTargetIsNotReplaced() {
        val original = litInt(3)
        val rewritten = rewriter.rewriteExprNode(original) as Literal

        // (lit 3) should still be (lit 3)
        assertEquals(3, (rewritten.ionValue as IonInt).intValue())
    }

}