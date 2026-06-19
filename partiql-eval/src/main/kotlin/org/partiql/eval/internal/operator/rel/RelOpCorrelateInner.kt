/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file.
 */

package org.partiql.eval.internal.operator.rel

import org.partiql.eval.Environment
import org.partiql.eval.ExprRelation
import org.partiql.eval.Row
import org.partiql.eval.internal.helpers.checkInterrupted

/**
 * Correlated (lateral) inner join. The RHS is opened per LHS row with the LHS row pushed into the environment.
 *
 * Algorithm:
 * ```
 * for lhsRecord in lhs:
 *   for rhsRecord in rhs(lhsRecord):
 *     yield(lhsRecord + rhsRecord)
 * ```
 */
internal class RelOpCorrelateInner(
    private val lhs: ExprRelation,
    private val rhs: ExprRelation,
) : RelOpPeeking() {

    private lateinit var env: Environment
    private lateinit var iterator: Iterator<Row>

    override fun openPeeking(env: Environment) {
        this.env = env
        lhs.open(env)
        iterator = implementation()
    }

    override fun peek(): Row? {
        return when (iterator.hasNext()) {
            true -> iterator.next()
            false -> null
        }
    }

    override fun closePeeking() {
        lhs.close()
        rhs.close()
        iterator = emptyList<Row>().iterator()
    }

    private fun implementation() = iterator {
        for (lhsRecord in lhs) {
            rhs.open(env.push(lhsRecord))
            for (rhsRecord in rhs) {
                checkInterrupted()
                yield(lhsRecord.concat(rhsRecord))
            }
        }
    }
}
