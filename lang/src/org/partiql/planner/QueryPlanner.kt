package org.partiql.planner

import com.amazon.ion.IonSystem
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.errors.Problem
import org.partiql.lang.errors.ProblemCollector
import org.partiql.lang.errors.Property
import org.partiql.lang.syntax.SqlParser
import org.partiql.lang.syntax.SyntaxException
import org.partiql.planner.transforms.PlanningProblemDetails
import org.partiql.planner.transforms.normalize
import org.partiql.planner.transforms.toResolved
import org.partiql.planner.transforms.toLogical
import org.partiql.planner.transforms.toPhysical

/** A simple interface for a query planner. */
interface QueryPlanner {
    /**
     * Parses a query and transforms it all the way to a [PartiqlPhysical.Statement] that is ready for compilation.
     */
    fun plan(sql: String): PlanningResult

    /**
     * Transforms the specified query in the form of a [PartiqlAst.Statement], transforming all the way to a
     * [PartiqlPhysical.Statement] that is ready for compilation.
     */
    fun plan(sql: PartiqlAst.Statement): PlanningResult
}

sealed class PlanningResult {
    /**
     * Indicates query planning was successful and includes a list of any warnings that were encountered along the way.
     */
    data class Success(val physicalPlan: PartiqlPhysical.Statement, val warnings: List<Problem>) : PlanningResult()

    /**
     * Indicates query planning was not successful and includes a list of errors and warnings that were encountered
     * along the way.
     */
    data class Error(val errors: List<Problem>) : PlanningResult()
}

/**
 * Creates an implementation of [QueryPlanner].
 *
 * Technical debt:
 * - We have to take a dependency on [IonSystem] because [SqlParser] requires it.  There is
 * [some work to do](https://github.com/partiql/partiql-lang-kotlin/issues/531) before this can change.
 *
 * @param ion This process's instance of [IonSystem].
 * @param globals Implemented by the PartiQL service, this interface is queried to translate all global variables
 * into unique id values that can be utilized at evaluation time to access the variable's contents.  Global variables
 * are typically database tables, but they can any type of data including scalar values.
 */
fun createQueryPlanner(
    ion: IonSystem,
    globals: GlobalBindings
): QueryPlanner = QueryPlannerImpl(ion, globals)


/**
 * This is an embryonic query planner.
 *
 * Currently, calling [plan] will:
 *
 * - Parse the specified SQL string, producing an AST.
 * - Convert the AST to a logical plan.
 * - Resolve all global and local variables in the logical plan, assigning unique indexes to local variables
 * and calling [GlobalBindings.resolve] of [globals] to obtain PartiQL-service specific unique identifiers of global
 * values such as tables.
 * - Convert the AST to a physical plan with `(impl default)` operators.
 *
 * Future work:
 *
 * - Query plan caching.
 * - In logical plans, push down filter and predicates until they are on top of their `(scan ...)` nodes.
 * - In physical plans, push down filter and predicates inside each `(project ...)` nodes.
 * - Provide a way for PartiQL services to select different operator implementations.
 */
private class QueryPlannerImpl(
    private val ion: IonSystem,
    private val globals: GlobalBindings
) : QueryPlanner {

    override fun plan(sql: String): PlanningResult {
        val parser = SqlParser(ion)

        val ast = try {
            parser.parseAstStatement(sql)
        } catch (ex: SyntaxException) {
            val problem = Problem(
                SourceLocationMeta(
                    ex.errorContext?.get(Property.LINE_NUMBER)?.longValue() ?: -1,
                    ex.errorContext?.get(Property.COLUMN_NUMBER)?.longValue() ?: -1
                ),
                PlanningProblemDetails.ParseError(ex.generateMessageNoLocation())
            )
            return PlanningResult.Error(listOf(problem))
        }

        return plan(ast)
    }

    override fun plan(ast: PartiqlAst.Statement): PlanningResult {

        // Now run the AST thru each pass until we arrive at the physical algebra.

        // Normalization--synthesizes any unspecified `AS` aliases, converts `SELECT *` to `SELECT f.*[, ...]` ...
        val normalizedAst = ast.normalize()

        // ast -> logical plan
        val logicalPlan = normalizedAst.toLogical()

        // logical plan -> resolved logical plan
        val problemHandler = ProblemCollector()
        val resolvedLogicalPlan = logicalPlan.toResolved(problemHandler, globals)
        // If there are unresolved variables after attempting to resolve variables, then we can't proceed.
        if (problemHandler.hasErrors) {
            return PlanningResult.Error(problemHandler.problems)
        }

        // Possible future passes:
        // - type checking and inferencing?
        // - constant folding
        // - common sub-expression removal
        // - push down predicates & projections on top of their scan nodes.

        // resolved logical plan -> physical plan.
        // this will give all relational operators `(impl default)`.
        val physicalPlan = resolvedLogicalPlan.toPhysical()

        // Future work: invoke passes to choose relational operator implementations other than `(impl default)`.
        // Future work: fully push down predicates and projections into their physical read operators.

        // If we reach this far, we're successful.  If there were any problems at all, they were just warnings.
        return PlanningResult.Success(physicalPlan, problemHandler.problems)
    }
}
