package org.partiql.planner.internal.functions.sql

import org.partiql.planner.catalog.Function

internal interface SqlFunction {
    fun getName(): String
    fun getVariants(): List<Function.Scalar>
}
