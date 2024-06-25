package org.partiql.planner.internal

import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.internal.casts.CastTable
import org.partiql.planner.internal.ir.Identifier
import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.ir.rexOpCastResolved
import org.partiql.planner.internal.typer.CompilerType
import org.partiql.planner.metadata.Namespace
import org.partiql.planner.metadata.Routine

/**
 * [Env] is similar to the database type environment from the PartiQL Specification. This includes resolution of
 * database binding values and scoped functions.
 *
 * See TypeEnv for the variables type environment.
 *
 * TODO: function resolution between scalar functions and aggregations.
 *
 * @property session
 */
internal class Env(
    private val namespace: Namespace,
    private val session: PartiQLPlanner.Session,
) {

    /**
     * Global name resolution logic.
     */
    fun resolve(path: Identifier): Rex? {
        // val item = objects.lookup(path) ?: return null
        // // Create an internal typed reference
        // val ref = refObj(
        //     catalog = item.catalog,
        //     path = item.handle.path.steps,
        //     type = CompilerType(item.handle.entity.getPType()),
        // )
        // // Rewrite as a path expression.
        // val root = rex(ref.type, rexOpVarGlobal(ref))
        // val depth = calculateMatched(path, item.input, ref.path)
        // val tail = path.steps.drop(depth)
        // return if (tail.isEmpty()) root else root.toPath(tail)
        return null
    }

    fun getRoutines(path: Identifier): Collection<Routine> {
        return emptyList()
    }

    fun getCast(input: Rex, target: CompilerType): Rex.Op.Cast.Resolved? {
        val operand = input.type
        val cast = CastTable.partiql.get(operand, target) ?: return null
        return rexOpCastResolved(cast, input)
    }

    // -----------------------
    //  Helpers
    // -----------------------

    /**
     * Logic for determining how many Identifier.Symbols were “matched” by the ConnectorMetadata
     *
     * Assume:
     * - steps_matched = user_input_path_size - path_steps_not_found_size
     * - path_steps_not_found_size = catalog_path_sent_to_spi_size - actual_catalog_absolute_path_size
     *
     * Therefore, we present the equation to [calculateMatched]:
     * - steps_matched = user_input_path_size - (catalog_path_sent_to_spi_size - actual_catalog_absolute_path_size)
     *                 = user_input_path_size + actual_catalog_absolute_path_size - catalog_path_sent_to_spi_size
     *
     * For example:
     *
     * Assume we are in some catalog, C, in some schema, S. There is a tuple, T, with attribute, A1. Assume A1 is of type
     * tuple with an attribute A2.
     * If our query references `T.A1.A2`, we will eventually ask SPI (connector C) for `S.T.A1.A2`. In this scenario:
     * - The original user input was `T.A1.A2` (length 3)
     * - The absolute path returned from SPI will be `S.T` (length 2)
     * - The path we eventually sent to SPI to resolve was `S.T.A1.A2` (length 4)
     *
     * So, we can now use [calculateMatched] to determine how many were actually matched from the user input. Using the
     * equation from above:
     *
     * - steps_matched = len(user input) + len(absolute catalog path) - len(path sent to SPI)
     * = len([userInputPath]) + len([actualAbsolutePath]) - len([pathSentToConnector])
     * = 3 + 2 - 4
     * = 5 - 4
     * = 1
     *
     *
     * Therefore, in this example we have determined that from the original input (`T.A1.A2`) `T` is the value matched in the
     * database environment.
     */
    private fun calculateMatched(
        userInputPath: Identifier.Qualified,
        pathSentToConnector: Identifier.Qualified,
        actualAbsolutePath: List<String>,
    ): Int {
        return userInputPath.steps.size + actualAbsolutePath.size - pathSentToConnector.steps.size
    }
}
