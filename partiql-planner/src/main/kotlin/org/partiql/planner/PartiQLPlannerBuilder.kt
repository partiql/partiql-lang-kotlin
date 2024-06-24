package org.partiql.planner

import org.partiql.planner.internal.PlannerFlag
import org.partiql.planner.internal.SqlPlanner
import org.partiql.planner.metadata.Namespace
import org.partiql.planner.metadata.Routine
import org.partiql.planner.metadata.System
import org.partiql.spi.connector.ConnectorMetadata

/**
 * PartiQLPlannerBuilder is used to programmatically construct a [PartiQLPlanner] implementation.
 *
 * Usage:
 *      PartiQLPlanner.builder()
 *                    .signalMode()
 *                    .addPass(myPass)
 *                    .build()
 */
public class PartiQLPlannerBuilder {

    private val flags: MutableSet<PlannerFlag> = mutableSetOf()

    private val passes: MutableList<PartiQLPlannerPass> = mutableListOf()

    private val system: System = object : System {
        override fun getCatalog(name: String): Namespace? {
            TODO("Not yet implemented")
        }

        override fun listCatalogs(): Collection<String> {
            TODO("Not yet implemented")
        }

        override fun getRoutines(name: String): List<Routine> {
            TODO("Not yet implemented")
        }
    }

    /**
     * Build the builder, return an implementation of a [PartiQLPlanner].
     *
     * @return
     */
    public fun build(): PartiQLPlanner = SqlPlanner(system, flags)

    /**
     * Java style method for adding a planner pass to this planner builder.
     *
     * @param pass
     * @return
     */
    public fun addPass(pass: PartiQLPlannerPass): PartiQLPlannerBuilder = this.apply {
        this.passes.add(pass)
    }

    /**
     * Kotlin style method for adding a list of planner passes to this planner builder.
     *
     * @param passes
     * @return
     */
    public fun addPasses(vararg passes: PartiQLPlannerPass): PartiQLPlannerBuilder = this.apply {
        this.passes.addAll(passes)
    }

    /**
     * Java style method for setting the planner to signal mode
     */
    public fun signalMode(): PartiQLPlannerBuilder = this.apply {
        this.flags.add(PlannerFlag.SIGNAL_MODE)
    }

    /**
     * Java style method for assigning a Catalog name to [ConnectorMetadata].
     *
     * @param catalog
     * @param metadata
     * @return
     */
    @Deprecated("This will be removed in version 1.0", ReplaceWith("Please use org.partiql.planner.PartiQLPlanner.Session"))
    public fun addCatalog(catalog: String, metadata: ConnectorMetadata): PartiQLPlannerBuilder = this

    /**
     * Kotlin style method for assigning Catalog names to [ConnectorMetadata].
     *
     * @param catalogs
     * @return
     */
    @Deprecated("This will be removed in v0.15.0+.", ReplaceWith("Please use org.partiql.planner.PartiQLPlanner.Session"))
    public fun catalogs(vararg catalogs: Pair<String, ConnectorMetadata>): PartiQLPlannerBuilder = this

    @Deprecated("This will be removed in v0.15.0+.", ReplaceWith("addPasses"))
    public fun passes(passes: List<PartiQLPlannerPass>): PartiQLPlannerBuilder = this.apply {
        this.passes.clear()
        this.passes.addAll(passes)
    }
}
