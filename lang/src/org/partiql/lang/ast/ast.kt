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
     * returns all the children nodes.
     */
    abstract val children: List<AstNode>

    /**
     * Depth first iterator over all nodes.
     */
    override operator fun iterator(): Iterator<AstNode> {
        fun depthFirstSequence(node: AstNode): Sequence<AstNode> =
            sequenceOf(node) + node.children.asSequence().flatMap { depthFirstSequence(it) }
        
        return depthFirstSequence(this).iterator();
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
            is Select            -> {
                copy(metas = metas)
            }
            is Struct            -> {
                copy(metas = metas)
            }
            is ListExprNode      -> {
                copy(metas = metas)
            }
            is Bag               -> {
                copy(metas = metas)
            }
        }
    }
}

//********************************
// Basic arithmetic expressions.
//********************************

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
    override val children: List<AstNode> = listOf(expr)
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
// Select Expression
//********************************

/**
 * Represents a `SELECT` statements as well as the `PIVOT` and `SELECT VALUE`, variants.
 */
data class Select(
    val setQuantifier: SetQuantifier = SetQuantifier.ALL,
    val projection: SelectProjection,
    val from: FromSource,
    val where: ExprNode? = null,
    val groupBy: GroupBy? = null,
    val having: ExprNode? = null,
    val limit: ExprNode? = null,
    override val metas: MetaContainer
) : ExprNode() {
    override val children: List<AstNode> = listOfNotNull(projection, from, where, groupBy, having, limit)
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
 * `1 + 1 AS bar` (#1) becomes a `SelectListItemExpr` with `1 + 1` as its expression and `bar` (#2) as its `asAlias`.
 * It would be ideal to point users to the location of bar specifically in the case of errors related to the alias
 * and to the location of the expression for errors related to the expression. Having SymbolicName allows for
 * this.
 */
data class SymbolicName(
    val name: String,
    override val metas: MetaContainer
) : HasMetas

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
    val valueExpr: ExprNode,
    val nameExpr: ExprNode
) : SelectProjection() {
    
    override val children: List<AstNode> = listOf(valueExpr, nameExpr)
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
sealed class FromSource : AstNode()

/** Represents `<expr> [AS <correlation>]` within a FROM clause. */
data class FromSourceExpr(
    val expr: ExprNode,
    val asName: SymbolicName? = null,
    val atName: SymbolicName? = null
) : FromSource() {
    override val children: List<AstNode> = listOf(expr)
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

/** Represents `SELECT ... FROM UNPIVOT <fromSource> [AS <asName>] [AT <atName>]`.
 *
 * Note:  although this is almost the same as [FromSourceExpr], it has to be kept in its own distinct
 * data class because [FromSourceExpr] has no metas of its own, instead relying on [FromSourceExpr.expr] to store
 * metas.  However, [FromSourceUnpivot] must have its own metas because it a transform on the value resulting from
 * [FromSourceUnpivot.expr]--and thus it changes its data type.  (When supported, type information will be stored
 * as meta nodes.)
 */
data class FromSourceUnpivot(
    val expr: ExprNode,
    val asName: SymbolicName?,
    val atName: SymbolicName?,
    override val metas: MetaContainer
) : FromSource(), HasMetas {
    
    override val children: List<AstNode> = listOf(expr)
}

fun FromSource.metas() =
    when(this) {
        is FromSourceExpr    -> this.expr.metas
        is FromSourceJoin    -> this.metas
        is FromSourceUnpivot -> this.expr.metas
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

/**
 * Represents a list constructor.
 * Note: `ExprNode` suffix in name disambiguates from [kotlin.collections.List].
 */
data class ListExprNode(
    val values: List<ExprNode>,
    override val metas: MetaContainer
) : ExprNode() {
    override val children: List<AstNode> = values
}

/** Represents a bag constructor. */
data class Bag(
    val bag: List<ExprNode>,
    override val metas: MetaContainer
) : ExprNode() {
    override val children: List<AstNode> = bag
}

/**
 * Represents a data type reference such as `INT` or `VARCHAR(50)`.
 */
data class DataType(
    val sqlDataType: SqlDataType,
    val args: List<Long>,
    override val metas: MetaContainer
) : HasMetas

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
 */
enum class NAryOp(val arityRange: IntRange, val symbol: String) {
    /** Add, but when arity is 1 then this just returns the value of its argument. */
    ADD(1..Int.MAX_VALUE, "+"),

    /** Subtract, but when when arity is 1, then this is assumed to be 0 - <arg1>. */
    SUB(1..Int.MAX_VALUE, "-"),

    /** Multiply */
    MUL(2..Int.MAX_VALUE, "*"),

    /** Divide */
    DIV(2..Int.MAX_VALUE, "/"),

    /** Modulus */
    MOD(2..Int.MAX_VALUE, "%"),

    /** Equivalent */
    EQ(2..Int.MAX_VALUE, "="),

    /** Less-than */
    LT(2..Int.MAX_VALUE, "<"),

    /** Less-than-or-equal.*/
    LTE(2..Int.MAX_VALUE, "<="),

    /** Greater-than. */
    GT(2..Int.MAX_VALUE, ">"),

    /** Greater-than-or-equal. */
    GTE(2..Int.MAX_VALUE, ">="),

    /** Not Equals. */
    NE(2..Int.MAX_VALUE, "<>"),

    /** Like. */
    LIKE(2..3, "like"),

    /** Between expression..i.e. `<expr1> BETWEEN <expr2> AND <expr3>`*/
    BETWEEN(3..3, "between"),

    /** IN expression, i.e. `<expr1> IN <containerExpr> ` */
    IN(2..Int.MAX_VALUE, "in"),

    /** Logical not */
    NOT(1..1, "not"),

    /** Logical `and`. */
    AND(2..Int.MAX_VALUE, "and"),

    /** Logical `or` */
    OR(2..Int.MAX_VALUE, "or"),

    /** String concatenation. */
    STRING_CONCAT(2..Int.MAX_VALUE, "||"),

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
    UNION_ALL(2..Int.MAX_VALUE, "union_all"),
    CONCAT(2..Int.MAX_VALUE, "concat");

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

    /** A FULL OUTER JOIN. */
    OUTER
}

/** Grouping strategy. */
enum class GroupingStrategy {
    /** Represents `GROUP BY` (no partial). */
    FULL,
    /** Represents `GROUP PARTIAL BY`.*/
    PARTIAL
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
    BAG("bag", 0..0);  // PartiQL

    companion object {
        private val DATA_TYPE_NAME_TO_TYPE_LOOKUP =
            SqlDataType.values()
                .map { Pair(it.typeName, it) }
                .toMap()

        fun forTypeName(typeName: String): SqlDataType? = DATA_TYPE_NAME_TO_TYPE_LOOKUP[typeName]
    }
}


