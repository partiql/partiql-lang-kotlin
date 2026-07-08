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
import org.partiql.plan.rel.RelType
import org.partiql.spi.value.Datum

/**
 * Correlated (lateral) left outer join. The RHS is opened per LHS row with the LHS row pushed into the environment.
 * LHS rows with no RHS match are preserved with NULL-padded RHS.
 *
 * Algorithm:
 * ```
 * for lhsRecord in lhs:
 *   matched = false
 *   for rhsRecord in rhs(lhsRecord):
 *     matched = true
 *     yield(lhsRecord + rhsRecord)
 *   if (!matched):
 *     yield(lhsRecord + NULL_RECORD)
 * ```
 */
internal class RelOpCorrelateLeft(
    private val lhs: ExprRelation,
    private val rhs: ExprRelation,
    rhsType: RelType,
) : RelOpPeeking() {

    private val rhsPadded = Row(
        rhsType.getFields().map { Datum.nullValue(it.type) }.toTypedArray()
    )

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
            var lhsMatched = false
            rhs.open(env.push(lhsRecord))
            for (rhsRecord in rhs) {
                checkInterrupted()
                lhsMatched = true
                yield(lhsRecord.concat(rhsRecord))
            }
            rhs.close()
            if (!lhsMatched) {
                yield(lhsRecord.concat(rhsPadded))
            }
        }
    }
}
