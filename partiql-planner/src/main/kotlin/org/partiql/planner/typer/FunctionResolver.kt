package org.partiql.planner.typer

import org.partiql.planner.Header
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValueExperimental

/**
 * Function arguments list. The planner is responsible for mapping arguments to parameters.
 */
internal typealias Args = List<FunctionParameter>

/**
 * Parameter mapping list tells the planner where to insert implicit casts. Null is the identity.
 */
internal typealias Mapping = List<FunctionSignature?>

/**
 * Tells us which function matched, and how the arguments are mapped.
 */
internal class Match(
    public val signature: FunctionSignature,
    public val mapping: Mapping,
)

/**
 * Logic for matching signatures to arguments.
 */
@OptIn(PartiQLValueExperimental::class)
internal class FunctionResolver(private val header: Header) {

    /**
     * Functions are sorted by precedence (which is not rigorously defined/specified at the moment).
     */
    public fun match(signatures: List<FunctionSignature>, args: Args): Match? {
        for (signature in signatures) {
            val mapping = match(signature, args)
            if (mapping != null) {
                return Match(signature, mapping)
            }
        }
        return null
    }

    /**
     * Attempt to match arguments to the parameters; return the implicit casts if necessary.
     */
    public fun match(signature: FunctionSignature, args: Args): Mapping? {
        if (signature.parameters.size != args.size) {
            return null
        }
        val mapping = ArrayList<FunctionSignature?>(args.size)
        for (i in args.indices) {
            val a = args[i]
            val p = signature.parameters[i]
            when {
                // 1. Different parameter types
                a::class != p::class -> return null
                // 2. Exact match
                a.type == p.type -> mapping.add(null)
                // 3. Check for coercion
                else -> {
                    val cast = header.lookupCast(a.type, p.type)
                    when (cast) {
                        null -> return null // short-circuit
                        else -> mapping.add(cast)
                    }
                }
            }
        }
        // we made a match
        return mapping
    }
}
