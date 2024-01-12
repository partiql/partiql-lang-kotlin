package org.partiql.planner.internal

import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.internal.ir.Agg
import org.partiql.planner.internal.ir.Catalog
import org.partiql.planner.internal.ir.Fn
import org.partiql.planner.internal.ir.Rel
import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.typer.FnResolver
import org.partiql.spi.BindingCase
import org.partiql.spi.BindingName
import org.partiql.spi.BindingPath
import org.partiql.spi.fn.FnExperimental
import org.partiql.types.StaticType
import org.partiql.types.StructType
import org.partiql.types.TupleConstraint

/**
 * TypeEnv represents the environment in which we type expressions and resolve variables while planning.
 *
 * TODO TypeEnv should be a stack of locals; also the strategy has been kept here because it's easier to
 *  pass through the traversal like this, but is conceptually odd to associate with the TypeEnv.
 * @property schema
 * @property strategy
 */
internal class TypeEnv(
    val schema: List<Rel.Binding>,
    val strategy: ResolutionStrategy,
) {

    /**
     * Return a copy with GLOBAL lookup strategy
     */
    fun global() = TypeEnv(schema, ResolutionStrategy.GLOBAL)

    /**
     * Return a copy with LOCAL lookup strategy
     */
    fun local() = TypeEnv(schema, ResolutionStrategy.LOCAL)

    /**
     * Debug string
     */
    override fun toString() = buildString {
        append("(")
        append("strategy=$strategy")
        append(", ")
        val bindings = "< " + schema.joinToString { "${it.name}: ${it.type}" } + " >"
        append("bindings=$bindings")
        append(")")
    }
}

/**
 * Metadata regarding a resolved variable.
 *
 * @property depth      The depth/level of the path match.
 */
internal sealed interface ResolvedVar {

    public val type: StaticType
    public val ordinal: Int
    public val depth: Int

    /**
     * Metadata for a resolved local variable.
     *
     * @property type              Resolved StaticType
     * @property ordinal           Index offset in [TypeEnv]
     * @property resolvedSteps     The fully resolved path steps.s
     */
    class Local(
        override val type: StaticType,
        override val ordinal: Int,
        val rootType: StaticType,
        val resolvedSteps: List<BindingName>,
    ) : ResolvedVar {
        // the depth are always going to be 1 because this is local variable.
        // the global path, however the path length maybe, going to be replaced by a binding name.
        override val depth: Int = 1
    }

    /**
     * Metadata for a resolved global variable
     *
     * @property type       Resolved StaticType
     * @property ordinal    The relevant catalog's index offset in the [Env.symbols] list
     * @property depth      The depth/level of the path match.
     * @property position   The relevant value's index offset in the [Catalog.values] list
     */
    class Global(
        override val type: StaticType,
        override val ordinal: Int,
        override val depth: Int,
        val position: Int,
    ) : ResolvedVar
}

/**
 * Variable resolution strategies — https://partiql.org/assets/PartiQL-Specification.pdf#page=35
 *
 * | Value      | Strategy              | Scoping Rules |
 * |------------+-----------------------+---------------|
 * | LOCAL      | local-first lookup    | Rules 1, 2    |
 * | GLOBAL     | global-first lookup   | Rule 3        |
 */
internal enum class ResolutionStrategy {
    LOCAL,
    GLOBAL,
}

/**
 * PartiQL Planner Global Environment of Catalogs backed by given plugins.
 *
 * @property session        Session details
 */
@OptIn(FnExperimental::class)
internal class Env(
    private val session: PartiQLPlanner.Session,
) {

    /**
     * Collect the list of all referenced catalog symbols during planning.
     */
    public val symbols = mutableListOf<Catalog>()

    /**
     * ConnectorMetadata for this query session.
     */
    private val catalogs = session.catalogs

    /**
     * Function resolution logic.
     */
    private val fnResolver = FnResolver(session, catalogs)

    /**
     * Leverages a [FnResolver] to find a matching function defined in the [Header] scalar function catalog.
     */
    internal fun resolveFn(fn: Fn.Unresolved, args: List<Rex>) = fnResolver.resolveFn(fn, args)

    /**
     * Leverages a [FnResolver] to find a matching function defined in the [Header] aggregation function catalog.
     */
    internal fun resolveAgg(agg: Agg.Unresolved, args: List<Rex>) = fnResolver.resolveAgg(agg, args)

    /**
     * Lookup a global using a [BindingName] as the catalog name.
     *
     * @param catalog
     * @param originalPath
     * @param catalogPath
     * @return
     */
    private fun getGlobalType(
        catalog: BindingName,
        originalPath: BindingPath,
        catalogPath: BindingPath,
    ) : ResolvedVar? {
        val cat = catalogs.keys.firstOrNull { catalog.matches(it) } ?: return null
        return getGlobalType(cat, originalPath, catalogPath)
    }

    /**
     * TODO optimization, check known globals before calling out to connector again
     *
     * @param catalog
     * @param originalPath
     * @param catalogPath
     * @return
     */
    private fun getGlobalType(
        catalog: String,
        originalPath: BindingPath,
        catalogPath: BindingPath,
    ): ResolvedVar? {
        return catalogs[catalog]?.let { metadata ->
            metadata.getObject(catalogPath)?.let { obj ->
                obj.entity.getType()?.let { type ->
                    val depth = calculateMatched(originalPath, catalogPath, obj.path)
                    val (catalogIndex, valueIndex) = getOrAddCatalogValue(catalog, obj.path, type)
                    // Return resolution metadata
                    ResolvedVar.Global(type, catalogIndex, depth, valueIndex)
                }
            }
        }
    }

    /**
     * @return a [Pair] where [Pair.first] is the catalog index and [Pair.second] is the value index within that catalog
     */
    private fun getOrAddCatalogValue(
        catalogName: String,
        valuePath: List<String>,
        valueType: StaticType,
    ): Pair<Int, Int> {
        val catalogIndex = getOrAddCatalog(catalogName)
        val symbols = symbols[catalogIndex].symbols
        return symbols.indexOfFirst { value ->
            value.path == valuePath
        }.let { index ->
            when (index) {
                -1 -> {
                    this.symbols[catalogIndex] = this.symbols[catalogIndex].copy(
                        symbols = symbols + listOf(Catalog.Symbol(valuePath, valueType))
                    )
                    catalogIndex to this.symbols[catalogIndex].symbols.lastIndex
                }
                else -> {
                    catalogIndex to index
                }
            }
        }
    }

    private fun getOrAddCatalog(catalogName: String): Int {
        return symbols.indexOfFirst { catalog ->
            catalog.name == catalogName
        }.let {
            when (it) {
                -1 -> {
                    symbols.add(Catalog(catalogName, emptyList()))
                    symbols.lastIndex
                }
                else -> it
            }
        }
    }

    /**
     * Attempt to resolve a [BindingPath] in the global + local type environments.
     */
    fun resolve(path: BindingPath, locals: TypeEnv, scope: Rex.Op.Var.Scope): ResolvedVar? {
        val strategy = when (scope) {
            Rex.Op.Var.Scope.DEFAULT -> locals.strategy
            Rex.Op.Var.Scope.LOCAL -> ResolutionStrategy.LOCAL
        }
        return when (strategy) {
            ResolutionStrategy.LOCAL -> {
                var type: ResolvedVar? = null
                type = type ?: resolveLocalBind(path, locals.schema)
                type = type ?: resolveGlobalBind(path)
                type
            }
            ResolutionStrategy.GLOBAL -> {
                var type: ResolvedVar? = null
                type = type ?: resolveGlobalBind(path)
                type = type ?: resolveLocalBind(path, locals.schema)
                type
            }
        }
    }

    /**
     * Logic is as follows:
     * 1. If Current Catalog and Schema are set, create a Path to the object and attempt to grab handle and schema.
     *   a. If not found, just try to find the object in the catalog.
     * 2. If Current Catalog is not set:
     *   a. Loop through all catalogs and try to find the object.
     *
     * TODO: Replace paths with global variable references if found
     */
    private fun resolveGlobalBind(path: BindingPath): ResolvedVar? {
        val currentCatalog = session.currentCatalog
        val currentCatalogPath = BindingPath(session.currentDirectory.map { BindingName(it, BindingCase.SENSITIVE) })
        val absoluteCatalogPath = BindingPath(currentCatalogPath.steps + path.steps)
        val resolvedVar = when (path.steps.size) {
            0 -> null
            1 -> getGlobalType(currentCatalog, path, absoluteCatalogPath)
            2 -> getGlobalType(currentCatalog, path, path) ?: getGlobalType(currentCatalog, path, absoluteCatalogPath)
            else -> {
                val inferredCatalog = path.steps[0]
                val newPath = BindingPath(path.steps.subList(1, path.steps.size))
                getGlobalType(inferredCatalog, path, newPath)
                    ?: getGlobalType(currentCatalog, path, path)
                    ?: getGlobalType(currentCatalog, path, absoluteCatalogPath)
            }
        }
        return resolvedVar
    }

    /**
     * Check locals, else search within structs.
     */
    internal fun resolveLocalBind(path: BindingPath, locals: List<Rel.Binding>): ResolvedVar? {
        if (path.steps.isEmpty()) {
            return null
        }

        // 1. Check locals for root
        locals.forEachIndexed { ordinal, binding ->
            val root = path.steps[0]
            if (root.matches(binding.name)) {
                return ResolvedVar.Local(binding.type, ordinal, binding.type, path.steps)
            }
        }

        // 2. Check if this variable is referencing a struct field, carrying ordinals
        val matches = mutableListOf<ResolvedVar.Local>()
        for (ordinal in locals.indices) {
            val rootType = locals[ordinal].type
            val pathPrefix = BindingName(locals[ordinal].name, BindingCase.SENSITIVE)
            if (rootType is StructType) {
                val varType = inferStructLookup(rootType, path)
                if (varType != null) {
                    // we found this path within a struct!
                    val match = ResolvedVar.Local(
                        varType.resolvedType,
                        ordinal,
                        rootType,
                        listOf(pathPrefix) + varType.replacementPath.steps,
                    )
                    matches.add(match)
                }
            }
        }

        // 0 -> no match
        // 1 -> resolved
        // N -> ambiguous
        return when (matches.size) {
            0 -> null
            1 -> matches.single()
            else -> null // TODO emit ambiguous error
        }
    }

    /**
     * Searches for the path within the given struct, returning null if not found.
     *
     * @return a [ResolvedPath] that contains the disambiguated [ResolvedPath.replacementPath] and the path's
     * [StaticType]. Returns NULL if unable to find the [path] given the [struct].
     */
    private fun inferStructLookup(struct: StructType, path: BindingPath): ResolvedPath? {
        var curr: StaticType = struct
        val replacementSteps = path.steps.map { step ->
            // Assume ORDERED for now
            val currentStruct = curr as? StructType ?: return null
            val (replacement, stepType) = inferStructLookup(currentStruct, step) ?: return null
            curr = stepType
            replacement
        }
        // Lookup final field
        return ResolvedPath(
            BindingPath(replacementSteps),
            curr
        )
    }

    /**
     * Represents a disambiguated [BindingPath] and its inferred [StaticType].
     */
    private class ResolvedPath(
        val replacementPath: BindingPath,
        val resolvedType: StaticType,
    )

    /**
     * @return a disambiguated [key] and the resulting [StaticType].
     */
    private fun inferStructLookup(struct: StructType, key: BindingName): Pair<BindingName, StaticType>? {
        val isClosed = struct.constraints.contains(TupleConstraint.Open(false))
        val isOrdered = struct.constraints.contains(TupleConstraint.Ordered)
        return when {
            // 1. Struct is closed and ordered
            isClosed && isOrdered -> {
                struct.fields.firstOrNull { entry -> key.matches(entry.key) }?.let {
                    (sensitive(it.key) to it.value)
                }
            }
            // 2. Struct is closed
            isClosed -> {
                val matches = struct.fields.filter { entry -> key.matches(entry.key) }
                when (matches.size) {
                    0 -> null
                    1 -> matches.first().let { (sensitive(it.key) to it.value) }
                    else -> {
                        val firstKey = matches.first().key
                        val sharedKey = when (matches.all { it.key == firstKey }) {
                            true -> sensitive(firstKey)
                            false -> key
                        }
                        sharedKey to StaticType.unionOf(matches.map { it.value }.toSet()).flatten()
                    }
                }
            }
            // 3. Struct is open
            else -> key to StaticType.ANY
        }
    }

    private fun sensitive(str: String): BindingName = BindingName(str, BindingCase.SENSITIVE)

    /**
     * Logic for determining how many BindingNames were “matched” by the ConnectorMetadata
     *
     * 1. Matched = RelativePath - Not Found
     * 2. Not Found = Input CatalogPath - Output CatalogPath
     * 3. Matched = RelativePath - (Input CatalogPath - Output CatalogPath)
     * 4. Matched = RelativePath + Output CatalogPath - Input CatalogPath
     */
    private fun calculateMatched(
        originalPath: BindingPath,
        inputCatalogPath: BindingPath,
        outputCatalogPath: List<String>,
    ): Int {
        return originalPath.steps.size + outputCatalogPath.size - inputCatalogPath.steps.size
    }
}
