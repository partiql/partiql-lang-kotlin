package org.partiql.planner.internal

import org.partiql.planner.internal.casts.CastTable
import org.partiql.spi.connector.ConnectorFn
import org.partiql.spi.connector.ConnectorHandle
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnSignature

/**
 *
 *
 * https://www.postgresql.org/docs/current/typeconv-func.html
 */
@OptIn(FnExperimental::class)
internal object FnResolver {

    private val casts = CastTable

    fun resolve(fn: ConnectorHandle<ConnectorFn>, args: FnArgs): FnSignature? {
        TODO()
    }

    // /**
    //  * Attempt to match arguments to the parameters; return the implicit casts if necessary.
    //  *
    //  * TODO we need to constrain the allowable runtime types for an ANY typed parameter.
    //  */
    // fun match(signature: FnSignature, args: FnArgs): boo {
    //     if (signature.parameters.size != args.size) {
    //         return null
    //     }
    //     // val mapping = ArrayList<FunctionSignature.Scalar?>(args.size)
    //     for (i in args.indices) {
    //         val a = args[i]
    //         val p = signature.parameters[i]
    //         when {
    //             // 1. Exact match
    //             a.type == p.type -> continue
    //             // 2. Match ANY, no coercion needed
    //             p.type == ANY -> mapping.add(null)
    //             // 3. Match NULL argument
    //             a.type == NULL -> mapping.add(null)
    //             // 4. Check for a coercion
    //             else -> {
    //
    //                 val coercion = lookupCoercion(a.type, p.type)
    //                 when (coercion) {
    //                     null -> return null // short-circuit
    //                     else -> mapping.add(coercion)
    //                 }
    //             }
    //         }
    //     }
    //     // if all elements requires casting, then no match
    //     // because there must be another function definition that requires no casting
    //     return if (mapping.isEmpty() || mapping.contains(null)) {
    //         // we made a match
    //         mapping
    //     } else {
    //         null
    //     }
    // }
    //
    // public fun resolveFn(fn: Fn.Unresolved, args: List<Rex>): FnMatch<FunctionSignature.Scalar> {
    //     val candidates = lookup(fn)
    //     var canReturnMissing = false
    //     val parameterPermutations = buildArgumentPermutations(args.map { it.type }).mapNotNull { argList ->
    //         argList.mapIndexed { i, arg ->
    //             if (arg.isMissable()) {
    //                 canReturnMissing = true
    //             }
    //             // Skip over if we cannot convert type to runtime type.
    //             val argType = arg.toRuntimeTypeOrNull() ?: return@mapNotNull null
    //             FunctionParameter("arg-$i", argType)
    //         }
    //     }
    //     val potentialFunctions = parameterPermutations.mapNotNull { parameters ->
    //         when (val match = match(candidates, parameters)) {
    //             null -> {
    //                 canReturnMissing = true
    //                 null
    //             }
    //             else -> {
    //                 val isMissable = canReturnMissing || isUnsafeCast(match.signature.specific)
    //                 FnMatch.Ok(match.signature, match.mapping, isMissable)
    //             }
    //         }
    //     }
    //     // Remove duplicates while maintaining order (precedence).
    //     val orderedUniqueFunctions = potentialFunctions.toSet().toList()
    //     return when (orderedUniqueFunctions.size) {
    //         0 -> FnMatch.Error(fn.identifier, args, candidates)
    //         1 -> orderedUniqueFunctions.first()
    //         else -> FnMatch.Dynamic(orderedUniqueFunctions, canReturnMissing)
    //     }
    // }
    //
    // private fun buildArgumentPermutations(args: List<StaticType>): List<List<StaticType>> {
    //     val flattenedArgs = args.map { it.flatten().allTypes }
    //     return buildArgumentPermutations(flattenedArgs, accumulator = emptyList())
    // }
    //
    // private fun buildArgumentPermutations(
    //     args: List<List<StaticType>>,
    //     accumulator: List<StaticType>,
    // ): List<List<StaticType>> {
    //     if (args.isEmpty()) {
    //         return listOf(accumulator)
    //     }
    //     val first = args.first()
    //     val rest = when (args.size) {
    //         1 -> emptyList()
    //         else -> args.subList(1, args.size)
    //     }
    //     return buildList {
    //         first.forEach { argSubType ->
    //             addAll(buildArgumentPermutations(rest, accumulator + listOf(argSubType)))
    //         }
    //     }
    // }

    // /**
    //  * Leverages a [FnResolver] to find a matching function defined in the [Header] aggregation function catalog.
    //  */
    // public fun resolveAgg(agg: Agg.Unresolved, args: List<Rex>): FnMatch<FunctionSignature.Aggregation> {
    //     val candidates = lookup(agg)
    //     var hadMissingArg = false
    //     val parameters = args.mapIndexed { i, arg ->
    //         if (!hadMissingArg && arg.type.isMissable()) {
    //             hadMissingArg = true
    //         }
    //         FunctionParameter("arg-$i", arg.type.toRuntimeType())
    //     }
    //     val match = match(candidates, parameters)
    //     return when (match) {
    //         null -> FnMatch.Error(agg.identifier, args, candidates)
    //         else -> {
    //             val isMissable = hadMissingArg || isUnsafeCast(match.signature.specific)
    //             FnMatch.Ok(match.signature, match.mapping, isMissable)
    //         }
    //     }
    // }

    // /**
    //  * Functions are sorted by precedence (which is not rigorously defined/specified at the moment).
    //  */
    // private fun <T : FnSignature> match(signatures: List<T>, args: FnArgs): Match<T>? {
    //     for (signature in signatures) {
    //         val mapping = match(signature, args)
    //         if (mapping != null) {
    //             return Match(signature, mapping)
    //         }
    //     }
    //     return null
    // }
    // companion object {
    // }
}
