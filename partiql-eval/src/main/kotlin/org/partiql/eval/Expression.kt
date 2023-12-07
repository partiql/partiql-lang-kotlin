package org.partiql.eval

import org.partiql.value.PartiQLValue


interface Expression {
    fun evaluate(record: Record): PartiQLValue
}
