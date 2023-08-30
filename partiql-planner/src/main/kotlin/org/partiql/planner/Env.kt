package org.partiql.planner

import org.partiql.plan.Fn
import org.partiql.plan.Global
import org.partiql.plan.Identifier
import org.partiql.plan.Plan
import org.partiql.plan.Rel
import org.partiql.plan.Rex
import org.partiql.planner.typer.FunctionResolver
import org.partiql.planner.typer.Mapping
import org.partiql.planner.typer.toRuntimeType
import org.partiql.spi.BindingCase
import org.partiql.spi.BindingName
import org.partiql.spi.BindingPath
import org.partiql.spi.Plugin
import org.partiql.spi.connector.Connector
import org.partiql.spi.connector.ConnectorMetadata
import org.partiql.spi.connector.ConnectorObjectHandle
import org.partiql.spi.connector.ConnectorObjectPath
import org.partiql.spi.connector.ConnectorSession
import org.partiql.spi.connector.Constants
import org.partiql.types.StaticType
import org.partiql.types.StructType
import org.partiql.types.TypingMode
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValueExperimental

/**
 * Handle for associating a catalog with the metadata; pair of catalog to data.
 */
internal typealias Handle<T> = Pair<String, T>

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
 * Result of attempting to match an unresolved function.
 */
internal sealed class FnMatch {
    public class Ok(
        public val signature: FunctionSignature,
        public val mapping: Mapping,
    ) : FnMatch()

    public class Error(
        public val fn: Fn.Unresolved,
        public val args: List<Rex>,
        public val candidates: List<FunctionSignature>,
    ) : FnMatch()
}

/**
 * Metadata regarding a resolved variable.
 */
internal sealed interface ResolvedVar {

    public val type: StaticType
    public val ordinal: Int

    /**
     * Metadata for a resolved local variable.
     *
     * @property type       Resolved StaticType
     * @property ordinal    Index offset in [TypeEnv]
     * @property tail       Remaining part of path (if any)
     */
    class Local(
        override val type: StaticType,
        override val ordinal: Int,
        val rootType: StaticType,
        val tail: List<BindingName>,
    ) : ResolvedVar

    /**
     * Metadata for a resolved global variable
     *
     * @property type       Resolved StaticType
     * @property ordinal    Index offset in the environment `globals` list
     * @property depth      The depth/level of the path match.
     */
    class Global(
        override val type: StaticType,
        override val ordinal: Int,
        val depth: Int,
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
 * @property header         List of namespaced definitions
 * @property plugins        List of plugins for global resolution
 * @property session        Session details
 */
@OptIn(PartiQLValueExperimental::class)
internal class Env(
    private val header: Header,
    private val mode: TypingMode,
    private val plugins: List<Plugin>,
    private val session: PartiQLPlanner.Session,
) {

    /**
     * Collect the list of all referenced globals during planning.
     */
    public val globals = mutableListOf<Global>()

    /**
     * Encapsulate function matching logic in
     */
    public val functionResolver = FunctionResolver(header)

    private val connectorSession = object : ConnectorSession {
        override fun getQueryId(): String = session.queryId
        override fun getUserId(): String = session.userId
    }

    /**
     * Map of catalog names to its underlying connector
     */
    private val catalogs: Map<String, Connector>

    // Initialize connectors
    init {
        val catalogs = mutableMapOf<String, Connector>()
        val connectors = plugins.flatMap { it.getConnectorFactories() }
        // map catalogs to connectors
        for ((catalog, config) in session.catalogConfig) {
            // find corresponding connector
            val connectorName = config[Constants.CONFIG_KEY_CONNECTOR_NAME].stringValue
            val connector = connectors.first { it.getName() == connectorName }
            // initialize connector with given config
            catalogs[catalog] = connector.create(catalog, config)
        }
        this.catalogs = catalogs.toMap()
    }

    /**
     * Leverages a [FunctionResolver] to find a matching function defined in the [Header].
     */
    internal fun resolveFn(fn: Fn.Unresolved, args: List<Rex>): FnMatch {
        val candidates = header.lookup(fn)
        val parameters = args.mapIndexed { i, arg ->
            FunctionParameter("arg-$i", arg.type.toRuntimeType())
        }
        val match = functionResolver.match(candidates, parameters)
        return when (match) {
            null -> FnMatch.Error(fn, args, candidates)
            else -> FnMatch.Ok(match.signature, match.mapping)
        }
    }

    /**
     * TODO
     */
    internal fun getFnAggHandle(identifier: Identifier): Nothing = TODO()

    /**
     * Fetch global object metadata from the given [BindingPath].
     *
     * @param catalog   Current catalog
     * @param path      Global identifier path
     * @return
     */
    internal fun getObjectHandle(catalog: BindingName, path: BindingPath): Handle<ConnectorObjectHandle>? {
        val metadata = getMetadata(catalog) ?: return null
        return metadata.second.getObjectHandle(connectorSession, path)?.let {
            metadata.first to it
        }
    }

    /**
     * Fetch a global variable's StaticType given its handle.
     *
     * @param handle
     * @return
     */
    internal fun getObjectDescriptor(handle: Handle<ConnectorObjectHandle>): StaticType {
        val metadata = getMetadata(BindingName(handle.first, BindingCase.SENSITIVE))!!.second
        return metadata.getObjectType(connectorSession, handle.second)!!
    }

    /**
     * Fetch [ConnectorMetadata] given a catalog name.
     *
     * @param catalogName
     * @return
     */
    private fun getMetadata(catalogName: BindingName): Handle<ConnectorMetadata>? {
        val catalogKey = catalogs.keys.firstOrNull { catalogName.isEquivalentTo(it) } ?: return null
        val connector = catalogs[catalogKey] ?: return null
        val metadata = connector.getMetadata(connectorSession)
        return catalogKey to metadata
    }

    /**
     * TODO
     *
     * @param catalog
     * @param originalPath
     * @param catalogPath
     * @return
     */
    private fun getGlobalType(
        catalog: BindingName?,
        originalPath: BindingPath,
        catalogPath: BindingPath,
    ): ResolvedVar? {
        return catalog?.let { cat ->
            getObjectHandle(cat, catalogPath)?.let { handle ->
                getObjectDescriptor(handle).let { type ->
                    val depth = calculateMatched(originalPath, catalogPath, handle.second.absolutePath)
                    // TODO check known globals before calling out to connector again
                    // Append this to the global list
                    val global = Plan.global(originalPath.toIdentifier(), type)
                    globals.add(global)
                    // Return resolution metadata
                    ResolvedVar.Global(type, globals.size - 1, depth)
                }
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
     * TODO: Add global bindings
     * TODO: Replace paths with global variable references if found
     */
    private fun resolveGlobalBind(path: BindingPath): ResolvedVar? {
        val currentCatalog = session.currentCatalog?.let { BindingName(it, BindingCase.SENSITIVE) }
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
     * Check locals, else search structs.
     */
    private fun resolveLocalBind(path: BindingPath, locals: List<Rel.Binding>): ResolvedVar? {
        if (path.steps.isEmpty()) {
            return null
        }

        // 1. Check locals for root
        locals.forEachIndexed { ordinal, binding ->
            val root = path.steps[0]
            val tail = path.steps.drop(1)
            if (root.isEquivalentTo(binding.name)) {
                return ResolvedVar.Local(binding.type, ordinal, binding.type, tail)
            }
        }

        // 2. Check if this variable is referencing a struct field, carrying ordinals
        val matches = mutableListOf<ResolvedVar.Local>()
        for (ordinal in locals.indices) {
            val rootType = locals[ordinal].type
            if (rootType is StructType) {
                val varType = inferStructLookup(rootType, path)
                if (varType != null) {
                    // we found this path within a struct!
                    val match = ResolvedVar.Local(varType, ordinal, rootType, path.steps)
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
     */
    internal fun inferStructLookup(struct: StructType, path: BindingPath): StaticType? {
        var curr: StaticType = struct
        for (step in path.steps) {
            if (curr !is StructType) {
                // cannot navigate into non-tuple
                return null
            }
            // 1. Assume ORDERED for now
            // 2. Assume our spec is implying all struct navigation is case-sensitive
            val field = curr.fields.firstOrNull { it.key == step.name } ?: return null
            curr = field.value
        }
        // Lookup final field
        return curr
    }

    /**
     * Logic for determining how many BindingNames were “matched” by the ConnectorMetadata
     * 1. Matched = RelativePath - Not Found
     * 2. Not Found = Input CatalogPath - Output CatalogPath
     * 3. Matched = RelativePath - (Input CatalogPath - Output CatalogPath)
     * 4. Matched = RelativePath + Output CatalogPath - Input CatalogPath
     */
    private fun calculateMatched(
        originalPath: BindingPath,
        inputCatalogPath: BindingPath,
        outputCatalogPath: ConnectorObjectPath,
    ): Int {
        return originalPath.steps.size + outputCatalogPath.steps.size - inputCatalogPath.steps.size
    }

    private fun BindingPath.toIdentifier() = Plan.identifierQualified(
        root = steps[0].toIdentifier(),
        steps = steps.subList(1, steps.size).map { it.toIdentifier() }
    )

    private fun BindingName.toIdentifier() = Plan.identifierSymbol(
        symbol = name,
        caseSensitivity = when (bindingCase) {
            BindingCase.SENSITIVE -> Identifier.CaseSensitivity.SENSITIVE
            BindingCase.INSENSITIVE -> Identifier.CaseSensitivity.INSENSITIVE
        }
    )
}
