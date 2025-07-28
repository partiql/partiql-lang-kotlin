package org.partiql.eval.internal.operator.rel

import org.partiql.eval.ExprValue

/**
 * DO NOT USE FINAL.
 *
 * @property expr   The expression to sort by..
 * @property desc   True iff DESC sort, otherwise ASC.
 * @property last   True iff NULLS LAST sort, otherwise NULLS FIRST.
 */
internal class Collation(
    @JvmField var expr: ExprValue,
    @JvmField var desc: Boolean,
    @JvmField var last: Boolean,
)
