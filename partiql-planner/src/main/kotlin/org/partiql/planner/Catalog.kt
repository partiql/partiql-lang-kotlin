package org.partiql.planner

import org.partiql.plan.Fn
import org.partiql.plan.Type
import org.partiql.plan.builder.PlanFactory
import org.partiql.value.PartiQLValueExperimental

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

    companion object {

        // TEMPORARY — Hardcoded PartiQL Global Catalog
        public fun partiql(factory: PlanFactory = PlanFactory.DEFAULT) = with(factory) {
            // Helpers
            val anyT = typeRef("any", 0)
            val boolT = typeRef("bool", 3)
            val intT = typeRef("int", 8)
            val floatT = typeRef("float64", 11)
            val stringT = typeRef("string", 13)
            // Hardcoded for now
            Catalog(
                name = "partiql",
                types = listOf(
                    typeAtomic("any"),          // 0
                    typeAtomic("null"),         // 1
                    typeAtomic("missing"),      // 2
                    typeAtomic("bool"),         // 3
                    typeAtomic("int8"),         // 4
                    typeAtomic("int16"),        // 5
                    typeAtomic("int32"),        // 6
                    typeAtomic("int64"),        // 7
                    typeAtomic("int"),          // 8
                    typeAtomic("decimal"),      // 9
                    typeAtomic("float32"),      // 10
                    typeAtomic("float64"),      // 11
                    typeAtomic("char"),         // 12
                    typeAtomic("string"),       // 13
                    typeAtomic("symbol"),       // 14
                    typeAtomic("bit"),          // 15
                    typeAtomic("binary"),       // 16
                    typeAtomic("byte"),         // 17
                    typeAtomic("blob"),         // 18
                    typeAtomic("clob"),         // 19
                    typeAtomic("date"),         // 20
                    typeAtomic("time"),         // 21
                    typeAtomic("timestamp"),    // 22
                    typeAtomic("interval"),     // 23
                    typeAtomic("bag"),          // 24
                    typeAtomic("list"),         // 25
                    typeAtomic("sexp"),         // 26
                    typeAtomic("struct"),       // 27
                ),
                // Basic functions
                // TODO Generate from https://web.cecs.pdx.edu/~len/sql1999.pdf#page=861
                functions = listOf(
                    fn(
                        id = "plus",
                        params = listOf(v(intT), v(intT)),
                        returns = intT
                    ),
                ),
            )
        }

        private fun PlanFactory.v(type: Type.Ref) = this.fnParamValue(type)

        private fun PlanFactory.t(type: Type.Ref) = this.fnParamType(type)
    }
}
