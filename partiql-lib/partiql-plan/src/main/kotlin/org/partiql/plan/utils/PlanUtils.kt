package org.partiql.plan.utils

import org.partiql.plan.ir.Attribute
import org.partiql.plan.ir.Rel

internal object PlanUtils {
    internal fun getSchema(input: Rel): List<Attribute> = when (input) {
        is Rel.Project -> input.common.schema
        is Rel.Aggregate -> input.common.schema
        is Rel.Bag -> input.common.schema
        is Rel.Fetch -> input.common.schema
        is Rel.Filter -> input.common.schema
        is Rel.Join -> input.common.schema
        is Rel.Scan -> input.common.schema
        is Rel.Sort -> input.common.schema
        is Rel.Unpivot -> input.common.schema
    }
}
