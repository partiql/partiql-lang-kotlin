package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator

internal class RelOffset(
    private val input: Operator.Relation,
    private val offset: Long,
) : Operator.Relation {

    private var init = false
    private var seen = 0

    override fun open() {
        input.open()
        init = false
        seen = 0
    }

    override fun next(): Record? {
        if (!init) {
            while (seen < offset) {
                input.next() ?: return null
                seen += 1
            }
        }
        return input.next()
    }

    override fun close() {
        input.close()
    }
}
