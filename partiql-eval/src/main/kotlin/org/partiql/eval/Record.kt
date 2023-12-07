package org.partiql.eval

import org.partiql.value.PartiQLValue


class Record @OptIn(PartiQLValueExperimental::class) constructor(
    val values: List<PartiQLValue>
)
