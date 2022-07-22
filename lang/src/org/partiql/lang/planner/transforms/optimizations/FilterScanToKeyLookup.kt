package org.partiql.lang.planner.transforms.optimizations

import com.amazon.ionelement.api.TextElement
import com.amazon.ionelement.api.ionBool
import com.amazon.ionelement.api.ionSymbol
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.errors.ProblemHandler
import org.partiql.lang.planner.PartiqlPhysicalPass
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
 * Creates a pass that changes a full scan with a filter:
 *
 *      (filter (impl ...)
 *          (impl ...)
 *          <predicate encompassing the table's entire primary key>
 *          (scan (impl ...) (global_id <uuid>) (var_decl n))))
 *
 * To the optimized form that looks the record up using its key:
 *
 *     (project
 *          (impl $customOperatorName)
 *          (var_decl n)
 *          (list <key-field-x>, ...))
 *
 * Note that this requires filters to be pushed down on top of their corresponding scans.  Since we don't have such a
 * pass yet, this will only work when there's a single table involved in the query.  When there's a single table,
 * the filters and scans are already arranged in this fashion.
 * DL TODO: document the expected signature of the custom get-by-key operator (static and dynamic) and describe createKeyValueConstructor
 */
fun createFilterScanToKeyLookupPass(
    customOperatorName: String,
    staticTypeResolver: StaticTypeResolver,
    createKeyValueConstructor: (StructType, List<FieldEqualityPredicate>) -> PartiqlPhysical.Expr
): PartiqlPhysicalPass =
    object : PartiqlPhysicalPass {
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
                                    // At this point, we've matched a (filter ... (scan (global_id <uuid>)))
                                    // We know the unique id of the table, and we can use the id to get the table's static
                                    // type.

                                    // Non-table global variables may exist and can be any type.
                                    // Tables however are always bags or lists of structs.
                                    val rowStaticType = when (
                                        val globalStaticType =
                                            staticTypeResolver.getVariableStaticType(scanExpr.uniqueId.text)
                                    ) {
                                        is BagType -> globalStaticType.elementType
                                        is ListType -> globalStaticType.elementType
                                        else -> return rewritten // <-- bail out; this optimization does not apply
                                    }

                                    // If the element type (i.e. type of its rows) of the global variable is not a struct,
                                    // this optimization also does not apply
                                    if (rowStaticType !is StructType) {
                                        return rewritten
                                    }

                                    // Now that we have the metadata on hand we can attempt to rewrite the filter predicate
                                    // replacing `<table>.<pkField> = <expr>` with `TRUE`, but leaving any other expression
                                    // behind.  This function also returns one instance of KeyFieldEqualityPredicate for
                                    // each replacement it made, which contains
                                    val (newPredicate, keyFieldEqualityPredicates) = rewritten.predicate.rewriteFilterPredicate(
                                        filterSource.asDecl.index.value,
                                        rowStaticType.primaryKeyFields
                                    )   // if we didn't succeed in rewriting the filter predicate, there are no primary
                                    // key field references in equality expressions in the filter predicate (or not all
                                    // key fields were included) and this optimization does not apply.
                                        ?: return rewritten

                                    // just a quick sanity check to be more confident in the result of
                                    // .rewriteFilterPredicate(), above if this fails, there is definitely a bug.
                                    require(keyFieldEqualityPredicates.size == rowStaticType.primaryKeyFields.size)

                                    // Finally, compose a new filter/project to replace the original filter/scan.
                                    // For single-key tables, the rewritten filter predicate will just be `(lit true)`,
                                    // meaning it would be possible to eliminate the filter here.  However, this is
                                    // not the cause for tables with compound keys, so we don't
                                    return PartiqlPhysical.build {
                                        filter(
                                            DEFAULT_IMPL,
                                            newPredicate,
                                            project(
                                                impl(customOperatorName, listOf(ionSymbol(scanExpr.uniqueId.text))),
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
 * Given a filter predicate (the receiver of this function) and the table's metadata, rewrite
 * the predicate to remove references the primary key fields.  TODO: describe a lot more.
 *
 * DL TODO: need to clarify and reword all of this.
 *
 * Returns two things:
 * - The filter predicate with all references to the key fields removed.  (Might have multiple (lit true)) in it,
 * these will be removed in a later pass.)
 * - A list of the referenced key fields and value expressions.
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
    val remainingFilterKeys = primaryKeyFields.toMutableSet()
    val filterKeyValueExpressions = ArrayList<FieldEqualityPredicate>()

    val modifiedPredicate = object : PartiqlPhysical.VisitorTransform() {
        override fun transformExprEq(node: PartiqlPhysical.Expr.Eq): PartiqlPhysical.Expr {
            val rewritten = super.transformExprEq(node) as PartiqlPhysical.Expr.Eq

            // TODO: support more than two operands here? (The AST's modeling allows n arguments, but IRL the parser
            // TODO: never constructs a node with more than 2)
            require(rewritten.operands.size == 2)

            // handleKeyEqualityPredicate returns true and updates both remainingFilterKeys and
            // filterKeyValueExpressions  if the first argument is a reference to a primary key field
            // i.e. `x.keyField = <expr>`.
            var matched = handleKeyEqualityPredicate(rewritten.operands[0], rewritten.operands[1])

            // If we didn't match `x.keyField = <expr>`, try the reverse, i.e. `<expr> = x.keyField`
            if(!matched) {
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

        private fun handleKeyEqualityPredicate(
            operand1: PartiqlPhysical.Expr,
            operand2: PartiqlPhysical.Expr
        ): Boolean {
            val fieldReference = operand1.getKeyFieldReference() ?: return false

            // DL TODO: support case-insensitivity here. for now we force case-sensitive.
            return when {
                // if the located field reference was to the table of interest and if the field reference was to
                // one if its key fields
                fieldReference.variableId == variableIndexId && remainingFilterKeys.remove(fieldReference.referencedKey) -> {
                    // Need to track keep track of the key field equals expressions that we've removed so they can be
                    // used to make a constructor expression that returns only the values of the primary key fields
                    // later.
                    filterKeyValueExpressions.add(FieldEqualityPredicate(fieldReference.referencedKey, operand2))
                    true
                }
                else -> false
            }
        }

        override fun transformExprBindingsToValues(
            node: PartiqlPhysical.Expr.BindingsToValues
        ): PartiqlPhysical.Expr = node // <-- prevents recursion into sub-queries.

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
)

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
