package org.partiql.lang.planner.transforms.optimizations

import com.amazon.ionelement.api.TextElement
import com.amazon.ionelement.api.ionBool
import com.amazon.ionelement.api.ionSymbol
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.planner.MetadataResolver
import org.partiql.lang.planner.PartiqlPhysicalPass
import org.partiql.lang.planner.transforms.DEFAULT_IMPL
import org.partiql.lang.types.BagType
import org.partiql.lang.types.ListType
import org.partiql.lang.types.StructType

/**
 * The "filter scan to key lookup" pass identifies all equality expressions where the left-hand side is a primary key
 * within the filter's predicate.  This class represents the identified key field and right-hand-side of each such
 * expression. For example, given the filter predicate: `foo.id = 42`, the [keyFieldName] is `id` and the
 * [equivalentValue] is `(lit 42)`.
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
 * DL TODO: document the expected signature of the custom get-by-key operator (static and dynamic)
 * DL TODO: accept an argument that is a function creates the list of arguments to pass to the `project` operator.
 * DL TODO: based on a List<FoundKeyEqualityPredicate>
 */
fun createFilterScanToKeyLookupPass(
    customOperatorName: String,
    metadataResolver: MetadataResolver,
    createKeyValueConstructor: (StructType, List<FieldEqualityPredicate>) -> PartiqlPhysical.Expr
): PartiqlPhysicalPass =
    PartiqlPhysicalPass { inputPlan, _ ->
        object : PartiqlPhysical.VisitorTransform() {
            override fun transformBexprFilter(node: PartiqlPhysical.Bexpr.Filter): PartiqlPhysical.Bexpr {
                val rewritten = super.transformBexprFilter(node) as PartiqlPhysical.Bexpr.Filter
                when (val filterSource = rewritten.source) {
                    is PartiqlPhysical.Bexpr.Scan -> {
                        when (val scanExpr = filterSource.expr) {
                            is PartiqlPhysical.Expr.GlobalId -> {
                                // At this point, we've matched a (filter ... (scan (global_id <uuid>)))
                                // And we know the unique id of the table and we can use this table id to get its static
                                // type.

                                // The global variable must be a bag or list of structs, otherwise, it's not a table
                                // and this optimization does not apply.
                                val rowStaticType = when (
                                    val globalStaticType =
                                        metadataResolver.getGlboalVariableStaticType(scanExpr.uniqueId.text)
                                ) {
                                    is BagType -> globalStaticType.elementType
                                    is ListType -> globalStaticType.elementType
                                    else -> return rewritten
                                }

                                // If the element type (i.e. type of its rows) of the global variable is not a struct,
                                // this optimization also does not apply
                                if (rowStaticType !is StructType) {
                                    return rewritten
                                }

                                // Now that we have the metadata on hand we can attempt to rewrite the filter predicate
                                val (newPredicate, keyFieldEqualityPredicates) = rewritten.predicate.rewriteFilterPredicate(
                                    filterSource.asDecl.index.value,
                                    rowStaticType.primaryKeyFields
                                )
                                    ?: return rewritten // if we didn't succeed in rewriting the filter predicate, return original node.

                                // just a quick sanity check to be more confident in the result of .rewriteFilterPredicate()
                                require(keyFieldEqualityPredicates.size == rowStaticType.primaryKeyFields.size)

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
    /** The index of the variable containing the primary key. */
    variableIndexId: Long,
    primaryKeyFields: List<String>
): Pair<PartiqlPhysical.Expr, List<FieldEqualityPredicate>>? {
    val remainingFilterKeys = primaryKeyFields.toMutableSet()
    val filterKeyValueExpressions = ArrayList<FieldEqualityPredicate>()

    val modifiedPredicate = object : PartiqlPhysical.VisitorTransform() {
        override fun transformExprEq(node: PartiqlPhysical.Expr.Eq): PartiqlPhysical.Expr {
            // DL TODO: is it needed to recurse here?  what might be in child that we'd skip if we don't recurse?
            val rewritten = super.transformExprEq(node) as PartiqlPhysical.Expr.Eq

            // TODO: We still need to normalize predicates here so that f.key always appears left of =, i..e f.key = 42.
            // TODO: For now this pass won't work if the opposite (42 = f.key) is specified by the user.

            // TODO: support more than two operands here? (The AST's modeling allows n arguments, but IRL the parser
            // TODO: never constructs a node with more than 2)
            require(rewritten.operands.size == 2)
            val left = rewritten.operands[0]
            val right = rewritten.operands[1]

            // assume the lhs has the path expression for now since we are not yet normalizing.
            val fieldReference = left.getCandidateKeyFieldReference() ?: return rewritten

            // DL TODO: support case-insensitivity here. for now we force case-sensitive.
            return when {
                // if the located field reference was to the table of interest and if the field reference was to
                // one if its key fields
                fieldReference.variableId == variableIndexId && remainingFilterKeys.remove(fieldReference.referencedKey) -> {
                    // Need to track keep track of the key field equals expressions that we've removed so they can be
                    // used to make a constructor expression that returns only the values of the primary key fields
                    // later.
                    filterKeyValueExpressions.add(FieldEqualityPredicate(fieldReference.referencedKey, right))

                    // If we've found a reference to a key field, we can replace it with (lit true)!
                    // This might make an `and` expression or `(filter ...)` predicate redundant (i.e. `(and (lit true)...)`
                    // or (filter (lit true) ...)` but we remove those in a later pass.
                    PartiqlPhysical.build { lit(ionBool(true)) }
                }
                else -> {
                    // We didn't find a reference to a key field, so let's leave the expression untouched.
                    rewritten
                }
            }
        }

        // Prevent recursion into sub-queries.
        override fun transformExprBindingsToValues(node: PartiqlPhysical.Expr.BindingsToValues): PartiqlPhysical.Expr =
            node
    }.transformExpr(this)

    // The rewrite only succeeds if *all* of the key fields have been written out of the predicate.
    return if (remainingFilterKeys.any()) {
        null
    } else {
        modifiedPredicate to filterKeyValueExpressions.toList() // <-- .toList() makes the list immutable.
    }
}

/** todo: kdoc. */
data class FieldReferernce(val variableId: Long, val referencedKey: String, val case: PartiqlPhysical.CaseSensitivity)

/**
 * If the receiver matches `(path (local_id <n>) (path_step (lit <field>))` and `<field>` is a string,
 * returns `FieldReference(n, field)`.
 *
 * DL TODO: explain use of the term "candidate".
 */
private fun PartiqlPhysical.Expr.getCandidateKeyFieldReference(): FieldReferernce? {
    when (this) {
        is PartiqlPhysical.Expr.Path -> {
            if (this.steps.size != 1) {
                return null
            }
            val fieldStep: PartiqlPhysical.PathStep = this.steps.single()
            if (fieldStep !is PartiqlPhysical.PathStep.PathExpr) {
                return null
            }
            val fieldStepIndex = fieldStep.index
            return when {
                fieldStepIndex is PartiqlPhysical.Expr.Lit && fieldStepIndex.value is TextElement ->
                    when (val root = this.root) {
                        is PartiqlPhysical.Expr.LocalId -> {
                            FieldReferernce(root.index.value, fieldStepIndex.value.textValue, fieldStep.case)
                        }
                        else -> null
                    }
                else -> null
            }
        }
        else -> return null
    }
}
