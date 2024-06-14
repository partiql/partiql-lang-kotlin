package org.partiql.planner.intern.validate.validators

import org.partiql.planner.intern.ptype.PType
import org.partiql.planner.internal.FnMatch
import org.partiql.planner.metadata.Fn

internal class FnValidator(variants: List<Fn.Scalar>) {

    fun resolve(variants: List<Fn.Scalar>, args: List<PType>): FnMatch? = TODO()
}
