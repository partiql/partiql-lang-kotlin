package org.partiql.planner.internal

import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.internal.ir.Catalog
import org.partiql.planner.internal.ir.Fn
import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.ir.builder.CatalogBuilder
import org.partiql.planner.internal.typer.TypeEnv
import org.partiql.spi.BindingPath
import org.partiql.spi.fn.FnExperimental

/**
 * [Env] represents the combination of the database type environment (db env) and some local env (type env).
 *
 * @property session
 */
internal class Env(private val session: PartiQLPlanner.Session) {

    /**
     * Maintain all catalog items referenced during query planning.
     */
    private val catalogs = mutableListOf<CatalogBuilder>()

    /**
     * Builds and returns the current list of all referenced catalog items.
     *
     * @return
     */
    fun catalogs(): List<Catalog> = catalogs.map { it.build() }

    /**
     *
     *
     * @param path
     * @param locals
     * @param strategy
     * @return
     */
    public fun resolve(path: BindingPath, locals: TypeEnv, strategy: ResolutionStrategy): Rex? = when (strategy) {
        ResolutionStrategy.LOCAL -> locals.resolve(path) ?: catalog.lookup
        ResolutionStrategy.GLOBAL -> global(path) ?: locals.resolve(path)
    }




    // /**
    //  * TODO
    //  *
    //  * @param fn
    //  * @return
    //  */
    // @OptIn(FnExperimental::class)
    // public fun resolve(fn: Fn.Unresolved, args: List<Rex>): Rex? {
    //     TODO()
    // }

}
