package org.partiql.planner

import org.partiql.plan.Fn
import org.partiql.plan.Global
import org.partiql.plan.Identifier
import org.partiql.plan.PartiQLHeader
import org.partiql.plan.Plan
import org.partiql.plan.Rex
import org.partiql.plan.Type

/**
 * Hardcoded PartiQL Global Catalog
 *
 * !! ONLY DOING ATOMIC TYPES FOR V0 !!
 * !! NEED TO BACK BY A CATALOG!!
 * !! HIGHLY SIMPLIFIED FOR BOOTSTRAPPING PURPOSES !!
 */
internal class PartiQLPlannerEnv(private val session: PartiQLPlanner.Session) {

    private val factory = Plan
    private val catalog = Catalog.partiql()

    private val globals = mutableListOf<Global>()

    // TEMPORARY
    internal fun header(): PartiQLHeader = factory.partiQLHeader(
        types = catalog.types,
        functions = catalog.functions,
    )

    // TEMPORARY
    internal fun globals(): List<Global> = globals

    // TYPES

    /**
     * Get a Type.Ref from a StaticType
     */
    internal fun resolveType(type: Type.Atomic): Type.Ref {
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
    internal fun resolveFn(identifier: Identifier, args: List<Rex.Op.Call.Arg>): Fn.Ref {
        when (identifier) {
            is Identifier.Qualified -> throw IllegalArgumentException("Qualified function identifiers not supported")
            is Identifier.Symbol -> {
                val symbol = identifier.symbol.lowercase()
                // TODO actual function resolution
                return Plan.fnRefResolved(symbol, 0)
            }
        }
    }

    internal fun resolveAggFn(identifier: Identifier, args: List<Rex.Op.Call.Arg>): Fn.Ref {
        when (identifier) {
            is Identifier.Qualified -> throw IllegalArgumentException("Qualified function identifiers not supported")
            is Identifier.Symbol -> {
                val symbol = identifier.symbol.lowercase()
                // TODO actual function resolution
                return Plan.fnRefResolved(symbol, 0)
            }
        }
    }
    internal fun resolveGlobal(identifier: Identifier): Rex.Op.Global? {
        // TODO
        return null
    }
}
