/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
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

package org.partiql.planner.typer

import org.partiql.plan.Rex
import org.partiql.plan.rex
import org.partiql.plan.util.PlanRewriter

/**
 * Uses to replace [Rex]'s within an expression tree.
 */
internal object RexReplacer {

    /**
     * Within the [Rex] tree of [rex], replaces all instances of [replace] with the [with].
     */
    internal fun replace(rex: Rex, replace: Rex, with: Rex): Rex {
        val params = ReplaceParams(replace, with)
        return RexReplacerImpl.visitRex(rex, params)
    }

    private class ReplaceParams(val replace: Rex, val with: Rex)

    private object RexReplacerImpl : PlanRewriter<ReplaceParams>() {

        override fun visitRex(node: Rex, ctx: ReplaceParams): Rex {
            if (node == ctx.replace) { return ctx.with }
            val op = visitRexOp(node.op, ctx) as Rex.Op
            return if (op !== node.op) rex(node.type, op) else node
        }
    }
}
