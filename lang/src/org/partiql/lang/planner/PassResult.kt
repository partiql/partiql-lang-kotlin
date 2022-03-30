package org.partiql.lang.planner
import org.partiql.lang.errors.Problem

sealed class PassResult<TResult> {
    /**
     * Indicates query planning was successful and includes a list of any warnings that were encountered along the way.
     */
    data class Success<TResult>(val result: TResult, val warnings: List<Problem>) : PassResult<TResult>()

    /**
     * Indicates query planning was not successful and includes a list of errors and warnings that were encountered
     * along the way.
     */
    data class Error<TResult>(val errors: List<Problem>) : PassResult<TResult>()
}

// DL TODO: can I utilize any of the kdoc below?

//
///**
// * Creates an implementation of [QueryPlanner].
// *
// * Technical debt:
// * - We have to take a dependency on [IonSystem] because [SqlParser] requires it.  There is
// * [some work to do](https://github.com/partiql/partiql-lang-kotlin/issues/531) before this can change.
// *
// * @param ion This process's instance of [IonSystem].
// * @param globals Implemented by the PartiQL service, this interface is queried to translate all global variables
// * into unique id values that can be utilized at evaluation time to access the variable's contents.  Global variables
// * are typically database tables, but they can any type of data including scalar values.
// */
//internal fun createQueryPlanner(
//    ion: IonSystem,
//    allowUndefinedVariables: Boolean = false,
//    globals: GlobalBindings
//): QueryPlanner = QueryPlannerImpl(ion, allowUndefinedVariables, globals)
//
///**
// * This is an embryonic query planner.
// *
// * Currently, calling [plan] will:
// *
// * - Parse the specified SQL string, producing an AST.
// * - Convert the AST to a logical plan.
// * - Resolve all global and local variables in the logical plan, assigning unique indexes to local variables
// * and calling [GlobalBindings.resolve] of [globals] to obtain PartiQL-service specific unique identifiers of global
// * values such as tables.  If [allowUndefinedVariables] is set to true, undefined variables will not result in an error
// * and will remain unmodified.
// * - Convert the AST to a physical plan with `(impl default)` operators.
// *
// * Future work:
// *
// * - Query plan caching.
// * - In logical plans, push down filter and predicates until they are on top of their `(scan ...)` nodes.
// * - In physical plans, push down filter and predicates inside each `(project ...)` nodes.
// * - Provide a way for PartiQL services to select different operator implementations.
// */
//private class QueryPlannerImpl(
//    private val ion: IonSystem,
//    val allowUndefinedVariables: Boolean,
//    private val globals: GlobalBindings
//) : QueryPlanner {
//
//    override fun plan(ast: PartiqlAst.Statement): PassResult {
//
//
//    }
//}
