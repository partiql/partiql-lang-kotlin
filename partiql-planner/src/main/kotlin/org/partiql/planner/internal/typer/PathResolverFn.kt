package org.partiql.planner.internal.typer

import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.internal.ir.Fn
import org.partiql.planner.internal.ir.Identifier
import org.partiql.planner.internal.ir.Rex
import org.partiql.spi.BindingCase
import org.partiql.spi.BindingName
import org.partiql.spi.BindingPath
import org.partiql.spi.connector.ConnectorMetadata
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.types.StaticType
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType
import org.partiql.value.PartiQLValueType.ANY
import org.partiql.value.PartiQLValueType.BAG
import org.partiql.value.PartiQLValueType.BINARY
import org.partiql.value.PartiQLValueType.BLOB
import org.partiql.value.PartiQLValueType.BOOL
import org.partiql.value.PartiQLValueType.BYTE
import org.partiql.value.PartiQLValueType.CHAR
import org.partiql.value.PartiQLValueType.CLOB
import org.partiql.value.PartiQLValueType.DATE
import org.partiql.value.PartiQLValueType.DECIMAL
import org.partiql.value.PartiQLValueType.DECIMAL_ARBITRARY
import org.partiql.value.PartiQLValueType.FLOAT32
import org.partiql.value.PartiQLValueType.FLOAT64
import org.partiql.value.PartiQLValueType.INT
import org.partiql.value.PartiQLValueType.INT16
import org.partiql.value.PartiQLValueType.INT32
import org.partiql.value.PartiQLValueType.INT64
import org.partiql.value.PartiQLValueType.INT8
import org.partiql.value.PartiQLValueType.INTERVAL
import org.partiql.value.PartiQLValueType.LIST
import org.partiql.value.PartiQLValueType.MISSING
import org.partiql.value.PartiQLValueType.NULL
import org.partiql.value.PartiQLValueType.SEXP
import org.partiql.value.PartiQLValueType.STRING
import org.partiql.value.PartiQLValueType.STRUCT
import org.partiql.value.PartiQLValueType.SYMBOL
import org.partiql.value.PartiQLValueType.TIME
import org.partiql.value.PartiQLValueType.TIMESTAMP

/**
 * Fn arguments list. The planner is responsible for mapping arguments to parameters.
 */
@OptIn(FnExperimental::class)
internal typealias Args = List<FnParameter>

/**
 * Parameter mapping list tells the planner where to insert implicit casts. Null is the identity.
 */
@OptIn(FnExperimental::class)
internal typealias Mapping = List<FnSignature.Scalar?>

/**
 * Tells us which function matched, and how the arguments are mapped.
 */
@OptIn(FnExperimental::class)
internal class Match<T : FnSignature>(
    public val signature: T,
    public val mapping: Mapping,
)

/**
 * PartiQL type relationships and casts.
 */
private val casts = TypeCasts.partiql()

/**
 * Logic for matching signatures to arguments — this class contains all cast/coercion logic. In my opinion, casts
 * and coercions should come along with the type lattice. Considering we don't really have this, it is simple enough
 * at the moment to keep that information (derived from the current TypeLattice) with the [FnResolver].
 */
@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal class FnResolver(
    catalog: ConnectorMetadata,
    session: PartiQLPlanner.Session,
) : PathResolver<List<FnSignature>>(catalog, session) {

    override fun get(metadata: ConnectorMetadata, path: BindingPath): List<FnSignature> =
        metadata.getFunctions(path).map {
            it.entity.getType()
        }

    internal fun resolveFn(fn: Fn.Unresolved, args: List<Rex>): FnMatch<FnSignature.Scalar> {

        val path = fn.identifier.toBindingPath()
        val entity = resolve(path)
            ?: return FnMatch.Error(
                identifier = fn.identifier,
                args = args,
                candidates = emptyList(),
            )
        val candidates = entity.metadata
            .filterIsInstance<FnSignature.Scalar>()
            .sortedWith(fnPrecedence)

        var canReturnMissing = false
        val parameterPermutations = buildArgumentPermutations(args.map { it.type }).mapNotNull { argList ->
            argList.mapIndexed { i, arg ->
                if (arg.isMissable()) {
                    canReturnMissing = true
                }
                // Skip over if we cannot convert type to runtime type.
                val argType = arg.toRuntimeTypeOrNull() ?: return@mapNotNull null
                FnParameter("arg-$i", argType)
            }
        }
        val potentialFns = parameterPermutations.mapNotNull { parameters ->
            when (val match = match(candidates, parameters)) {
                null -> {
                    canReturnMissing = true
                    null
                }
                else -> {
                    val isMissable = canReturnMissing || casts.isUnsafeCast(match.signature.specific)
                    FnMatch.Ok(match.signature, match.mapping, isMissable)
                }
            }
        }
        // Remove duplicates while maintaining order (precedence).
        val orderedUniqueFns = potentialFns.toSet().toList()
        return when (orderedUniqueFns.size) {
            0 -> FnMatch.Error(fn.identifier, args, candidates)
            1 -> orderedUniqueFns.first()
            else -> FnMatch.Dynamic(orderedUniqueFns, canReturnMissing)
        }
    }

    internal fun resolveAgg(agg: Agg.Unresolved, args: List<Rex>): FnMatch<FnSignature.Aggregation> {

        val path = agg.identifier.toBindingPath()
        val entity = resolve(path)
            ?: return FnMatch.Error(
                identifier = agg.identifier,
                args = args,
                candidates = emptyList(),
            )
        val candidates = entity.metadata
            .filterIsInstance<FnSignature.Aggregation>()
            .sortedWith(fnPrecedence)

        var hadMissingArg = false
        val parameters = args.mapIndexed { i, arg ->
            if (!hadMissingArg && arg.type.isMissable()) {
                hadMissingArg = true
            }
            FnParameter("arg-$i", arg.type.toRuntimeType())
        }
        val match = match(candidates, parameters)
        return when (match) {
            null -> FnMatch.Error(agg.identifier, args, candidates)
            else -> {
                val isMissable = hadMissingArg || casts.isUnsafeCast(match.signature.specific)
                FnMatch.Ok(match.signature, match.mapping, isMissable)
            }
        }
    }

    /**
     * Fns are sorted by precedence (which is not rigorously defined/specified at the moment).
     */
    private fun <T : FnSignature> match(signatures: List<T>, args: Args): Match<T>? {
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
    private fun match(signature: FnSignature, args: Args): Mapping? {
        if (signature.parameters.size != args.size) {
            return null
        }
        val mapping = ArrayList<FnSignature.Scalar?>(args.size)
        for (i in args.indices) {
            val a = args[i]
            val p = signature.parameters[i]
            when {
                // 1. Exact match
                a.type == p.type -> mapping.add(null)
                // 2. Match ANY, no coercion needed
                p.type == ANY -> mapping.add(null)
                // 3. Match NULL argument
                a.type == NULL -> mapping.add(null)
                // 4. Check for a coercion
                else -> {
                    when (val coercion = casts.lookupCoercion(a.type, p.type)) {
                        null -> return null // short-circuit
                        else -> mapping.add(coercion)
                    }
                }
            }
        }
        // if all elements requires casting, then no match
        // because there must be another function definition that requires no casting
        return if (mapping.isEmpty() || mapping.contains(null)) {
            // we made a match
            mapping
        } else {
            null
        }
    }

    private fun buildArgumentPermutations(args: List<StaticType>): List<List<StaticType>> {
        val flattenedArgs = args.map { it.flatten().allTypes }
        return buildArgumentPermutations(flattenedArgs, accumulator = emptyList())
    }

    private fun buildArgumentPermutations(
        args: List<List<StaticType>>,
        accumulator: List<StaticType>,
    ): List<List<StaticType>> {
        if (args.isEmpty()) {
            return listOf(accumulator)
        }
        val first = args.first()
        val rest = when (args.size) {
            1 -> emptyList()
            else -> args.subList(1, args.size)
        }
        return buildList {
            first.forEach { argSubType ->
                addAll(buildArgumentPermutations(rest, accumulator + listOf(argSubType)))
            }
        }
    }

    private fun Identifier.toBindingPath() = when (this) {
        is Identifier.Qualified -> this.toBindingPath()
        is Identifier.Symbol -> BindingPath(listOf(this.toBindingName()))
    }

    private fun Identifier.Qualified.toBindingPath() = BindingPath(steps = listOf(this.root.toBindingName()) + steps.map { it.toBindingName() })

    private fun Identifier.Symbol.toBindingName() = BindingName(
        name = symbol,
        case = when (caseSensitivity) {
            Identifier.CaseSensitivity.SENSITIVE -> BindingCase.SENSITIVE
            Identifier.CaseSensitivity.INSENSITIVE -> BindingCase.INSENSITIVE
        }
    )

    companion object {

        // ====================================
        //  SORTING
        // ====================================

        // Fn precedence comparator
        // 1. Fewest args first
        // 2. Parameters are compared left-to-right
        @JvmStatic
        private val fnPrecedence = Comparator<FnSignature> { fn1, fn2 ->
            // Compare number of arguments
            if (fn1.parameters.size != fn2.parameters.size) {
                return@Comparator fn1.parameters.size - fn2.parameters.size
            }
            // Compare operand type precedence
            for (i in fn1.parameters.indices) {
                val p1 = fn1.parameters[i]
                val p2 = fn2.parameters[i]
                val comparison = p1.compareTo(p2)
                if (comparison != 0) return@Comparator comparison
            }
            // unreachable?
            0
        }

        private fun FnParameter.compareTo(other: FnParameter): Int =
            comparePrecedence(this.type, other.type)

        private fun comparePrecedence(t1: PartiQLValueType, t2: PartiQLValueType): Int {
            if (t1 == t2) return 0
            val p1 = precedence[t1]!!
            val p2 = precedence[t2]!!
            return p1 - p2
        }

        // This simply describes some precedence for ordering functions.
        // This is not explicitly defined in the PartiQL Specification!!
        // This does not imply the ability to CAST; this defines function resolution behavior.
        private val precedence: Map<PartiQLValueType, Int> = listOf(
            NULL,
            MISSING,
            BOOL,
            INT8,
            INT16,
            INT32,
            INT64,
            INT,
            DECIMAL,
            FLOAT32,
            FLOAT64,
            DECIMAL_ARBITRARY, // Arbitrary precision decimal has a higher precedence than FLOAT
            CHAR,
            STRING,
            CLOB,
            SYMBOL,
            BINARY,
            BYTE,
            BLOB,
            DATE,
            TIME,
            TIMESTAMP,
            INTERVAL,
            LIST,
            SEXP,
            BAG,
            STRUCT,
            ANY,
        ).mapIndexed { precedence, type -> type to precedence }.toMap()
    }
}
