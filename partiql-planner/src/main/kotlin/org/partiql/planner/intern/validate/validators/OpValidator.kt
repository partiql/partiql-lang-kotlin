package org.partiql.planner.intern.validate.validators

import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.metadata.Operator

/**
 * TODO
 *
 * @property variants
 */
internal class OpValidator(val variants: List<Operator>) {

    fun validate(lhs: Rex?, rhs: Rex?): Rex? = null
}
