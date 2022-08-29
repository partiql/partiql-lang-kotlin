package org.partiql.lang.ots_work.plugins.standard.plugin

import org.partiql.lang.errors.ErrorCode

/**
 * Defines the behavior when divisor is zero for DIVIDE operator
 */
enum class BehaviorWhenDivisorIsZero {
    /**
     * Throws error with error code of [ErrorCode.EVALUATOR_MODULO_BY_ZERO]
     */
    ERROR,

    /**
     * Returns MISSING
     */
    MISSING
}