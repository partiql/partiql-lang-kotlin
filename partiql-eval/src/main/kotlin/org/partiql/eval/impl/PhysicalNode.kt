package org.partiql.eval.impl

import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

internal interface PhysicalNode {

    interface Expression : PhysicalNode {
        @OptIn(PartiQLValueExperimental::class)
        fun evaluate(record: Record): PartiQLValue
    }

    interface Relation : PhysicalNode {
        fun evaluate(): Iterator<Record>
    }
}
