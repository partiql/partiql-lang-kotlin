package org.partiql.planner.internal

import org.partiql.spi.BindingName
import org.partiql.types.StaticType

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
     * @property ordinal           Index offset in [TypeEnvLocal]
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
