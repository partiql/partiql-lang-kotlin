package org.partiql.lang.util

import org.partiql.lang.eval.time.SECONDS_PER_HOUR
import org.partiql.lang.eval.time.SECONDS_PER_MINUTE
import java.time.ZoneOffset
import kotlin.math.absoluteValue

/**
 * Returns the string representation of the [ZoneOffset] in HH:mm format.
 */
internal fun ZoneOffset.getOffsetHHmm(): String =
    (if(totalSeconds >= 0) "+" else "-") +
    (totalSeconds / SECONDS_PER_HOUR).absoluteValue.toString().padStart(2, '0') +
    ":" +
    ((totalSeconds / SECONDS_PER_MINUTE) % SECONDS_PER_MINUTE).absoluteValue.toString().padStart(2, '0')
