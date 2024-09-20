package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.operator.Operator
import org.partiql.spi.value.Datum

/**
 * Implementation for variable lookup; walks up environments if necessary, otherwise lookup using tuple offset.
 */
internal class ExprVar(depth: Int, offset: Int) : Operator.Expr {

    // DO NOT USE FINAL
    private var _depth = depth
    private var _offset = offset

    override fun eval(env: Environment): Datum {
        // shortcut for depth 0
        if (_depth == 0) {
            return env[_offset]
        }
        // walk up environments
        var curr = env
        repeat(_depth) {
            curr = curr.next() ?: error("We ran out of environments for depth ($_depth) and env: $env.")
        }
        return curr.getOrNull(_offset) ?: error("The env doesn't have a variable for depth/offset ($_depth/$_offset) and env: $env. Current is: $curr.")
    }
}
