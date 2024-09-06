package org.partiql.plan.v1.operator.rel

/**
 * Logical EXCLUDE step, one of: index, key, symbol, struct wildcard, or collection wildcard.
 */
public interface RelExcludeStep {

    public fun getSubsteps(): Collection<RelExcludeStep>

    companion object {

        @JvmStatic
        public fun index(index: Int, substeps: List<RelExcludeStep> = emptyList()): RelExcludeIndex =
            RelExcludeIndexImpl(index, substeps)

        @JvmStatic
        public fun key(key: String, substeps: List<RelExcludeStep> = emptyList()): RelExcludeKey =
            RelExcludeKeyImpl(key, substeps)

        @JvmStatic
        public fun symbol(symbol: String, substeps: List<RelExcludeStep> = emptyList()): RelExcludeSymbol =
            RelExcludeSymbolImpl(symbol, substeps)

        @JvmStatic
        public fun struct(substeps: List<RelExcludeStep> = emptyList()): RelExcludeStructWildcard =
            RelExcludeStructWildcardImpl(substeps)

        @JvmStatic
        public fun collection(substeps: List<RelExcludeStep> = emptyList()): RelExcludeCollectionWildcard =
            RelExcludeCollectionWildcardImpl(substeps)
    }
}

/**
 * Logical representation of an EXCLUDE path index step.
 */
public interface RelExcludeIndex : RelExcludeStep {
    public fun getIndex(): Int
}

private data class RelExcludeIndexImpl(
    private val index: Int,
    private val substeps: List<RelExcludeStep> = emptyList(),
) : RelExcludeIndex {
    override fun getSubsteps(): Collection<RelExcludeStep> = substeps
    override fun getIndex(): Int = index
}

/**
 * Logical representation of an EXCLUDE path key step.
 */
public interface RelExcludeKey : RelExcludeStep {
    public fun getKey(): String
}

// TODO hashcode/equals without data class
private data class RelExcludeKeyImpl(
    private val key: String,
    private val substeps: List<RelExcludeStep> = emptyList(),
) : RelExcludeKey {
    override fun getSubsteps(): Collection<RelExcludeStep> = substeps
    override fun getKey(): String = key
}

/**
 * Logical representation of an EXCLUDE path symbol step.
 */
public interface RelExcludeSymbol : RelExcludeStep {
    public fun getSymbol(): String
}

// TODO hashcode/equals without data class
private data class RelExcludeSymbolImpl(
    private val symbol: String,
    private val substeps: List<RelExcludeStep> = emptyList(),
) : RelExcludeSymbol {
    override fun getSubsteps(): Collection<RelExcludeStep> = substeps
    override fun getSymbol(): String = symbol
}

/**
 * Logical representation of an EXCLUDE struct wildcard step.
 */
public interface RelExcludeStructWildcard : RelExcludeStep

// TODO hashcode/equals without data class
private data class RelExcludeStructWildcardImpl(
    private val substeps: List<RelExcludeStep> = emptyList(),
) : RelExcludeStructWildcard {
    override fun getSubsteps(): Collection<RelExcludeStep> = substeps
}

/**
 * Logical representation of an EXCLUDE collection wildcard step.
 */
public interface RelExcludeCollectionWildcard : RelExcludeStep

// TODO hashcode/equals without data class
private data class RelExcludeCollectionWildcardImpl(
    private val substeps: List<RelExcludeStep> = emptyList(),
) : RelExcludeCollectionWildcard {
    override fun getSubsteps(): Collection<RelExcludeStep> = substeps
}
