package org.partiql.eval.internal.plan

import org.partiql.plan.Exclusion
import org.partiql.plan.WindowFunctionSignature
import org.partiql.plan.rel.RelType

internal sealed class PRel {
    abstract val type: RelType?

    data class Scan(val expr: PExpr, override val type: RelType? = null) : PRel()
    data class Iterate(val expr: PExpr, override val type: RelType? = null) : PRel()
    data class Unpivot(val expr: PExpr, override val type: RelType? = null) : PRel()
    data class Filter(val input: PRel, val predicate: PExpr, override val type: RelType? = null) : PRel()
    data class Project(val input: PRel, val projections: List<PExpr>, override val type: RelType? = null) : PRel()
    data class Join(val lhs: PRel, val rhs: PRel, val condition: PExpr, val joinType: PJoinType, override val type: RelType? = null) : PRel()
    data class Sort(val input: PRel, val collations: List<PCollation>, override val type: RelType? = null) : PRel()
    data class Distinct(val input: PRel, override val type: RelType? = null) : PRel()
    data class Limit(val input: PRel, val limit: PExpr, override val type: RelType? = null) : PRel()
    data class Offset(val input: PRel, val offset: PExpr, override val type: RelType? = null) : PRel()
    data class Aggregate(val input: PRel, val measures: List<PMeasure>, val groups: List<PExpr>, override val type: RelType? = null) : PRel()
    data class Union(val lhs: PRel, val rhs: PRel, val all: Boolean, override val type: RelType? = null) : PRel()
    data class Intersect(val lhs: PRel, val rhs: PRel, val all: Boolean, override val type: RelType? = null) : PRel()
    data class Except(val lhs: PRel, val rhs: PRel, val all: Boolean, override val type: RelType? = null) : PRel()
    data class Exclude(val input: PRel, val exclusions: List<Exclusion>, override val type: RelType? = null) : PRel()
    data class Window(val input: PRel, val functions: List<PWindowFn>, val partitions: List<PExpr>, val sorts: List<PCollation>, override val type: RelType? = null) : PRel()
}

internal data class PCollation(val expr: PExpr, val desc: Boolean, val nullsLast: Boolean)
internal data class PMeasure(val catalogId: Int, val aggId: Int, val args: List<PExpr>, val distinct: Boolean)
internal data class PWindowFn(val signature: WindowFunctionSignature, val args: List<PExpr>)

internal enum class PJoinType { INNER, LEFT, RIGHT, FULL }
