package org.partiql.planner.typer

import org.partiql.planner.Header
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType

/**
 * Function arguments list. The planner is responsible for mapping arguments to parameters.
 */
internal typealias Args = List<FunctionParameter>

/**
 * Parameter mapping list tells the planner where to insert implicit casts. Null is the identity.
 */
internal typealias Mapping = List<FunctionSignature.Scalar?>

/**
 * Tells us which function matched, and how the arguments are mapped.
 */
internal class Match<T : FunctionSignature>(
    public val signature: T,
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
    public fun <T : FunctionSignature> match(signatures: List<T>, args: Args): Match<T>? {
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
     *
     * TODO we need to constrain the allowable runtime types for an ANY typed parameter.
     */
    public fun match(signature: FunctionSignature, args: Args): Mapping? {
        if (signature.parameters.size != args.size) {
            return null
        }
        val mapping = ArrayList<FunctionSignature.Scalar?>(args.size)
        for (i in args.indices) {
            val a = args[i]
            val p = signature.parameters[i]
            when {
                // 1. Exact match
                a.type == p.type -> mapping.add(null)
                // 2. Match ANY, no coercion needed
                p.type == PartiQLValueType.ANY -> mapping.add(null)
                // 3. Match NULL argument
                a.type == PartiQLValueType.NULL -> mapping.add(null)
                // 4. Check for a coercion
                else -> {
                    val coercion = header.lookupCoercion(a.type, p.type)
                    when (coercion) {
                        null -> return null // short-circuit
                        else -> mapping.add(coercion)
                    }
                }
            }
        }
        // we made a match
        return mapping
    }
}
