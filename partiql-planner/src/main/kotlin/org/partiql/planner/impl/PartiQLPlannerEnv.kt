package org.partiql.planner.impl

import org.partiql.ast.Identifier
import org.partiql.plan.Fn
import org.partiql.plan.PartiQLHeader
import org.partiql.plan.Plan
import org.partiql.plan.Rex
import org.partiql.plan.Type
import org.partiql.plan.builder.PlanFactory
import org.partiql.planner.Catalog

/**
 * Hardcoded PartiQL Global Catalog
 *
 * !! ONLY DOING ATOMIC TYPES FOR V0 !!
 * !! NEED TO BACK BY A CATALOG!!
 * !! HIGHLY SIMPLIFIED FOR BOOTSTRAPPING PURPOSES !!
 */
internal class PartiQLPlannerEnv {

    private val factory = Plan
    private val catalog = partiql()

    //
    private fun header(): PartiQLHeader = factory.partiQLHeader(
        types = catalog.types,
        functions = catalog.functions,
    )

    // TYPES

    /**
     * Get a Type.Ref from a StaticType
     */
    fun resolveType(type: Type.Atomic): Type.Ref {
        catalog.types.forEachIndexed { i, t ->
            // need .equals() if we want to include more variants
            if (t.symbol == type.symbol) {
                return Plan.typeRef(t.symbol, i)
            }
        }
        throw IllegalArgumentException("Catalog does not contain type ${type.symbol}")
    }

    // FUNCTIONS

    /**
     * This will need to be greatly improved upon. We will need to return some kind of pair which has a list of
     * implicit casts to introduce.
     */
    fun resolveFn(identifier: Identifier, args: List<Rex.Op.Call.Arg>): Fn.Ref {
        when (identifier) {
            is Identifier.Qualified -> throw IllegalArgumentException("Qualified function identifiers not supported")
            is Identifier.Symbol -> {
                val symbol = identifier.symbol.lowercase()
                // TODO actual function resolution
                return Plan.fnRefResolved(symbol, 0)
            }
        }
    }

    // Hardcoded PartiQL Global Catalog
    private fun partiql() = with(factory) {
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
