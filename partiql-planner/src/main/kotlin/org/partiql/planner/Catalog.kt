package org.partiql.planner

import org.partiql.plan.Fn
import org.partiql.plan.Type

// Simplified Catalog for plan bootstrapping
public class Catalog(
    val name: String,
    val types: List<Type.Atomic>,
    val functions: List<Fn>
) {
    fun lookup(ref: Type.Ref): Type.Atomic {
        if (ref.ordinal < 0) {
            throw IllegalArgumentException("Negative index ${ref.ordinal}")
        }
        if (ref.ordinal >= types.size) {
            throw IllegalArgumentException("Ordinal out of range")
        }
        return types[ref.ordinal]
    }

    fun lookup(ref: Fn.Ref.Resolved): Fn? {
        if (ref.ordinal < 0) {
            throw IllegalArgumentException("Negative index ${ref.ordinal}")
        }
        if (ref.ordinal >= functions.size) {
            throw IllegalArgumentException("Ordinal out of range")
        }
        return functions[ref.ordinal]
    }
}
