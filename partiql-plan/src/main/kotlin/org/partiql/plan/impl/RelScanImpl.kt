package org.partiql.plan.rel

import org.partiql.plan.rex.Rex

/**
 * Default [RelScan] implementation.
 */
internal class RelScanImpl(input: Rex) : RelScan {

    // DO NOT USE FINAL
    private var _input: Rex = input

    override fun getInput(): Rex = _input

    override fun getType(): RelType {
        TODO("Implement getSchema for scan")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is RelScan) return false
        return _input == other.getInput()
    }

    override fun hashCode(): Int {
        return _input.hashCode()
    }
}