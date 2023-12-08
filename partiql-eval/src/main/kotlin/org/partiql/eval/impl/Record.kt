package org.partiql.eval.impl

import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

internal class Record @OptIn(PartiQLValueExperimental::class) constructor(
    val values: List<PartiQLValue>
)
