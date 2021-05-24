/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.lang.ast

import com.amazon.ion.*
import org.partiql.lang.util.*
import java.util.*

/**
 * Base type for all AST nodes.
 */
sealed class AstNode : Iterable<AstNode> {

    /**
     * Returns all the children nodes.
     *
     * This property is [deprecated](see https://github.com/partiql/partiql-lang-kotlin/issues/396).  Use
     * one of the following PIG-generated classes to analyze AST nodes instead:
     *
     * - [org.partiql.lang.domains.PartiqlAst.Visitor]
     * - [org.partiql.lang.domains.PartiqlAst.VisitorFold]
     */
    @Deprecated("DO NOT USE - see kdoc, see https://github.com/partiql/partiql-lang-kotlin/issues/396")
    abstract val children: List<AstNode>

    /**
     * Depth first iterator over all nodes.
     *
     * While collecting child nodes, throws [InterruptedException] if the [Thread.interrupted] flag has been set.
     *
     * This property is [deprecated](see https://github.com/partiql/partiql-lang-kotlin/issues/396).  Use
     * one of the following PIG-generated classes to analyze AST nodes instead:
     *
     * - [org.partiql.lang.domains.PartiqlAst.Visitor]
     * - [org.partiql.lang.domains.PartiqlAst.VisitorFold]
     */
    @Deprecated("DO NOT USE - see kdoc for alternatives")
    override operator fun iterator(): Iterator<AstNode> {
        val allNodes = mutableListOf<AstNode>()

        fun depthFirstSequence(node: AstNode) {
            allNodes.add(node)
            node.children.interruptibleMap { depthFirstSequence(it) }
        }

        depthFirstSequence(this)
        return allNodes.toList().iterator()
    }
}

/**
 * The only nodes that inherit directly from ExprNode should just be generic expressions that can be used any
 * place a value is allowed.
 */
sealed class ExprNode : AstNode(), HasMetas {

    fun copy(newMetas: MetaContainer? = null): ExprNode {
        // This looks like duplication but really isn't: each branch executes a different compiler-generated `copy` function.
        val metas = newMetas ?: this.metas
        return when (this) {
            is Literal           -> {
                copy(metas = metas)
            }
            is LiteralMissing    -> {
                copy(metas = metas)
            }
            is VariableReference -> {
                copy(metas = metas)
            }
            is NAry              -> {
                copy(metas = metas)
            }
            is CallAgg           -> {
                copy(metas = metas)
            }
            is Typed             -> {
                copy(metas = metas)
            }
            is Path              -> {
                copy(metas = metas)
            }
            is SimpleCase        -> {
                copy(metas = metas)
            }
            is SearchedCase      -> {
                copy(metas = metas)
            }
            is Select -> {
                copy(metas = metas)
            }
            is Struct -> {
                copy(metas = metas)
            }
            is Seq               -> {
                copy(metas = metas)
            }
            is DataManipulation  -> {
                copy(metas = metas)
            }
            is CreateTable       -> {
                copy(metas = metas)
            }
            is CreateIndex       -> {
                copy(metas = metas)
            }
            is DropTable         -> {
                copy(metas = metas)
            }
            is DropIndex         -> {
                copy(metas = metas)
            }
            is Parameter       -> {
                copy(metas = metas)
            }
            is Exec            -> {
                copy(metas = metas)
            }
            is DateTimeType.Date -> {
                copy(metas = metas)
            }
            is DateTimeType.Time -> {
                copy(metas = metas)
            }
        }
    }
}

/** Represents a literal value.  */
data class Literal(
    val ionValue: IonValue,
    override val metas: MetaContainer
) : ExprNode() {
    init {
        ionValue.clone().makeReadOnly()
    }

    override val children: List<AstNode> = listOf()
}


/** Represents the literal value `MISSING`. */
data class LiteralMissing(
    override val metas: MetaContainer
) : ExprNode() {

    override val children: List<AstNode> = listOf()
}

/**
 * A variable reference, which contains a [SymbolicName] and the [CaseSensitivity] that should be used to determine when
 * resolving the variable's binding.
 */
data class VariableReference(
    val id: String,
    val case: CaseSensitivity,
    val scopeQualifier: ScopeQualifier = ScopeQualifier.UNQUALIFIED,
    override val metas: MetaContainer
) : ExprNode() {

    /**
     * Respects case sensitivity when comparing against another [VariableReference].
     */
    override fun equals(other: Any?): Boolean =
        if(other !is VariableReference) { false }
        else {
            id.compareTo(other.id, case == CaseSensitivity.INSENSITIVE) == 0
            && case             == other.case
            && scopeQualifier   == other.scopeQualifier
            && metas            == other.metas
        }

    override fun hashCode(): Int =
        Arrays.hashCode(
            arrayOf(
                when(case) {
                    CaseSensitivity.SENSITIVE -> id
                    CaseSensitivity.INSENSITIVE -> id.toLowerCase()
                },
                case,
                scopeQualifier,
                metas))

    override val children: List<AstNode> = listOf()
}

/**
 * Represents a dynamic parameter with ordinal position for a variable to be provided
 * by the evaluation environment.
 */
data class Parameter(
        val position: Int,
        override val metas: MetaContainer
) : ExprNode() {
    override val children: List<AstNode> = listOf()
}

/**
 * Represents an n-ary expression.  See [NAryOp] for the types of expressions
 * that can be represented with an instance of [NAry].
 */
data class NAry(
    val op: NAryOp,
    val args: List<ExprNode>,
    override val metas: MetaContainer
) : ExprNode() {
    override val children: List<AstNode> = args
}

/**
 * Represents a call to an aggregate function.
 */
data class CallAgg(
    val funcExpr: ExprNode,
    val setQuantifier: SetQuantifier,
    val arg: ExprNode,
    override val metas: MetaContainer
) : ExprNode() {
    override val children: List<AstNode> = listOf(funcExpr, arg)
}

/** Represents a "typed expression", i.e. `CAST` and `IS`. */
data class Typed(
    val op: TypedOp,
    val expr: ExprNode,
    val type: DataType,
    override val metas: MetaContainer
) : ExprNode() {
    override val children: List<AstNode> = listOf(expr, type)
}

//********************************
// Stored procedure clauses
//********************************

/** Represents a call to a stored procedure, i.e. `EXEC stored_procedure [<expr>.*]` */
data class Exec(
    val procedureName: SymbolicName,
    val args: List<ExprNode>,
    override val metas: MetaContainer
) : ExprNode() {
    override val children: List<AstNode> = args
}

//********************************
// Path expressions
//********************************

/** Represents a path expression, i.e. `foo.bar`, `foo[*].bar`, etc. */
data class Path(
    val root: ExprNode,
    val components: List<PathComponent>,
    override val metas: MetaContainer
) : ExprNode() {
    override val children: List<AstNode> = listOf(root) + components
}

//********************************
// Simple CASE
//********************************

/** For `CASE foo WHEN <value> THEN <expr> ELSE <else> END` */
data class SimpleCase(
    val valueExpr: ExprNode,
    val whenClauses: List<SimpleCaseWhen>,
    val elseExpr: ExprNode?,
    override val metas: MetaContainer) : ExprNode() {
    override val children: List<AstNode> = listOf(valueExpr) + whenClauses + listOfNotNull(elseExpr)
}


/** Represents a case of a [SimpleCase]. */
data class SimpleCaseWhen(
    val valueExpr: ExprNode,
    val thenExpr: ExprNode
) : AstNode() {
    override val children: List<AstNode> = listOf(valueExpr, thenExpr)
}

//********************************
// Searched CASE
//********************************

/** For `CASE WHEN <conditionExpr> THEN <thenExpr> ELSE <elseExpr> END`. */
data class SearchedCase(
    val whenClauses: List<SearchedCaseWhen>,
    val elseExpr: ExprNode?,
    override val metas: MetaContainer
) : ExprNode() {
    override val children: List<AstNode> = whenClauses + listOfNotNull(elseExpr)
}

/** Represents a case of a [SearchedCase]. */
data class SearchedCaseWhen(
    val condition: ExprNode,
    val thenExpr: ExprNode
) : AstNode() {
    override val children: List<AstNode> = listOf(condition, thenExpr)
}

//********************************
// Data Manipulation Expressions
//********************************

sealed class DataManipulationOperation(val name: String) : AstNode()

/** Represents `FROM <fromSource> WHERE <whereExpr> <dataManipulationOperation> */
data class DataManipulation(
    val dmlOperations: DmlOpList,
    val from: FromSource? = null,
    val where: ExprNode? = null,
    val returning: ReturningExpr? = null,
    override val metas: MetaContainer
) : ExprNode() {
    override val children: List<AstNode> =
        dmlOperations.children + listOfNotNull(from, where, returning, dmlOperations)
}

data class DmlOpList(val ops: List<DataManipulationOperation>) : AstNode() {
    override val children: List<AstNode> get() = ops
}

/** Represents `INSERT INTO <lvalueExpr> <valuesExpr>` */
data class InsertOp(
    val lvalue: ExprNode,
    val values: ExprNode
) : DataManipulationOperation(name = "insert") {
    override val children: List<AstNode> = listOf(lvalue, values)
}

/** Represents `INSERT INTO <lvalueExpr> VALUE <valueExpr> [AT <position>] [ON CONFLICT WHERE <Expr> <CONFLICT ACTION>]` */
data class InsertValueOp(
    val lvalue: ExprNode,
    val value: ExprNode,
    val position: ExprNode?,
    val onConflict: OnConflict?
): DataManipulationOperation(name = "insert_value") {
    override val children: List<AstNode> = listOfNotNull(lvalue, value, position, onConflict)
}

data class OnConflict(val condition: ExprNode, val conflictAction: ConflictAction
) : AstNode() {
    override val children: List<AstNode> = listOf(condition)
}

/** ConflictAction */
enum class ConflictAction {
    /** Represents DO NOTHING action in ON CONFLICT operation */
    DO_NOTHING
}

data class Assignment(val lvalue: ExprNode, val rvalue: ExprNode) : AstNode() {
    override val children: List<AstNode> = listOf(lvalue, rvalue)
}

data class InsertReturning(
    val ops: List<DataManipulationOperation>,
    val returning: ReturningExpr? = null
) : AstNode() {
    override val children: List<AstNode> get() = ops
}

/**
 * Represents `SET <lvalueExpr> = <rvalueExpr>...`
 */
data class AssignmentOp(val assignment: Assignment) : DataManipulationOperation(name = "set") {
    override val children: List<AstNode> get() = listOf(assignment)
}

/** Represents `REMOVE <lvalueExpr>` */
data class RemoveOp(val lvalue: ExprNode) : DataManipulationOperation(name = "remove") {
    override val children: List<AstNode> get() = listOf(lvalue)
}

/** Represents a legacy SQL `DELETE` whose target is implicit (over the `FROM`/`WHERE` clause) */
object DeleteOp : DataManipulationOperation(name = "delete") {
    override val children: List<AstNode> get() = emptyList()
}
fun DeleteOp() = DeleteOp

/** Represents `RETURNING <returning element> [ ',' <returning element>]*` */
data class ReturningExpr(
    val returningElems: List<ReturningElem>
): AstNode() {
    override val children: List<AstNode> = returningElems
}

/** Represents `<returning mapping> <column_expr>` */
data class ReturningElem(
    val returningMapping: ReturningMapping,
    val columnComponent: ColumnComponent
): AstNode() {
    override val children: List<AstNode> = listOf(columnComponent)
}

sealed class ColumnComponent : AstNode()

data class ReturningColumn(val column: ExprNode) : ColumnComponent() {
    override val children: List<AstNode> get() = listOf(column)
}

/** Represents an `*` that is not part of a path expression in a Returning column, i.e. `RETURNING ALL OLD *`  */
data class ReturningWildcard(override val metas: MetaContainer) : ColumnComponent(), HasMetas {
    override val children: List<AstNode> = listOf()
}

/** Represents ( MODIFIED | ALL ) ( NEW | OLD ) */
enum class ReturningMapping {
    MODIFIED_NEW,
    MODIFIED_OLD,
    ALL_NEW,
    ALL_OLD
}

//********************************
// Select Expression
//********************************

/**
 * Represents a `SELECT` statements as well as the `PIVOT` and `SELECT VALUE`, variants.
 */
data class Select(
    val setQuantifier: SetQuantifier = SetQuantifier.ALL,
    val projection: SelectProjection,
    val from: FromSource,
    val fromLet: LetSource? = null,
    val where: ExprNode? = null,
    val groupBy: GroupBy? = null,
    val having: ExprNode? = null,
    val orderBy: OrderBy? = null,
    val limit: ExprNode? = null,
    override val metas: MetaContainer
) : ExprNode() {
    override val children: List<AstNode> = listOfNotNull(projection, from, fromLet, where, groupBy, having, orderBy, limit)
}

//********************************
// DDL Expressions
//********************************

// TODO determine if we should encapsulate DDL as a separate space from ExprNode...

/**
 * Represents a `CREATE TABLE...` statement.
 *
 * @param tableName the name of the table to be created
 */
data class CreateTable(
    val tableName: String,
    override val metas: MetaContainer
) : ExprNode() {
    override val children: List<AstNode> get() = emptyList()
}

/**
 * Represents a `CREATE INDEX...` statement.
 *
 * TODO:  [keys] should not be modeled as a [List<ExprNode>] since these are not actually
 * expressions.  It actually a reference to an index key, which doesn't have
 * the same semantics--for instance it does not exist in any scope and should not be resolved
 * like a variable does.  We should create a new [AstNode] named `Identifier` which has `id` and
 * `case` properties.
 *
 * @param tableName the name of the table which the index will be created
 * @param keys The expressions that extract the keys from the target to be applied to the index.
 */
data class CreateIndex(
    val tableName: String,
    val keys: List<ExprNode>,
    override val metas: MetaContainer
) : ExprNode() {
    override val children: List<AstNode> get() = keys
}

/**
 * Represents a `DROP TABLE...` statement.
 */
data class DropTable(
    val tableName: String,
    override val metas: MetaContainer
) : ExprNode() {
    override val children: List<AstNode> get() = emptyList()
}

/**
 * Represents a `DROP INDEX $index_identifier ON $table_name` statement.
 *
 * TODO:  [identifier] should not be modeled as a [VariableReference] since it is not actually
 * a reference to a variable.  It actually a reference to an index identifier, which doesn't have
 * the same semantics--for instance identifier only exists within a scope that is limited to its
 * table and should not be resolved in the same way a variable does.  We should create a new
 * [AstNode] named `Identifier` which has `id` and `case` properties.
 */
data class DropIndex(
    val tableName: String,
    val identifier: VariableReference,
    override val metas: MetaContainer
) : ExprNode() {
    override val children: List<AstNode> get() = emptyList()
}

/**
 * SymbolicName does have a semantic meaning and primarily exists so that any identifier in the query may
 * have `Meta` associated with it that is independent of the parent node which imbues its meaning.
 *
 * In the following example:
 *
 * ```
 *     SELECT 1 + 1 AS bar FROM  ...
 *            ^        ^
 *            1        2
 * ```
 *
 * `1 + 1 AS bar` (#1) becomes a `SelectListItemExpr` with `1 + 1` as its expression and `bar` (#2) as its `asName`.
 * It would be ideal to point users to the location of bar specifically in the case of errors related to the alias
 * and to the location of the expression for errors related to the expression. Having SymbolicName allows for
 * this.
 */
data class SymbolicName(
    val name: String,
    override val metas: MetaContainer
) : AstNode(), HasMetas {
    override val children: List<AstNode> = listOf()
}

/**
 * A path component can be:
 *   - An identifier
 *   - An expression with an optional alias
 *   - An unpivot operator (i.e. the `*` in `a.*.c`)
 *   - A wildcard operator (i.e. the `[*]` in `a[*].b`
 * The [PathComponent] base class is used to constrain the types of child nodes that are allowed
 * in a [Path] expression.
 */
sealed class PathComponent : AstNode()

/**
 * Represents a path component that is an expression, i.e. '''['bar']''' in '''foo['bar']'''.
 * Also represents ```a.b``` using a literal string `b` as [expr].
 * [case] indicates the case-sensitivity of the lookup, but this is ignored in the event that [expr] evaluates
 * a value that is not a string.
 */
data class PathComponentExpr(val expr: ExprNode, val case: CaseSensitivity) : PathComponent() {
    companion object {
        private fun getStringValueIfCaseInsensitiveLiteral(component: PathComponentExpr): String? =
            when {
                component.case == CaseSensitivity.INSENSITIVE
                && component.expr is Literal
                && component.expr.ionValue.type == IonType.STRING -> {
                    component.expr.ionValue.stringValue()
                }
                else -> null
            }
    }

    override fun equals(other: Any?): Boolean =
        when (other as? PathComponentExpr) {
            null -> false
            else -> {
                val myStringValue = getStringValueIfCaseInsensitiveLiteral(this)
                val otherStringValue = getStringValueIfCaseInsensitiveLiteral(other)
                when {
                    myStringValue == null && otherStringValue == null -> {
                        // Neither component is a case insensitive string literal, standard equality applies
                        val (otherExpr, otherCase) = other
                        expr.equals(otherExpr) && case == otherCase
                    }
                    else ->
                        when {
                            myStringValue == null || otherStringValue == null ->
                                // Only one of the components was a case insensitive literal, so they are not equal
                                false
                            else                                              ->
                                // Both components are case insensitive literals, perform case insensitive comparison
                                myStringValue.equals(otherStringValue, true)
                        }
                }
            }
        }

    override fun hashCode(): Int =
        Arrays.hashCode(arrayOf(getStringValueIfCaseInsensitiveLiteral(this)?.toLowerCase() ?: expr, case))

    override val children: List<AstNode> = listOf(expr)
}

/** Represents an unpivot path component, i.e. `*` in `foo.*.bar` */
data class PathComponentUnpivot(override val metas: MetaContainer) : PathComponent(), HasMetas {
    override val children: List<AstNode> = listOf()
}

/** Represents a wildcard path component, i.e. `[*]` in `foo[*].bar`. */
data class PathComponentWildcard(override val metas: MetaContainer) : PathComponent(), HasMetas {
    override val children: List<AstNode> = listOf()
}


sealed class SelectProjection : AstNode()

/** For `SELECT <SelectListItem> [, <SelectListItem>]...` and `SELECT *` cases */
data class SelectProjectionList(
    val items: List<SelectListItem>
) : SelectProjection() {

    override val children: List<AstNode> = items
}

/** For `SELECT VALUE <expr>` */
data class SelectProjectionValue(
    val expr: ExprNode
) : SelectProjection() {

    override val children: List<AstNode> = listOf(expr)
}

/** For `PIVOT <expr> AS <asName> AT <atName>` */
data class SelectProjectionPivot(
    val nameExpr: ExprNode,
    val valueExpr: ExprNode
) : SelectProjection() {

    override val children: List<AstNode> = listOf(nameExpr, valueExpr)
}

/**
 * A [SelectListItem] node can be:
 *   - An expression with an optional alias
 *   - A select-all expression (i.e. the `*` in `SELECT * FROM foo`)
 * The [SelectListItem] base class is used to constrain the types of child nodes that are
 * allowed in a [Select] expression's select list.
 *
 * This is intentionally not a subtype of [ExprNode] because things like the select-all expression (`*`) and
 * expressions with aliases are not allowed any place an [ExprNode] is allowed.
 */
sealed class SelectListItem : AstNode()

/** Represents `<expr> [AS alias]` in a select list.*/
data class SelectListItemExpr(
    val expr: ExprNode,
    val asName: SymbolicName? = null
) : SelectListItem() {

    override val children: List<AstNode> = listOf(expr)
}

/** Represents `<expr>.*` in a select list. */
data class SelectListItemProjectAll(val expr: ExprNode) : SelectListItem() {
    override val children: List<AstNode> = listOf(expr)
}

/** Represents an `*` that is not part of a path expression in a select list, i.e. `SELECT * FROM foo`.  */
data class SelectListItemStar(override val metas: MetaContainer) : SelectListItem(), HasMetas {
    override val children: List<AstNode> = listOf()
}

/**
 * A [FromSource] node can be:
 *   - A n-ary expression with optional `AS` and `AT` aliases
 *   - A qualified join (i.e. inner, left, right, outer)
 *   - An unpviot:
 *   Note: a `CROSS JOIN` is modeled as an `INNER JOIN` with a condition of `true`.
 *   Note: `FromSource`s that are separated by commas are modeled as an INNER JOIN with a condition of `true`.
 */
sealed class FromSource : AstNode() {
    fun metas(): MetaContainer = when(this) {
        is FromSourceExpr    -> this.expr.metas
        is FromSourceJoin    -> this.metas
        is FromSourceUnpivot -> this.expr.metas
    }
}

/** Represents `<leftRef> [INNER | OUTER | LEFT | RIGHT] JOIN <rightRef> ON <condition>`. */
data class FromSourceJoin(
    val joinOp: JoinOp,
    val leftRef: FromSource,
    val rightRef: FromSource,
    val condition: ExprNode,
    override val metas: MetaContainer
) : FromSource(), HasMetas {

    override val children: List<AstNode> = listOf(leftRef, rightRef, condition)
}

data class LetVariables(
    val asName: SymbolicName? = null,
    val atName: SymbolicName? = null,
    val byName: SymbolicName? = null
) {
    val isAnySpecified get() = asName != null || atName != null || byName != null
}

/** A base class for the two `FromSource` variants that can introduce `AS`, `AT` or `BY` bindings. */
sealed class FromSourceLet : FromSource() {
    abstract val expr: ExprNode
    abstract val variables: LetVariables

    fun copy(newVariables: LetVariables): FromSourceLet =
        when(this) {
            is FromSourceExpr    -> this.copy(variables = newVariables)
            is FromSourceUnpivot -> this.copy(variables = newVariables)
        }
}

/** Represents `<expr> [AS <correlation>]` within a FROM clause. */
data class FromSourceExpr(
    override val expr: ExprNode,
    override val variables: LetVariables
) : FromSourceLet() {
    override val children: List<AstNode> get() = listOf(expr)
}

/** Represents `SELECT ... FROM UNPIVOT <fromSource> [AS <asName>] [AT <atName>]`.
 *
 * Note:  although this is almost the same as [FromSourceExpr], it has to be kept in its own distinct
 * data class because [FromSourceExpr] has no metas of its own, instead relying on [FromSourceExpr.expr] to store
 * metas.  However, [FromSourceUnpivot] must have its own metas because it a transform on the value resulting from
 * [FromSourceUnpivot.expr]--and thus it changes its data type.  (When supported, type information will be stored
 * as meta nodes.)
 */
data class FromSourceUnpivot(
    override val expr: ExprNode,
    override val variables: LetVariables,
    override val metas: MetaContainer
) : FromSourceLet(), HasMetas {
    override val children: List<AstNode> = listOf(expr)
}

//********************************
// LET clause
//********************************

/** Represents a list of LetBindings */
data class LetSource(
    val bindings: List<LetBinding>
) : AstNode() {
    override val children: List<AstNode> = bindings
}

/** Represents `<expr> AS <name>` */
data class LetBinding(
    val expr: ExprNode,
    val name: SymbolicName
) : AstNode() {
    override val children: List<AstNode> = listOf(expr)
}

/** For `GROUP [ PARTIAL ] BY <item>... [ GROUP AS <gropuName> ]`. */
data class GroupBy(
    val grouping: GroupingStrategy,
    val groupByItems: List<GroupByItem>,
    val groupName: SymbolicName? = null
): AstNode() {
    override val children: List<AstNode> = groupByItems
}

data class GroupByItem(
    val expr: ExprNode,
    val asName: SymbolicName? = null
): AstNode() {
    override val children: List<AstNode> = listOf(expr)
}

/**
 * TODO: Support NULLS FIRST | NULLS LAST
 * `ORDER BY ( <orderingExpression> [ ASC|DESC ] ? `
 */
data class OrderBy(
    val sortSpecItems: List<SortSpec>
): AstNode() {
    override val children: List<AstNode> = sortSpecItems
}

data class SortSpec(
    val expr: ExprNode,
    val orderingSpec: OrderingSpec
): AstNode() {
    override val children: List<AstNode> = listOf(expr)
}
//********************************
// Constructors
//********************************

/** Represents a field in a struct constructor. */
data class StructField(
    val name: ExprNode,
    val expr: ExprNode
): AstNode() {
    override val children: List<AstNode> = listOf(name, expr)
}

/** Represents a struct constructor. */
data class Struct(
    val fields: List<StructField>,
    override val metas: MetaContainer
) : ExprNode() {
    override val children: List<AstNode> = fields
}

enum class SeqType(val typeName: String) {
    LIST("list"),
    SEXP("sexp"),
    BAG("bag")
}

/**
 * Represents a sequence constructor for `list`, `s-expression`, and `bag`.
 */
data class Seq(
    val type: SeqType,
    val values: List<ExprNode>,
    override val metas: MetaContainer
) : ExprNode() {
    override val children: List<AstNode> get() = values
}

/**
 * Represents a data type reference such as `INT` or `VARCHAR(50)`.
 */
data class DataType(
    val sqlDataType: SqlDataType,
    val args: List<Long>,
    override val metas: MetaContainer
) : AstNode(), HasMetas {
    override val children: List<AstNode> = listOf()
}

//********************************
// Node attributes
//********************************

/** Indicates case sensitivity of variable references. */
enum class CaseSensitivity(private val symbol: String) {
    SENSITIVE("case_sensitive"),
    INSENSITIVE("case_insensitive");

    companion object {
        fun fromSymbol(s: String) : CaseSensitivity = when (s) {
            "case_sensitive" -> SENSITIVE
            "case_insensitive" -> INSENSITIVE
            else -> throw IllegalArgumentException("Unrecognized CaseSensitivity $s")
        }
    }

    fun toSymbol() = symbol

}

/** Indicates if all rows in a select query are to be returned or only distinct rows. */
enum class SetQuantifier {
    ALL, DISTINCT
}

/**
 * Different types of n-ary operations.
 * [minArity] and [maxArity] are to be used during AST validation.
 * [symbol] is used to look up an [NAryOp] instance from the token text and must be unique among
 * all instances of [NAryOp].
 * [textName] is the user-friendly name of this operation. It's used to indicate
 * operator in an AST with version [AstVersion.V2] or above.
 */
enum class NAryOp(val arityRange: IntRange, val symbol: String, val textName: String = symbol) {
    /** Add, but when arity is 1 then this just returns the value of its argument. */
    ADD(1..Int.MAX_VALUE, "+", "plus"),

    /** Subtract, but when when arity is 1, then this is assumed to be 0 - <arg1>. */
    SUB(1..Int.MAX_VALUE, "-", "minus"),

    /** Multiply */
    MUL(2..Int.MAX_VALUE, "*", "times"),

    /** Divide */
    DIV(2..Int.MAX_VALUE, "/", "divide"),

    /** Modulus */
    MOD(2..Int.MAX_VALUE, "%", "modulo"),

    /** Equivalent */
    EQ(2..Int.MAX_VALUE, "=", "eq"),

    /** Less-than */
    LT(2..Int.MAX_VALUE, "<", "lt"),

    /** Less-than-or-equal.*/
    LTE(2..Int.MAX_VALUE, "<=", "lte"),

    /** Greater-than. */
    GT(2..Int.MAX_VALUE, ">", "gt"),

    /** Greater-than-or-equal. */
    GTE(2..Int.MAX_VALUE, ">=", "gte"),

    /** Not Equals. */
    NE(2..Int.MAX_VALUE, "<>", "ne"),

    /** Like. */
    LIKE(2..3, "like"),

    /** Between expression..i.e. `<expr1> BETWEEN <expr2> AND <expr3>`*/
    BETWEEN(3..3, "between"),

    /** IN expression, i.e. `<expr1> IN <containerExpr> ` */
    IN(2..Int.MAX_VALUE, "in", "in_collection"),

    /** Logical not */
    NOT(1..1, "not"),

    /** Logical `and`. */
    AND(2..Int.MAX_VALUE, "and"),

    /** Logical `or` */
    OR(2..Int.MAX_VALUE, "or"),

    /** String concatenation. */
    STRING_CONCAT(2..Int.MAX_VALUE, "||", "concat"),

    /** A function call. */
    CALL(0..Int.MAX_VALUE, "call"),

    // The following are capable of being parsed by [SqlParser] but are not currently being consumed by
    // [EvaluatingCompiler].  Additionally, they are not well defined in README-AST.md, which does not
    // seem to agree with what is defined for these in the PartiQL spec.  For instance, the spec references an
    // ORDERED keyword, but this keyword is never used in relation to these operations in the source or README-AST.md.
    // As a result, these definitions are a "best guess" that will likely need to be modified when it comes time
    // to support these operators.
    INTERSECT(2..Int.MAX_VALUE, "intersect"),
    INTERSECT_ALL(2..Int.MAX_VALUE, "intersect_all"),
    EXCEPT(2..Int.MAX_VALUE, "except"),
    EXCEPT_ALL(2..Int.MAX_VALUE, "except_all"),
    UNION(2..Int.MAX_VALUE, "union"),
    UNION_ALL(2..Int.MAX_VALUE, "union_all");

    companion object {
        /** Map of [NAryOp] keyed by the operation's text. */
        private val OP_SYMBOL_TO_OP_LOOKUP =
            NAryOp.values()
                .map { Pair(it.symbol, it) }
                .toMap()


        fun forSymbol(symbol: String): NAryOp? = OP_SYMBOL_TO_OP_LOOKUP[symbol]
    }
}

/** Different types of type expressions.  */
enum class TypedOp(val text: String) {
    CAST("cast"),
    IS("is")
}

/** Type of join operation. */
enum class JoinOp {
    /** INNER JOIN. */
    INNER,

    /** A LEFT OUTER JOIN. */
    LEFT,

    /** A RIGHT OUTER JOIN. */
    RIGHT,

    /**
     * A FULL OUTER JOIN.
     * TODO/NOTE:  this really should be named "full" instead of "outer".
     */
    OUTER
}

/** Grouping strategy. */
enum class GroupingStrategy {
    /** Represents `GROUP BY` (no partial). */
    FULL,
    /** Represents `GROUP PARTIAL BY`.*/
    PARTIAL
}

/** Ordering specification */
enum class OrderingSpec {
    /** Represents */
    ASC,
    DESC
}

/**
 * The sealed class includes all the datetime types such as DATE, TIME, TIMESTAMP
 * Note that the ast nodes corresponding to the DATE, TIME and TIMESTAMP here are different from the [Literal] nodes.
 * You can create an Ion literal as [Timestamp] which will correspond to the [Literal] node and will have the type
 * [SqlDataType.TIMESTAMP]. However that will be different from the
 * `TIMESTAMP` here.
 * Note: TIME and TIMESTAMP are yet to be added.
 */
sealed class DateTimeType : ExprNode() {
    /**
     * AST Node corresponding to the DATE literal
     */
    data class Date(
        val year: Int,
        val month: Int,
        val day: Int,
        override val metas: MetaContainer
    ) : DateTimeType() {
        override val children: List<AstNode> = listOf()
    }

    /**
     * AST node representing the TIME literal.
     *
     * @param hour represents the hour value.
     * @param minute represents the minute value.
     * @param second represents the second value.
     * @param nano represents the fractional part of the second up to the nanoseconds' precision.
     * @param precision is an optional parameter which, if specified, represents the precision of the fractional second.
     * @param tz_minutes is the optional time zone in minutes which can be specified with "WITH TIME ZONE".
     * If [tz_minutes] is null, that means the time zone is undefined.
     */
    data class Time(
        val hour: Int,
        val minute: Int,
        val second: Int,
        val nano: Int,
        val precision: Int,
        val tz_minutes: Int? = null,
        override val metas: MetaContainer
    ) : DateTimeType() {
        override val children: List<AstNode> = listOf()
    }
}

/**
 * Indicates strategy for binding lookup within scopes.
 */
enum class ScopeQualifier {
    /** For variable references *not* prefixed with '@'. */
    UNQUALIFIED,

    /** For variable references prefixed with '@'. */
    LEXICAL,
}

/**
 * The core PartiQL data types.
 */
enum class SqlDataType(val typeName: String, val arityRange: IntRange) {
    MISSING("missing", 0..0), // PartiQL
    NULL("null", 0..0), // Ion
    BOOLEAN("boolean", 0..0), // Ion & SQL-99
    SMALLINT("smallint", 0..0), // SQL-92
    INTEGER("integer", 0..0), // Ion & SQL-92
    FLOAT("float", 0..1), // Ion & SQL-92
    REAL("real", 0..0), // SQL-92
    DOUBLE_PRECISION("double_precision", 0..0), // SQL-92
    DECIMAL("decimal", 0..2), // Ion & SQL-92
    NUMERIC("numeric", 0..2), // SQL-92
    TIMESTAMP("timestamp", 0..0), // Ion & SQL-92
    CHARACTER("character", 0..1), // SQL-92
    CHARACTER_VARYING("character_varying", 0..1), // SQL-92
    STRING("string", 0..0), // Ion
    SYMBOL("symbol", 0..0), // Ion
    CLOB("clob", 0..0), // Ion
    BLOB("blob", 0..0), // Ion
    STRUCT("struct", 0..0), // Ion
    TUPLE("tuple", 0..0), // PartiQL
    LIST("list", 0..0), // Ion
    SEXP("sexp", 0..0), // Ion
    DATE("date", 0..0), // SQL-92
    TIME("time", 0..1), // SQL-92
    TIME_WITH_TIME_ZONE("time_with_time_zone", 0..1), // SQL-92
    BAG("bag", 0..0);  // PartiQL

    companion object {
        private val DATA_TYPE_NAME_TO_TYPE_LOOKUP =
            SqlDataType.values()
                .map { Pair(it.typeName, it) }
                .toMap()

        fun forTypeName(typeName: String): SqlDataType? = DATA_TYPE_NAME_TO_TYPE_LOOKUP[typeName]
    }
}
