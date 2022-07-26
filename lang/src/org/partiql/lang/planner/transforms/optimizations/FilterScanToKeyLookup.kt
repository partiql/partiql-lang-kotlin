package org.partiql.lang.planner.transforms.optimizations

import com.amazon.ionelement.api.TextElement
import com.amazon.ionelement.api.ionBool
import com.amazon.ionelement.api.ionSymbol
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.domains.toBindingCase
import org.partiql.lang.errors.ProblemHandler
import org.partiql.lang.eval.BindingName
import org.partiql.lang.planner.PartiqlPhysicalPass
import org.partiql.lang.planner.PlannerPipeline
import org.partiql.lang.planner.StaticTypeResolver
import org.partiql.lang.planner.transforms.DEFAULT_IMPL
import org.partiql.lang.types.BagType
import org.partiql.lang.types.ListType
import org.partiql.lang.types.StructType

/**
 * The "filter scan to key lookup" pass identifies all equality expressions where either side is a primary key
 * within the filter's predicate.  This class represents the identified key field and the opposing value expression of
 * each such equality expression. For example, given the filter predicate: `foo.id = 42`, the [keyFieldName] is `id`
 * and the [equivalentValue] is `(lit 42)`.
 */
data class FieldEqualityPredicate(val keyFieldName: String, val equivalentValue: PartiqlPhysical.Expr)

/**
 * Given a `filter` node with a nested `scan`, such as:
 *
 *      (filter (impl ...)
 *          (impl ...)
 *          <predicate>
 *          (scan (impl ...) (global_id <table_id>) (var_decl n))))
 *
 * If `<predicate>` encompasses all primary key fields of the table referenced by `<table_id>` (in the form of
 * `x.keyField1 = <expr-1> [and x.keyFieldN = <expr-n>]...`), then each `x.keyFieldN = <expr-n>` in the `<predicate>`
 * is replaced with the `true` literal. This may leave some unnecessary `and` expressions and `filter` `<predicate>`s.
 * These are removed later by the passes [RemoveUselessAndsPass] and [RemoveUselessFiltersPass].
 *
 * Finally, the nested `scan` is replaced with:
 *
 *     (project
 *          (impl <custom-project-operator-name> <table_id>)
 *          (var_decl n)
 *          <key-value>)
 *
 * An implementation of the `project` operator named [customProjectOperatorName] must be provided by the embedding
 * PartiQL application separately (see [PlannerPipeline.Builder.addRelationalOperatorFactory]).  The custom project
 * operator impl must accept a single `<table_id>` static argument and a single dynamic argument, `<key-value>`, which
 * is the primary key value.  The expression used to compute the key is constructed by [createKeyValueConstructor]
 * which is a function that is passed the table's [StructType] and a list of [FieldEqualityPredicate] instances.
 *
 * ### Notes
 *
 * - Although not strictly needed, it is recommended to include the [RemoveUselessAndsPass] and
 * [RemoveUselessFiltersPass] **after** this pass to remove any useless ands and filters left behind.
 *
 * ### Limitations
 *
 * - Key field references must be fully qualified, (e.g. `someLocalVar.primaryKey = <expr>` and not
 * `primaryKey = <expr>`.  In the future, implicit qualification might be handled by a separate, earlier pass.
 * - Key fields must be values at the first level within the row's struct and cannot be nested. e.g. `x.primaryKey`
 * is supported by `x.person.ssn` is not.
 * - Key field references must be at root node of the `<predicate>` or nested at any level within a tree of expressions
 * consisting only of `and` parents.  (This rewrite cannot apply to other expression types without changing the semantic
 * intent of the query.)
 * - `(eq <expr>...)` expressions not involving exactly 2 operands will fail.  The AST modeling supports an arbitrary
 * number of operands, but supporting > 2 here adds complexity that we may never use, because as of now there is
 * nothing in this entire codebase that composes n-ary expressions with more than 2 operands.
 */
fun createFilterScanToKeyLookupPass(
    customProjectOperatorName: String,
    staticTypeResolver: StaticTypeResolver,
    createKeyValueConstructor: (StructType, List<FieldEqualityPredicate>) -> PartiqlPhysical.Expr
): PartiqlPhysicalPass =
    FilterScanToKeyLookupPass(staticTypeResolver, customProjectOperatorName, createKeyValueConstructor)

private class FilterScanToKeyLookupPass(
    private val staticTypeResolver: StaticTypeResolver,
    private val customProjectOperatorName: String,
    private val createKeyValueConstructor: (StructType, List<FieldEqualityPredicate>) -> PartiqlPhysical.Expr
) : PartiqlPhysicalPass {
    override val passName: String get() = "filter_scan_to_key_lookup"
    override fun rewrite(inputPlan: PartiqlPhysical.Plan, problemHandler: ProblemHandler): PartiqlPhysical.Plan {
        return object : PartiqlPhysical.VisitorTransform() {
            override fun transformBexprFilter(node: PartiqlPhysical.Bexpr.Filter): PartiqlPhysical.Bexpr {
                // Rewrite children first.
                val rewritten = super.transformBexprFilter(node) as PartiqlPhysical.Bexpr.Filter
                when (val filterSource = rewritten.source) {
                    // check if the source of the filter is a full scan
                    is PartiqlPhysical.Bexpr.Scan -> {
                        when (val scanExpr = filterSource.expr) {
                            // check if the scan's expression is a reference to a global variable
                            is PartiqlPhysical.Expr.GlobalId -> {
                                // At this point, we've matched a (filter ... (scan ... (global_id <unique_id>)))
                                // We know the unique id of the table, and we can use it to get the table's static
                                // type.

                                // Non-table global variables may exist and can be any type.
                                // Tables however are always bags or lists of structs.

                                // Let's ensure that we have a bag or list and get the type of its row.
                                val rowStaticType = when (
                                    val globalStaticType =
                                        staticTypeResolver.getVariableStaticType(scanExpr.uniqueId.text)
                                ) {
                                    is BagType -> globalStaticType.elementType
                                    is ListType -> globalStaticType.elementType
                                    else -> return rewritten // <-- bail out; this optimization does not apply
                                }

                                // If the element type (i.e. type of its rows) of the global variable is not a
                                // struct, this optimization also does not apply
                                if (rowStaticType !is StructType) {
                                    return rewritten
                                }

                                // Now that we have static type of the row on hand we can attempt to rewrite the
                                // filter predicate, replacing `<table>.<pkField> = <expr>` with `TRUE`, but
                                // leaving any other part of the filter predicate unmodified.
                                val (newPredicate, keyFieldEqualityPredicates) = rewritten.predicate.rewriteFilterPredicate(
                                    filterSource.asDecl.index.value,
                                    rowStaticType.primaryKeyFields
                                ) // if we didn't succeed in rewriting the filter predicate, there are no primary
                                    // key field references in equality expressions in the filter predicate or not
                                    // all key fields were included and this optimization does not apply.
                                    ?: return rewritten

                                // this just a quick sanity check to be more confident in the result of
                                // .rewriteFilterPredicate(), above if this fails, there is definitely a bug.
                                require(keyFieldEqualityPredicates.size == rowStaticType.primaryKeyFields.size)

                                // Finally, compose a new filter/project to replace the original filter/scan.
                                return PartiqlPhysical.build {
                                    filter(
                                        DEFAULT_IMPL,
                                        newPredicate,
                                        project(
                                            impl(customProjectOperatorName, listOf(ionSymbol(scanExpr.uniqueId.text))),
                                            filterSource.asDecl,
                                            createKeyValueConstructor(rowStaticType, keyFieldEqualityPredicates)
                                        )
                                    )
                                }
                            }
                            else -> return rewritten // didn't match--return the original node unmodified.
                        }
                    }
                    else -> return rewritten // didn't match--return the original node unmodified.
                }
            }
        }.transformPlan(inputPlan)
    }
}

/**
 * In the receiving expression, replaces all expressions in the form of:
 *
 * ```
 * (eq
 *      (path (local_id <local_id>) (path_expr (lit <key-field-name>) <case-sensitivity>))
 *      <expr>
*  )
 * ```
 *
 * ... with `(lit true)` if and only if:
 *
 * - `<local_id>` is equal to [variableIndexId] and
 * - `<key-field-name>` contained within [primaryKeyFields] (respecting `<case-sensitivity>`).
 *
 * Returns a [Pair] containing:
 *
 * - The expression predicate with all references to the key fields replaced with `(lit true)`.
 * - A list of [FieldEqualityPredicate], which is a list of the referenced key fields and value expressions.
 */
private fun PartiqlPhysical.Expr.rewriteFilterPredicate(
    /**
     * The index of the variable containing the primary key. We ignore equality expressions that don't reference this
     * variable.
     */
    variableIndexId: Long,

    /**
     * A list of primary key fields.  Equals expressions must be found in the filter predicate that match all these
     * keys, otherwise, we refuse to rewrite the filter predicate.
     */
    primaryKeyFields: List<String>
): Pair<PartiqlPhysical.Expr, List<FieldEqualityPredicate>>? {
    val remainingFilterKeys = primaryKeyFields.toMutableList()
    val filterKeyValueExpressions = ArrayList<FieldEqualityPredicate>()

    val modifiedPredicate = object : PartiqlPhysical.VisitorTransform() {

        /**
         * Only allow recursion into `eq` and `and` expressions.
         *
         * Other expression types cannot be rewritten without changing the semantic intent of the query.
         */
        override fun transformExpr(node: PartiqlPhysical.Expr): PartiqlPhysical.Expr =
            when (node) {
                is PartiqlPhysical.Expr.And, is PartiqlPhysical.Expr.Eq -> super.transformExpr(node)
                else -> node
            }

        override fun transformExprEq(node: PartiqlPhysical.Expr.Eq): PartiqlPhysical.Expr {
            val rewritten = super.transformExprEq(node) as PartiqlPhysical.Expr.Eq

            // The AST's modeling allows n arguments, but IRL the parser never constructs a node with more than 2
            // operands. If the check below fails, to fix, either:
            // - `eq` expressions with > 2 arguments must be re-nested as binary expressions
            // - rework the logic below to work with more than 2 operands.
            //
            // We don't do the latter now because it seems particularly complex and doesn't have value--there's nothing
            // that flattens n-ary expressions with > 2 arguments today.
            require(rewritten.operands.size == 2) {
                "nest n-ary equality expressions to binary before calling this expression"
            }

            // handleKeyEqualityPredicate returns true and updates both remainingFilterKeys and
            // filterKeyValueExpressions  if the first argument is a reference to a primary key field
            // i.e. `x.keyField = <expr>`.
            var matched = handleKeyEqualityPredicate(rewritten.operands[0], rewritten.operands[1])

            // If we didn't match `x.keyField = <expr>`, try the reverse, i.e. `<expr> = x.keyField`
            if (!matched) {
                matched = handleKeyEqualityPredicate(rewritten.operands[1], rewritten.operands[0])
            }

            return when (matched) {
                false -> rewritten
                true ->
                    // If we've found a reference to a key field, we can replace it with (lit true).
                    // This might make an `and` expression or `(filter ...)` predicate redundant (i.e. `(and (lit true)...)`
                    // or (filter (lit true) ...)` but we remove those in a later pass.
                    PartiqlPhysical.build { lit(ionBool(true)) }
            }
        }

        /**
         * If [operand1] is a key field reference of the local variable with matching [variableIndexId], adds
         * and removes the field from [remainingFilterKeys] and adds an entry to [filterKeyValueExpressions].
         *
         * Otherwise, `false`.
         */
        private fun handleKeyEqualityPredicate(
            operand1: PartiqlPhysical.Expr,
            operand2: PartiqlPhysical.Expr
        ): Boolean {
            val fieldReference = operand1.getKeyFieldReference() ?: return false

            // DL TODO: support case-insensitivity here. for now we force case-sensitive.
            // if the located field reference was to the table of interest
            if (fieldReference.variableId == variableIndexId) {
                // and if the field reference was to one if its key fields (respecting the case-sensitivity requested
                // by the query author)
                val fieldIndex = remainingFilterKeys.indexOfFirst { fieldReference.fieldBindingName.isEquivalentTo(it) }
                if (fieldIndex >= 0) {
                    filterKeyValueExpressions.add(FieldEqualityPredicate(remainingFilterKeys[fieldIndex], operand2))
                    remainingFilterKeys.removeAt(fieldIndex)
                    return true
                }
            }
            return false
        }
    }.transformExpr(this)

    // The rewrite only succeeds if *all* of the key fields have been written out of the predicate.
    return if (remainingFilterKeys.any()) {
        null
    } else {
        modifiedPredicate to filterKeyValueExpressions.toList() // <-- .toList() makes the list immutable.
    }
}

private data class FieldReference(
    val variableId: Long,
    val referencedKey: String,
    val case: PartiqlPhysical.CaseSensitivity
) {
    val fieldBindingName = BindingName(this.referencedKey, this.case.toBindingCase())
}

/**
 * If the receiver matches `(path (local_id <n>) (path_step (lit <field>) <case_sensitivity>)` and if `<field>`
 * is a string, returns `FieldReference(n, field, case_sensitivity)`.  Otherwise, returns null.
 *
 * This makes no determination if the field reference a key or not.
 */
private fun PartiqlPhysical.Expr.getKeyFieldReference(): FieldReference? {
    when (this) {
        is PartiqlPhysical.Expr.Path -> {
            if (this.steps.size != 1) {
                return null
            }
            val fieldStep = this.steps.single() as? PartiqlPhysical.PathStep.PathExpr ?: return null
            val fieldStepIndex = fieldStep.index
            return when {
                fieldStepIndex is PartiqlPhysical.Expr.Lit && fieldStepIndex.value is TextElement ->
                    when (val root = this.root) {
                        is PartiqlPhysical.Expr.LocalId ->
                            FieldReference(root.index.value, fieldStepIndex.value.textValue, fieldStep.case)
                        else -> null
                    }
                else -> null
            }
        }
        else -> return null
    }
}
