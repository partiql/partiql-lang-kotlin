package org.partiql.plan

/**
 * Logical EXCLUDE step, one of: index, key, symbol, struct wildcard, or collection wildcard.
 *
 * TODO does not need to be an interface.
 */
public interface ExcludeStep {

    public fun getSubsteps(): Collection<ExcludeStep>

    public companion object {

        @JvmStatic
        public fun index(index: Int, substeps: List<ExcludeStep> = emptyList()): ExcludeIndex =
            ExcludeIndexImpl(index, substeps)

        @JvmStatic
        public fun key(key: String, substeps: List<ExcludeStep> = emptyList()): ExcludeKey =
            ExcludeKeyImpl(key, substeps)

        @JvmStatic
        public fun symbol(symbol: String, substeps: List<ExcludeStep> = emptyList()): ExcludeSymbol =
            ExcludeSymbolImpl(symbol, substeps)

        @JvmStatic
        public fun struct(substeps: List<ExcludeStep> = emptyList()): ExcludeStructWildcard =
            ExcludeStructWildcardImpl(substeps)

        @JvmStatic
        public fun collection(substeps: List<ExcludeStep> = emptyList()): ExcludeCollectionWildcard =
            ExcludeCollectionWildcardImpl(substeps)
    }
}

/**
 * Logical representation of an EXCLUDE path index step.
 */
public interface ExcludeIndex : ExcludeStep {
    public fun getIndex(): Int
}

private data class ExcludeIndexImpl(
    private val index: Int,
    private val substeps: List<ExcludeStep> = emptyList(),
) : ExcludeIndex {
    override fun getSubsteps(): Collection<ExcludeStep> = substeps
    override fun getIndex(): Int = index
}

/**
 * Logical representation of an EXCLUDE path key step.
 */
public interface ExcludeKey : ExcludeStep {
    public fun getKey(): String
}

// TODO hashcode/equals without data class
private data class ExcludeKeyImpl(
    private val key: String,
    private val substeps: List<ExcludeStep> = emptyList(),
) : ExcludeKey {
    override fun getSubsteps(): Collection<ExcludeStep> = substeps
    override fun getKey(): String = key
}

/**
 * Logical representation of an EXCLUDE path symbol step.
 */
public interface ExcludeSymbol : ExcludeStep {
    public fun getSymbol(): String
}

// TODO hashcode/equals without data class
private data class ExcludeSymbolImpl(
    private val symbol: String,
    private val substeps: List<ExcludeStep> = emptyList(),
) : ExcludeSymbol {
    override fun getSubsteps(): Collection<ExcludeStep> = substeps
    override fun getSymbol(): String = symbol
}

/**
 * Logical representation of an EXCLUDE struct wildcard step.
 */
public interface ExcludeStructWildcard : ExcludeStep

// TODO hashcode/equals without data class
private data class ExcludeStructWildcardImpl(
    private val substeps: List<ExcludeStep> = emptyList(),
) : ExcludeStructWildcard {
    override fun getSubsteps(): Collection<ExcludeStep> = substeps
}

/**
 * Logical representation of an EXCLUDE collection wildcard step.
 */
public interface ExcludeCollectionWildcard : ExcludeStep

// TODO hashcode/equals without data class
private data class ExcludeCollectionWildcardImpl(
    private val substeps: List<ExcludeStep> = emptyList(),
) : ExcludeCollectionWildcard {
    override fun getSubsteps(): Collection<ExcludeStep> = substeps
}
