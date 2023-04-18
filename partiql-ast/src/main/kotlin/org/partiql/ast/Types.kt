package org.partiql.ast

import com.amazon.ionelement.api.IonElement
import org.partiql.ast.builder.ExceptBuilder
import org.partiql.ast.builder.ExprAggBuilder
import org.partiql.ast.builder.ExprBetweenBuilder
import org.partiql.ast.builder.ExprBinaryBuilder
import org.partiql.ast.builder.ExprCallBuilder
import org.partiql.ast.builder.ExprCanCastBuilder
import org.partiql.ast.builder.ExprCanLosslessCastBuilder
import org.partiql.ast.builder.ExprCastBuilder
import org.partiql.ast.builder.ExprCoalesceBuilder
import org.partiql.ast.builder.ExprCollectionBuilder
import org.partiql.ast.builder.ExprDateBuilder
import org.partiql.ast.builder.ExprIdentifierBuilder
import org.partiql.ast.builder.ExprInCollectionBuilder
import org.partiql.ast.builder.ExprIsTypeBuilder
import org.partiql.ast.builder.ExprLikeBuilder
import org.partiql.ast.builder.ExprLitBuilder
import org.partiql.ast.builder.ExprMatchBuilder
import org.partiql.ast.builder.ExprMissingBuilder
import org.partiql.ast.builder.ExprNullIfBuilder
import org.partiql.ast.builder.ExprParameterBuilder
import org.partiql.ast.builder.ExprPathBuilder
import org.partiql.ast.builder.ExprPathStepIndexBuilder
import org.partiql.ast.builder.ExprPathStepUnpivotBuilder
import org.partiql.ast.builder.ExprPathStepWildcardBuilder
import org.partiql.ast.builder.ExprSetBuilder
import org.partiql.ast.builder.ExprSfwBuilder
import org.partiql.ast.builder.ExprSwitchBranchBuilder
import org.partiql.ast.builder.ExprSwitchBuilder
import org.partiql.ast.builder.ExprTimeBuilder
import org.partiql.ast.builder.ExprTupleBuilder
import org.partiql.ast.builder.ExprTupleFieldBuilder
import org.partiql.ast.builder.ExprUnaryBuilder
import org.partiql.ast.builder.ExprWindowBuilder
import org.partiql.ast.builder.FromCollectionBuilder
import org.partiql.ast.builder.FromJoinBuilder
import org.partiql.ast.builder.GraphMatchBuilder
import org.partiql.ast.builder.GraphMatchPatternBuilder
import org.partiql.ast.builder.GraphMatchPatternPartEdgeBuilder
import org.partiql.ast.builder.GraphMatchPatternPartNodeBuilder
import org.partiql.ast.builder.GraphMatchQuantifierBuilder
import org.partiql.ast.builder.GraphMatchSelectorAllShortestBuilder
import org.partiql.ast.builder.GraphMatchSelectorAnyBuilder
import org.partiql.ast.builder.GraphMatchSelectorAnyKBuilder
import org.partiql.ast.builder.GraphMatchSelectorAnyShortestBuilder
import org.partiql.ast.builder.GraphMatchSelectorShortestKBuilder
import org.partiql.ast.builder.GraphMatchSelectorShortestKGroupBuilder
import org.partiql.ast.builder.GroupByBuilder
import org.partiql.ast.builder.GroupByKeyBuilder
import org.partiql.ast.builder.IntersectBuilder
import org.partiql.ast.builder.LetBindingBuilder
import org.partiql.ast.builder.LetBuilder
import org.partiql.ast.builder.OnConflictActionDoNothingBuilder
import org.partiql.ast.builder.OnConflictActionDoReplaceBuilder
import org.partiql.ast.builder.OnConflictActionDoUpdateBuilder
import org.partiql.ast.builder.OnConflictBuilder
import org.partiql.ast.builder.OrderByBuilder
import org.partiql.ast.builder.OrderBySortBuilder
import org.partiql.ast.builder.OverBuilder
import org.partiql.ast.builder.ReturningBuilder
import org.partiql.ast.builder.ReturningColumnBuilder
import org.partiql.ast.builder.ReturningColumnValueExpressionBuilder
import org.partiql.ast.builder.ReturningColumnValueWildcardBuilder
import org.partiql.ast.builder.SelectPivotBuilder
import org.partiql.ast.builder.SelectProjectBuilder
import org.partiql.ast.builder.SelectProjectItemAllBuilder
import org.partiql.ast.builder.SelectProjectItemVarBuilder
import org.partiql.ast.builder.SelectStarBuilder
import org.partiql.ast.builder.SelectValueBuilder
import org.partiql.ast.builder.StatementDdlCreateIndexBuilder
import org.partiql.ast.builder.StatementDdlCreateTableBuilder
import org.partiql.ast.builder.StatementDdlDropIndexBuilder
import org.partiql.ast.builder.StatementDdlDropTableBuilder
import org.partiql.ast.builder.StatementDmlDeleteBuilder
import org.partiql.ast.builder.StatementDmlInsertBuilder
import org.partiql.ast.builder.StatementDmlInsertValueBuilder
import org.partiql.ast.builder.StatementDmlRemoveBuilder
import org.partiql.ast.builder.StatementDmlSetAssignmentBuilder
import org.partiql.ast.builder.StatementDmlSetBuilder
import org.partiql.ast.builder.StatementExecBuilder
import org.partiql.ast.builder.StatementExplainBuilder
import org.partiql.ast.builder.StatementExplainTargetDomainBuilder
import org.partiql.ast.builder.StatementQueryBuilder
import org.partiql.ast.builder.TableDefinitionBuilder
import org.partiql.ast.builder.TableDefinitionColumnBuilder
import org.partiql.ast.builder.TableDefinitionColumnConstraintBodyCheckBuilder
import org.partiql.ast.builder.TableDefinitionColumnConstraintBodyNotNullBuilder
import org.partiql.ast.builder.TableDefinitionColumnConstraintBodyNullableBuilder
import org.partiql.ast.builder.TableDefinitionColumnConstraintBuilder
import org.partiql.ast.builder.TypeBuilder
import org.partiql.ast.builder.UnionBuilder
import org.partiql.ast.visitor.AstVisitor

public abstract class AstNode {
    public abstract val id: Int

    public open val children: List<AstNode> = emptyList()

    public abstract fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R
}

public sealed class Statement : AstNode() {
    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = when (this) {
        is Query -> visitor.visitStatementQuery(this, ctx)
        is DML -> visitor.visitStatementDML(this, ctx)
        is DDL -> visitor.visitStatementDDL(this, ctx)
        is Exec -> visitor.visitStatementExec(this, ctx)
        is Explain -> visitor.visitStatementExplain(this, ctx)
    }

    public data class Query(
        public override val id: Int,
        public val expr: Expr
    ) : Statement() {
        public override val children: List<AstNode> by lazy {
            val kids = mutableListOf<AstNode?>()
            kids.add(expr)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitStatementQuery(this, ctx)

        public companion object {
            @JvmStatic
            public fun builder(): StatementQueryBuilder = StatementQueryBuilder()
        }
    }

    public sealed class DML : Statement() {
        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = when (this) {
            is Insert -> visitor.visitStatementDMLInsert(this, ctx)
            is InsertValue -> visitor.visitStatementDMLInsertValue(this, ctx)
            is Set -> visitor.visitStatementDMLSet(this, ctx)
            is Remove -> visitor.visitStatementDMLRemove(this, ctx)
            is Delete -> visitor.visitStatementDMLDelete(this, ctx)
        }

        public data class Insert(
            public override val id: Int,
            public val target: Expr,
            public val values: Expr,
            public val onConflict: OnConflict.Action
        ) : DML() {
            public override val children: List<AstNode> by lazy {
                val kids = mutableListOf<AstNode?>()
                kids.add(target)
                kids.add(values)
                kids.add(onConflict)
                kids.filterNotNull()
            }

            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                visitor.visitStatementDMLInsert(this, ctx)

            public companion object {
                @JvmStatic
                public fun builder(): StatementDmlInsertBuilder = StatementDmlInsertBuilder()
            }
        }

        public data class InsertValue(
            public override val id: Int,
            public val target: Expr,
            public val `value`: Expr,
            public val atAlias: Expr,
            public val index: Expr?,
            public val onConflict: OnConflict
        ) : DML() {
            public override val children: List<AstNode> by lazy {
                val kids = mutableListOf<AstNode?>()
                kids.add(target)
                kids.add(value)
                kids.add(atAlias)
                kids.add(index)
                kids.add(onConflict)
                kids.filterNotNull()
            }

            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                visitor.visitStatementDMLInsertValue(this, ctx)

            public companion object {
                @JvmStatic
                public fun builder(): StatementDmlInsertValueBuilder = StatementDmlInsertValueBuilder()
            }
        }

        public data class Set(
            public override val id: Int,
            public val assignments: List<Assignment>
        ) : DML() {
            public override val children: List<AstNode> by lazy {
                val kids = mutableListOf<AstNode?>()
                kids.addAll(assignments)
                kids.filterNotNull()
            }

            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                visitor.visitStatementDMLSet(this, ctx)

            public data class Assignment(
                public override val id: Int,
                public val target: Expr.Path,
                public val `value`: Expr
            ) : AstNode() {
                public override val children: List<AstNode> by lazy {
                    val kids = mutableListOf<AstNode?>()
                    kids.add(target)
                    kids.add(value)
                    kids.filterNotNull()
                }

                public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                    visitor.visitStatementDMLSetAssignment(this, ctx)

                public companion object {
                    @JvmStatic
                    public fun builder(): StatementDmlSetAssignmentBuilder =
                        StatementDmlSetAssignmentBuilder()
                }
            }

            public companion object {
                @JvmStatic
                public fun builder(): StatementDmlSetBuilder = StatementDmlSetBuilder()
            }
        }

        public data class Remove(
            public override val id: Int,
            public val target: Expr.Path
        ) : DML() {
            public override val children: List<AstNode> by lazy {
                val kids = mutableListOf<AstNode?>()
                kids.add(target)
                kids.filterNotNull()
            }

            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                visitor.visitStatementDMLRemove(this, ctx)

            public companion object {
                @JvmStatic
                public fun builder(): StatementDmlRemoveBuilder = StatementDmlRemoveBuilder()
            }
        }

        public data class Delete(
            public override val id: Int,
            public val from: From,
            public val `where`: Expr?,
            public val returning: Returning
        ) : DML() {
            public override val children: List<AstNode> by lazy {
                val kids = mutableListOf<AstNode?>()
                kids.add(from)
                kids.add(where)
                kids.add(returning)
                kids.filterNotNull()
            }

            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                visitor.visitStatementDMLDelete(this, ctx)

            public companion object {
                @JvmStatic
                public fun builder(): StatementDmlDeleteBuilder = StatementDmlDeleteBuilder()
            }
        }
    }

    public sealed class DDL : Statement() {
        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = when (this) {
            is CreateTable -> visitor.visitStatementDDLCreateTable(this, ctx)
            is CreateIndex -> visitor.visitStatementDDLCreateIndex(this, ctx)
            is DropTable -> visitor.visitStatementDDLDropTable(this, ctx)
            is DropIndex -> visitor.visitStatementDDLDropIndex(this, ctx)
        }

        public data class CreateTable(
            public override val id: Int,
            public val name: String,
            public val definition: TableDefinition?
        ) : DDL() {
            public override val children: List<AstNode> by lazy {
                val kids = mutableListOf<AstNode?>()
                kids.add(definition)
                kids.filterNotNull()
            }

            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                visitor.visitStatementDDLCreateTable(this, ctx)

            public companion object {
                @JvmStatic
                public fun builder(): StatementDdlCreateTableBuilder = StatementDdlCreateTableBuilder()
            }
        }

        public data class CreateIndex(
            public override val id: Int,
            public val name: String,
            public val fields: List<Expr>
        ) : DDL() {
            public override val children: List<AstNode> by lazy {
                val kids = mutableListOf<AstNode?>()
                kids.addAll(fields)
                kids.filterNotNull()
            }

            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                visitor.visitStatementDDLCreateIndex(this, ctx)

            public companion object {
                @JvmStatic
                public fun builder(): StatementDdlCreateIndexBuilder = StatementDdlCreateIndexBuilder()
            }
        }

        public data class DropTable(
            public override val id: Int,
            public val identifier: Expr.Identifier
        ) : DDL() {
            public override val children: List<AstNode> by lazy {
                val kids = mutableListOf<AstNode?>()
                kids.add(identifier)
                kids.filterNotNull()
            }

            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                visitor.visitStatementDDLDropTable(this, ctx)

            public companion object {
                @JvmStatic
                public fun builder(): StatementDdlDropTableBuilder = StatementDdlDropTableBuilder()
            }
        }

        public data class DropIndex(
            public override val id: Int,
            public val table: Expr.Identifier,
            public val keys: Expr.Identifier
        ) : DDL() {
            public override val children: List<AstNode> by lazy {
                val kids = mutableListOf<AstNode?>()
                kids.add(table)
                kids.add(keys)
                kids.filterNotNull()
            }

            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                visitor.visitStatementDDLDropIndex(this, ctx)

            public companion object {
                @JvmStatic
                public fun builder(): StatementDdlDropIndexBuilder = StatementDdlDropIndexBuilder()
            }
        }
    }

    public data class Exec(
        public override val id: Int,
        public val procedure: String,
        public val args: List<Expr>
    ) : Statement() {
        public override val children: List<AstNode> by lazy {
            val kids = mutableListOf<AstNode?>()
            kids.addAll(args)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitStatementExec(this, ctx)

        public companion object {
            @JvmStatic
            public fun builder(): StatementExecBuilder = StatementExecBuilder()
        }
    }

    public data class Explain(
        public override val id: Int,
        public val target: Target
    ) : Statement() {
        public override val children: List<AstNode> by lazy {
            val kids = mutableListOf<AstNode?>()
            kids.add(target)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitStatementExplain(this, ctx)

        public sealed class Target : AstNode() {
            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = when (this) {
                is Domain -> visitor.visitStatementExplainTargetDomain(this, ctx)
            }

            public data class Domain(
                public override val id: Int,
                public val statement: Statement,
                public val type: String?,
                public val format: String?
            ) : Target() {
                public override val children: List<AstNode> by lazy {
                    val kids = mutableListOf<AstNode?>()
                    kids.add(statement)
                    kids.filterNotNull()
                }

                public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                    visitor.visitStatementExplainTargetDomain(this, ctx)

                public companion object {
                    @JvmStatic
                    public fun builder(): StatementExplainTargetDomainBuilder =
                        StatementExplainTargetDomainBuilder()
                }
            }
        }

        public companion object {
            @JvmStatic
            public fun builder(): StatementExplainBuilder = StatementExplainBuilder()
        }
    }
}

public data class Type(
    public override val id: Int,
    public val identifier: String,
    public val parameters: List<IonElement>
) : AstNode() {
    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = visitor.visitType(
        this,
        ctx
    )

    public companion object {
        @JvmStatic
        public fun builder(): TypeBuilder = TypeBuilder()
    }
}

public sealed class Expr : AstNode() {
    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = when (this) {
        is Missing -> visitor.visitExprMissing(this, ctx)
        is Lit -> visitor.visitExprLit(this, ctx)
        is Identifier -> visitor.visitExprIdentifier(this, ctx)
        is Path -> visitor.visitExprPath(this, ctx)
        is Call -> visitor.visitExprCall(this, ctx)
        is Agg -> visitor.visitExprAgg(this, ctx)
        is Parameter -> visitor.visitExprParameter(this, ctx)
        is Unary -> visitor.visitExprUnary(this, ctx)
        is Binary -> visitor.visitExprBinary(this, ctx)
        is Collection -> visitor.visitExprCollection(this, ctx)
        is Tuple -> visitor.visitExprTuple(this, ctx)
        is Date -> visitor.visitExprDate(this, ctx)
        is Time -> visitor.visitExprTime(this, ctx)
        is Like -> visitor.visitExprLike(this, ctx)
        is Between -> visitor.visitExprBetween(this, ctx)
        is InCollection -> visitor.visitExprInCollection(this, ctx)
        is IsType -> visitor.visitExprIsType(this, ctx)
        is Switch -> visitor.visitExprSwitch(this, ctx)
        is Coalesce -> visitor.visitExprCoalesce(this, ctx)
        is NullIf -> visitor.visitExprNullIf(this, ctx)
        is Cast -> visitor.visitExprCast(this, ctx)
        is CanCast -> visitor.visitExprCanCast(this, ctx)
        is CanLosslessCast -> visitor.visitExprCanLosslessCast(this, ctx)
        is Set -> visitor.visitExprSet(this, ctx)
        is SFW -> visitor.visitExprSFW(this, ctx)
        is Match -> visitor.visitExprMatch(this, ctx)
        is Window -> visitor.visitExprWindow(this, ctx)
    }

    public data class Missing(
        public override val id: Int
    ) : Expr() {
        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprMissing(this, ctx)

        public companion object {
            @JvmStatic
            public fun builder(): ExprMissingBuilder = ExprMissingBuilder()
        }
    }

    public data class Lit(
        public override val id: Int,
        public val `value`: IonElement
    ) : Expr() {
        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprLit(this, ctx)

        public companion object {
            @JvmStatic
            public fun builder(): ExprLitBuilder = ExprLitBuilder()
        }
    }

    public data class Identifier(
        public override val id: Int,
        public val name: String,
        public val case: Case,
        public val scope: Scope
    ) : Expr() {
        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprIdentifier(this, ctx)

        public enum class Scope {
            UNQUALIFIED,
            LOCALS_FIRST,
        }

        public companion object {
            @JvmStatic
            public fun builder(): ExprIdentifierBuilder = ExprIdentifierBuilder()
        }
    }

    public data class Path(
        public override val id: Int,
        public val root: Expr,
        public val steps: List<Step>
    ) : Expr() {
        public override val children: List<AstNode> by lazy {
            val kids = mutableListOf<AstNode?>()
            kids.add(root)
            kids.addAll(steps)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprPath(this, ctx)

        public sealed class Step : AstNode() {
            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = when (this) {
                is Index -> visitor.visitExprPathStepIndex(this, ctx)
                is Wildcard -> visitor.visitExprPathStepWildcard(this, ctx)
                is Unpivot -> visitor.visitExprPathStepUnpivot(this, ctx)
            }

            public data class Index(
                public override val id: Int,
                public val key: Expr,
                public val case: Case
            ) : Step() {
                public override val children: List<AstNode> by lazy {
                    val kids = mutableListOf<AstNode?>()
                    kids.add(key)
                    kids.filterNotNull()
                }

                public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                    visitor.visitExprPathStepIndex(this, ctx)

                public companion object {
                    @JvmStatic
                    public fun builder(): ExprPathStepIndexBuilder = ExprPathStepIndexBuilder()
                }
            }

            public data class Wildcard(
                public override val id: Int
            ) : Step() {
                public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                    visitor.visitExprPathStepWildcard(this, ctx)

                public companion object {
                    @JvmStatic
                    public fun builder(): ExprPathStepWildcardBuilder = ExprPathStepWildcardBuilder()
                }
            }

            public data class Unpivot(
                public override val id: Int
            ) : Step() {
                public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                    visitor.visitExprPathStepUnpivot(this, ctx)

                public companion object {
                    @JvmStatic
                    public fun builder(): ExprPathStepUnpivotBuilder = ExprPathStepUnpivotBuilder()
                }
            }
        }

        public companion object {
            @JvmStatic
            public fun builder(): ExprPathBuilder = ExprPathBuilder()
        }
    }

    public data class Call(
        public override val id: Int,
        public val function: String,
        public val args: List<Expr>
    ) : Expr() {
        public override val children: List<AstNode> by lazy {
            val kids = mutableListOf<AstNode?>()
            kids.addAll(args)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprCall(this, ctx)

        public companion object {
            @JvmStatic
            public fun builder(): ExprCallBuilder = ExprCallBuilder()
        }
    }

    public data class Agg(
        public override val id: Int,
        public val function: String,
        public val args: List<Expr>,
        public val quantifier: SetQuantifier
    ) : Expr() {
        public override val children: List<AstNode> by lazy {
            val kids = mutableListOf<AstNode?>()
            kids.addAll(args)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprAgg(this, ctx)

        public companion object {
            @JvmStatic
            public fun builder(): ExprAggBuilder = ExprAggBuilder()
        }
    }

    public data class Parameter(
        public override val id: Int,
        public val index: Int
    ) : Expr() {
        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprParameter(this, ctx)

        public companion object {
            @JvmStatic
            public fun builder(): ExprParameterBuilder = ExprParameterBuilder()
        }
    }

    public data class Unary(
        public override val id: Int,
        public val op: Op,
        public val expr: Expr
    ) : Expr() {
        public override val children: List<AstNode> by lazy {
            val kids = mutableListOf<AstNode?>()
            kids.add(expr)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprUnary(this, ctx)

        public enum class Op {
            NOT,
            POS,
            NEG,
        }

        public companion object {
            @JvmStatic
            public fun builder(): ExprUnaryBuilder = ExprUnaryBuilder()
        }
    }

    public data class Binary(
        public override val id: Int,
        public val op: Op,
        public val lhs: Expr,
        public val rhs: Expr
    ) : Expr() {
        public override val children: List<AstNode> by lazy {
            val kids = mutableListOf<AstNode?>()
            kids.add(lhs)
            kids.add(rhs)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprBinary(this, ctx)

        public enum class Op {
            PLUS,
            MINUS,
            TIMES,
            DIVIDE,
            MODULO,
            CONCAT,
            AND,
            OR,
            EQ,
            NE,
            GT,
            GTE,
            LT,
            LTE,
        }

        public companion object {
            @JvmStatic
            public fun builder(): ExprBinaryBuilder = ExprBinaryBuilder()
        }
    }

    public data class Collection(
        public override val id: Int,
        public val type: Type,
        public val values: List<Expr>
    ) : Expr() {
        public override val children: List<AstNode> by lazy {
            val kids = mutableListOf<AstNode?>()
            kids.addAll(values)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprCollection(this, ctx)

        public enum class Type {
            BAG,
            ARRAY,
            LIST,
            SEXP,
        }

        public companion object {
            @JvmStatic
            public fun builder(): ExprCollectionBuilder = ExprCollectionBuilder()
        }
    }

    public data class Tuple(
        public override val id: Int,
        public val fields: List<Field>
    ) : Expr() {
        public override val children: List<AstNode> by lazy {
            val kids = mutableListOf<AstNode?>()
            kids.addAll(fields)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprTuple(this, ctx)

        public data class Field(
            public override val id: Int,
            public val name: Expr,
            public val `value`: Expr
        ) : AstNode() {
            public override val children: List<AstNode> by lazy {
                val kids = mutableListOf<AstNode?>()
                kids.add(name)
                kids.add(value)
                kids.filterNotNull()
            }

            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                visitor.visitExprTupleField(this, ctx)

            public companion object {
                @JvmStatic
                public fun builder(): ExprTupleFieldBuilder = ExprTupleFieldBuilder()
            }
        }

        public companion object {
            @JvmStatic
            public fun builder(): ExprTupleBuilder = ExprTupleBuilder()
        }
    }

    public data class Date(
        public override val id: Int,
        public val year: Long,
        public val month: Long,
        public val day: Long
    ) : Expr() {
        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprDate(this, ctx)

        public companion object {
            @JvmStatic
            public fun builder(): ExprDateBuilder = ExprDateBuilder()
        }
    }

    public data class Time(
        public override val id: Int,
        public val hour: Long,
        public val minute: Long,
        public val second: Long,
        public val nano: Long,
        public val precision: Long,
        public val tzOffsetMinutes: Long?
    ) : Expr() {
        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprTime(this, ctx)

        public companion object {
            @JvmStatic
            public fun builder(): ExprTimeBuilder = ExprTimeBuilder()
        }
    }

    public data class Like(
        public override val id: Int,
        public val `value`: Expr,
        public val pattern: Expr,
        public val escape: Expr?
    ) : Expr() {
        public override val children: List<AstNode> by lazy {
            val kids = mutableListOf<AstNode?>()
            kids.add(value)
            kids.add(pattern)
            kids.add(escape)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprLike(this, ctx)

        public companion object {
            @JvmStatic
            public fun builder(): ExprLikeBuilder = ExprLikeBuilder()
        }
    }

    public data class Between(
        public override val id: Int,
        public val `value`: Expr,
        public val from: Expr,
        public val to: Expr
    ) : Expr() {
        public override val children: List<AstNode> by lazy {
            val kids = mutableListOf<AstNode?>()
            kids.add(value)
            kids.add(from)
            kids.add(to)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprBetween(this, ctx)

        public companion object {
            @JvmStatic
            public fun builder(): ExprBetweenBuilder = ExprBetweenBuilder()
        }
    }

    public data class InCollection(
        public override val id: Int,
        public val lhs: Expr,
        public val rhs: Expr
    ) : Expr() {
        public override val children: List<AstNode> by lazy {
            val kids = mutableListOf<AstNode?>()
            kids.add(lhs)
            kids.add(rhs)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprInCollection(this, ctx)

        public companion object {
            @JvmStatic
            public fun builder(): ExprInCollectionBuilder = ExprInCollectionBuilder()
        }
    }

    public data class IsType(
        public override val id: Int,
        public val `value`: Expr,
        public val type: Type
    ) : Expr() {
        public override val children: List<AstNode> by lazy {
            val kids = mutableListOf<AstNode?>()
            kids.add(value)
            kids.add(type)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprIsType(this, ctx)

        public companion object {
            @JvmStatic
            public fun builder(): ExprIsTypeBuilder = ExprIsTypeBuilder()
        }
    }

    public data class Switch(
        public override val id: Int,
        public val expr: Expr?,
        public val branches: List<Branch>,
        public val default: Expr?
    ) : Expr() {
        public override val children: List<AstNode> by lazy {
            val kids = mutableListOf<AstNode?>()
            kids.add(expr)
            kids.addAll(branches)
            kids.add(default)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprSwitch(this, ctx)

        public data class Branch(
            public override val id: Int,
            public val condition: Expr,
            public val expr: Expr
        ) : AstNode() {
            public override val children: List<AstNode> by lazy {
                val kids = mutableListOf<AstNode?>()
                kids.add(condition)
                kids.add(expr)
                kids.filterNotNull()
            }

            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                visitor.visitExprSwitchBranch(this, ctx)

            public companion object {
                @JvmStatic
                public fun builder(): ExprSwitchBranchBuilder = ExprSwitchBranchBuilder()
            }
        }

        public companion object {
            @JvmStatic
            public fun builder(): ExprSwitchBuilder = ExprSwitchBuilder()
        }
    }

    public data class Coalesce(
        public override val id: Int,
        public val args: List<Expr>
    ) : Expr() {
        public override val children: List<AstNode> by lazy {
            val kids = mutableListOf<AstNode?>()
            kids.addAll(args)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprCoalesce(this, ctx)

        public companion object {
            @JvmStatic
            public fun builder(): ExprCoalesceBuilder = ExprCoalesceBuilder()
        }
    }

    public data class NullIf(
        public override val id: Int,
        public val expr0: Expr,
        public val expr1: Expr
    ) : Expr() {
        public override val children: List<AstNode> by lazy {
            val kids = mutableListOf<AstNode?>()
            kids.add(expr0)
            kids.add(expr1)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprNullIf(this, ctx)

        public companion object {
            @JvmStatic
            public fun builder(): ExprNullIfBuilder = ExprNullIfBuilder()
        }
    }

    public data class Cast(
        public override val id: Int,
        public val `value`: Expr,
        public val asType: Type
    ) : Expr() {
        public override val children: List<AstNode> by lazy {
            val kids = mutableListOf<AstNode?>()
            kids.add(value)
            kids.add(asType)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprCast(this, ctx)

        public companion object {
            @JvmStatic
            public fun builder(): ExprCastBuilder = ExprCastBuilder()
        }
    }

    public data class CanCast(
        public override val id: Int,
        public val `value`: Expr,
        public val asType: Type
    ) : Expr() {
        public override val children: List<AstNode> by lazy {
            val kids = mutableListOf<AstNode?>()
            kids.add(value)
            kids.add(asType)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprCanCast(this, ctx)

        public companion object {
            @JvmStatic
            public fun builder(): ExprCanCastBuilder = ExprCanCastBuilder()
        }
    }

    public data class CanLosslessCast(
        public override val id: Int,
        public val `value`: Expr,
        public val asType: Type
    ) : Expr() {
        public override val children: List<AstNode> by lazy {
            val kids = mutableListOf<AstNode?>()
            kids.add(value)
            kids.add(asType)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprCanLosslessCast(this, ctx)

        public companion object {
            @JvmStatic
            public fun builder(): ExprCanLosslessCastBuilder = ExprCanLosslessCastBuilder()
        }
    }

    public data class Set(
        public override val id: Int,
        public val op: Op,
        public val quantifier: SetQuantifier,
        public val outer: Boolean,
        public val lhs: Expr,
        public val rhs: Expr
    ) : Expr() {
        public override val children: List<AstNode> by lazy {
            val kids = mutableListOf<AstNode?>()
            kids.add(lhs)
            kids.add(rhs)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprSet(this, ctx)

        public enum class Op {
            UNION,
            INTERSECT,
            EXCEPT,
        }

        public companion object {
            @JvmStatic
            public fun builder(): ExprSetBuilder = ExprSetBuilder()
        }
    }

    public data class SFW(
        public override val id: Int,
        public val select: Select,
        public val from: From,
        public val let: Let?,
        public val `where`: Expr?,
        public val groupBy: GroupBy?,
        public val having: Expr?,
        public val orderBy: OrderBy?,
        public val limit: Expr?,
        public val offset: Expr?
    ) : Expr() {
        public override val children: List<AstNode> by lazy {
            val kids = mutableListOf<AstNode?>()
            kids.add(select)
            kids.add(from)
            kids.add(let)
            kids.add(where)
            kids.add(groupBy)
            kids.add(having)
            kids.add(orderBy)
            kids.add(limit)
            kids.add(offset)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprSFW(this, ctx)

        public companion object {
            @JvmStatic
            public fun builder(): ExprSfwBuilder = ExprSfwBuilder()
        }
    }

    public data class Match(
        public override val id: Int,
        public val expr: Expr,
        public val pattern: GraphMatch
    ) : Expr() {
        public override val children: List<AstNode> by lazy {
            val kids = mutableListOf<AstNode?>()
            kids.add(expr)
            kids.add(pattern)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprMatch(this, ctx)

        public companion object {
            @JvmStatic
            public fun builder(): ExprMatchBuilder = ExprMatchBuilder()
        }
    }

    public data class Window(
        public override val id: Int,
        public val function: String,
        public val over: Over,
        public val args: List<Expr>
    ) : Expr() {
        public override val children: List<AstNode> by lazy {
            val kids = mutableListOf<AstNode?>()
            kids.add(over)
            kids.addAll(args)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprWindow(this, ctx)

        public companion object {
            @JvmStatic
            public fun builder(): ExprWindowBuilder = ExprWindowBuilder()
        }
    }
}

public sealed class Select : AstNode() {
    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = when (this) {
        is Star -> visitor.visitSelectStar(this, ctx)
        is Project -> visitor.visitSelectProject(this, ctx)
        is Pivot -> visitor.visitSelectPivot(this, ctx)
        is Value -> visitor.visitSelectValue(this, ctx)
    }

    public data class Star(
        public override val id: Int,
        public val quantifier: SetQuantifier
    ) : Select() {
        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitSelectStar(this, ctx)

        public companion object {
            @JvmStatic
            public fun builder(): SelectStarBuilder = SelectStarBuilder()
        }
    }

    public data class Project(
        public override val id: Int,
        public val quantifier: SetQuantifier,
        public val items: List<Item>
    ) : Select() {
        public override val children: List<AstNode> by lazy {
            val kids = mutableListOf<AstNode?>()
            kids.addAll(items)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitSelectProject(this, ctx)

        public sealed class Item : AstNode() {
            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = when (this) {
                is All -> visitor.visitSelectProjectItemAll(this, ctx)
                is Var -> visitor.visitSelectProjectItemVar(this, ctx)
            }

            public data class All(
                public override val id: Int,
                public val expr: Expr
            ) : Item() {
                public override val children: List<AstNode> by lazy {
                    val kids = mutableListOf<AstNode?>()
                    kids.add(expr)
                    kids.filterNotNull()
                }

                public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                    visitor.visitSelectProjectItemAll(this, ctx)

                public companion object {
                    @JvmStatic
                    public fun builder(): SelectProjectItemAllBuilder = SelectProjectItemAllBuilder()
                }
            }

            public data class Var(
                public override val id: Int,
                public val expr: Expr,
                public val asAlias: String?
            ) : Item() {
                public override val children: List<AstNode> by lazy {
                    val kids = mutableListOf<AstNode?>()
                    kids.add(expr)
                    kids.filterNotNull()
                }

                public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                    visitor.visitSelectProjectItemVar(this, ctx)

                public companion object {
                    @JvmStatic
                    public fun builder(): SelectProjectItemVarBuilder = SelectProjectItemVarBuilder()
                }
            }
        }

        public companion object {
            @JvmStatic
            public fun builder(): SelectProjectBuilder = SelectProjectBuilder()
        }
    }

    public data class Pivot(
        public override val id: Int,
        public val `value`: Expr,
        public val key: Expr
    ) : Select() {
        public override val children: List<AstNode> by lazy {
            val kids = mutableListOf<AstNode?>()
            kids.add(value)
            kids.add(key)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitSelectPivot(this, ctx)

        public companion object {
            @JvmStatic
            public fun builder(): SelectPivotBuilder = SelectPivotBuilder()
        }
    }

    public data class Value(
        public override val id: Int,
        public val quantifier: SetQuantifier,
        public val `constructor`: Expr
    ) : Select() {
        public override val children: List<AstNode> by lazy {
            val kids = mutableListOf<AstNode?>()
            kids.add(constructor)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitSelectValue(this, ctx)

        public companion object {
            @JvmStatic
            public fun builder(): SelectValueBuilder = SelectValueBuilder()
        }
    }
}

public sealed class From : AstNode() {
    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = when (this) {
        is Collection -> visitor.visitFromCollection(this, ctx)
        is Join -> visitor.visitFromJoin(this, ctx)
    }

    public data class Collection(
        public override val id: Int,
        public val expr: Expr,
        public val unpivot: Boolean?,
        public val asAlias: String?,
        public val atAlias: String?,
        public val byAlias: String?
    ) : From() {
        public override val children: List<AstNode> by lazy {
            val kids = mutableListOf<AstNode?>()
            kids.add(expr)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitFromCollection(this, ctx)

        public companion object {
            @JvmStatic
            public fun builder(): FromCollectionBuilder = FromCollectionBuilder()
        }
    }

    public data class Join(
        public override val id: Int,
        public val type: Type,
        public val condition: Expr?,
        public val lhs: From,
        public val rhs: From
    ) : From() {
        public override val children: List<AstNode> by lazy {
            val kids = mutableListOf<AstNode?>()
            kids.add(condition)
            kids.add(lhs)
            kids.add(rhs)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitFromJoin(this, ctx)

        public enum class Type {
            INNER,
            LEFT,
            RIGHT,
            FULL,
        }

        public companion object {
            @JvmStatic
            public fun builder(): FromJoinBuilder = FromJoinBuilder()
        }
    }
}

public data class Let(
    public override val id: Int,
    public val bindings: List<Binding>
) : AstNode() {
    public override val children: List<AstNode> by lazy {
        val kids = mutableListOf<AstNode?>()
        kids.addAll(bindings)
        kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = visitor.visitLet(
        this,
        ctx
    )

    public data class Binding(
        public override val id: Int,
        public val expr: Expr,
        public val asAlias: String
    ) : AstNode() {
        public override val children: List<AstNode> by lazy {
            val kids = mutableListOf<AstNode?>()
            kids.add(expr)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitLetBinding(this, ctx)

        public companion object {
            @JvmStatic
            public fun builder(): LetBindingBuilder = LetBindingBuilder()
        }
    }

    public companion object {
        @JvmStatic
        public fun builder(): LetBuilder = LetBuilder()
    }
}

public data class GroupBy(
    public override val id: Int,
    public val strategy: Strategy,
    public val keys: List<Key>,
    public val asAlias: String?
) : AstNode() {
    public override val children: List<AstNode> by lazy {
        val kids = mutableListOf<AstNode?>()
        kids.addAll(keys)
        kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
        visitor.visitGroupBy(this, ctx)

    public enum class Strategy {
        FULL,
        PARTIAL,
    }

    public data class Key(
        public override val id: Int,
        public val expr: Expr,
        public val asAlias: String?
    ) : AstNode() {
        public override val children: List<AstNode> by lazy {
            val kids = mutableListOf<AstNode?>()
            kids.add(expr)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitGroupByKey(this, ctx)

        public companion object {
            @JvmStatic
            public fun builder(): GroupByKeyBuilder = GroupByKeyBuilder()
        }
    }

    public companion object {
        @JvmStatic
        public fun builder(): GroupByBuilder = GroupByBuilder()
    }
}

public data class OrderBy(
    public override val id: Int,
    public val sorts: List<Sort>
) : AstNode() {
    public override val children: List<AstNode> by lazy {
        val kids = mutableListOf<AstNode?>()
        kids.addAll(sorts)
        kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
        visitor.visitOrderBy(this, ctx)

    public data class Sort(
        public override val id: Int,
        public val expr: Expr,
        public val dir: Dir,
        public val nulls: Nulls
    ) : AstNode() {
        public override val children: List<AstNode> by lazy {
            val kids = mutableListOf<AstNode?>()
            kids.add(expr)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitOrderBySort(this, ctx)

        public enum class Dir {
            ASC,
            DESC,
        }

        public enum class Nulls {
            FIRST,
            LAST,
        }

        public companion object {
            @JvmStatic
            public fun builder(): OrderBySortBuilder = OrderBySortBuilder()
        }
    }

    public companion object {
        @JvmStatic
        public fun builder(): OrderByBuilder = OrderByBuilder()
    }
}

public data class Union(
    public override val id: Int,
    public val quantifier: SetQuantifier,
    public val lhs: Expr.SFW,
    public val rhs: Expr.SFW
) : AstNode() {
    public override val children: List<AstNode> by lazy {
        val kids = mutableListOf<AstNode?>()
        kids.add(lhs)
        kids.add(rhs)
        kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = visitor.visitUnion(
        this,
        ctx
    )

    public companion object {
        @JvmStatic
        public fun builder(): UnionBuilder = UnionBuilder()
    }
}

public data class Intersect(
    public override val id: Int,
    public val quantifier: SetQuantifier,
    public val lhs: Expr.SFW,
    public val rhs: Expr.SFW
) : AstNode() {
    public override val children: List<AstNode> by lazy {
        val kids = mutableListOf<AstNode?>()
        kids.add(lhs)
        kids.add(rhs)
        kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
        visitor.visitIntersect(this, ctx)

    public companion object {
        @JvmStatic
        public fun builder(): IntersectBuilder = IntersectBuilder()
    }
}

public data class Except(
    public override val id: Int,
    public val quantifier: SetQuantifier,
    public val lhs: Expr.SFW,
    public val rhs: Expr.SFW
) : AstNode() {
    public override val children: List<AstNode> by lazy {
        val kids = mutableListOf<AstNode?>()
        kids.add(lhs)
        kids.add(rhs)
        kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
        visitor.visitExcept(this, ctx)

    public companion object {
        @JvmStatic
        public fun builder(): ExceptBuilder = ExceptBuilder()
    }
}

public data class GraphMatch(
    public override val id: Int,
    public val patterns: List<Pattern>,
    public val selector: Selector?
) : AstNode() {
    public override val children: List<AstNode> by lazy {
        val kids = mutableListOf<AstNode?>()
        kids.addAll(patterns)
        kids.add(selector)
        kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
        visitor.visitGraphMatch(this, ctx)

    public enum class Direction {
        LEFT,
        UNDIRECTED,
        RIGHT,
        LEFT_OR_UNDIRECTED,
        UNDIRECTED_OR_RIGHT,
        LEFT_OR_RIGHT,
        LEFT_UNDIRECTED_OR_RIGHT,
    }

    public enum class Restrictor {
        TRAIL,
        ACYCLIC,
        SIMPLE,
    }

    public data class Pattern(
        public override val id: Int,
        public val restrictor: Restrictor?,
        public val prefilter: Expr?,
        public val variable: String?,
        public val quantifier: Quantifier?,
        public val parts: List<Part>
    ) : AstNode() {
        public override val children: List<AstNode> by lazy {
            val kids = mutableListOf<AstNode?>()
            kids.add(prefilter)
            kids.add(quantifier)
            kids.addAll(parts)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitGraphMatchPattern(this, ctx)

        public sealed class Part : AstNode() {
            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = when (this) {
                is Node -> visitor.visitGraphMatchPatternPartNode(this, ctx)
                is Edge -> visitor.visitGraphMatchPatternPartEdge(this, ctx)
            }

            public data class Node(
                public override val id: Int,
                public val prefilter: Expr?,
                public val variable: String?,
                public val label: List<String>
            ) : Part() {
                public override val children: List<AstNode> by lazy {
                    val kids = mutableListOf<AstNode?>()
                    kids.add(prefilter)
                    kids.filterNotNull()
                }

                public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                    visitor.visitGraphMatchPatternPartNode(this, ctx)

                public companion object {
                    @JvmStatic
                    public fun builder(): GraphMatchPatternPartNodeBuilder =
                        GraphMatchPatternPartNodeBuilder()
                }
            }

            public data class Edge(
                public override val id: Int,
                public val direction: Direction,
                public val quantifier: Quantifier?,
                public val prefilter: Expr?,
                public val variable: String?,
                public val label: List<String>
            ) : Part() {
                public override val children: List<AstNode> by lazy {
                    val kids = mutableListOf<AstNode?>()
                    kids.add(quantifier)
                    kids.add(prefilter)
                    kids.filterNotNull()
                }

                public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                    visitor.visitGraphMatchPatternPartEdge(this, ctx)

                public companion object {
                    @JvmStatic
                    public fun builder(): GraphMatchPatternPartEdgeBuilder =
                        GraphMatchPatternPartEdgeBuilder()
                }
            }
        }

        public companion object {
            @JvmStatic
            public fun builder(): GraphMatchPatternBuilder = GraphMatchPatternBuilder()
        }
    }

    public data class Quantifier(
        public override val id: Int,
        public val lower: Long,
        public val upper: Long?
    ) : AstNode() {
        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitGraphMatchQuantifier(this, ctx)

        public companion object {
            @JvmStatic
            public fun builder(): GraphMatchQuantifierBuilder = GraphMatchQuantifierBuilder()
        }
    }

    public sealed class Selector : AstNode() {
        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = when (this) {
            is AnyShortest -> visitor.visitGraphMatchSelectorAnyShortest(this, ctx)
            is AllShortest -> visitor.visitGraphMatchSelectorAllShortest(this, ctx)
            is Any -> visitor.visitGraphMatchSelectorAny(this, ctx)
            is AnyK -> visitor.visitGraphMatchSelectorAnyK(this, ctx)
            is ShortestK -> visitor.visitGraphMatchSelectorShortestK(this, ctx)
            is ShortestKGroup -> visitor.visitGraphMatchSelectorShortestKGroup(this, ctx)
        }

        public data class AnyShortest(
            public override val id: Int
        ) : Selector() {
            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                visitor.visitGraphMatchSelectorAnyShortest(this, ctx)

            public companion object {
                @JvmStatic
                public fun builder(): GraphMatchSelectorAnyShortestBuilder =
                    GraphMatchSelectorAnyShortestBuilder()
            }
        }

        public data class AllShortest(
            public override val id: Int
        ) : Selector() {
            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                visitor.visitGraphMatchSelectorAllShortest(this, ctx)

            public companion object {
                @JvmStatic
                public fun builder(): GraphMatchSelectorAllShortestBuilder =
                    GraphMatchSelectorAllShortestBuilder()
            }
        }

        public data class Any(
            public override val id: Int
        ) : Selector() {
            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                visitor.visitGraphMatchSelectorAny(this, ctx)

            public companion object {
                @JvmStatic
                public fun builder(): GraphMatchSelectorAnyBuilder = GraphMatchSelectorAnyBuilder()
            }
        }

        public data class AnyK(
            public override val id: Int,
            public val k: Long
        ) : Selector() {
            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                visitor.visitGraphMatchSelectorAnyK(this, ctx)

            public companion object {
                @JvmStatic
                public fun builder(): GraphMatchSelectorAnyKBuilder = GraphMatchSelectorAnyKBuilder()
            }
        }

        public data class ShortestK(
            public override val id: Int,
            public val k: Long
        ) : Selector() {
            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                visitor.visitGraphMatchSelectorShortestK(this, ctx)

            public companion object {
                @JvmStatic
                public fun builder(): GraphMatchSelectorShortestKBuilder =
                    GraphMatchSelectorShortestKBuilder()
            }
        }

        public data class ShortestKGroup(
            public override val id: Int,
            public val k: Long
        ) : Selector() {
            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                visitor.visitGraphMatchSelectorShortestKGroup(this, ctx)

            public companion object {
                @JvmStatic
                public fun builder(): GraphMatchSelectorShortestKGroupBuilder =
                    GraphMatchSelectorShortestKGroupBuilder()
            }
        }
    }

    public companion object {
        @JvmStatic
        public fun builder(): GraphMatchBuilder = GraphMatchBuilder()
    }
}

public data class Over(
    public override val id: Int,
    public val partitions: List<Expr>,
    public val sorts: List<OrderBy.Sort>
) : AstNode() {
    public override val children: List<AstNode> by lazy {
        val kids = mutableListOf<AstNode?>()
        kids.addAll(partitions)
        kids.addAll(sorts)
        kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = visitor.visitOver(
        this,
        ctx
    )

    public companion object {
        @JvmStatic
        public fun builder(): OverBuilder = OverBuilder()
    }
}

public data class OnConflict(
    public override val id: Int,
    public val expr: Expr,
    public val action: Action
) : AstNode() {
    public override val children: List<AstNode> by lazy {
        val kids = mutableListOf<AstNode?>()
        kids.add(expr)
        kids.add(action)
        kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
        visitor.visitOnConflict(this, ctx)

    public enum class Value {
        EXCLUDED,
    }

    public sealed class Action : AstNode() {
        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = when (this) {
            is DoReplace -> visitor.visitOnConflictActionDoReplace(this, ctx)
            is DoUpdate -> visitor.visitOnConflictActionDoUpdate(this, ctx)
            is DoNothing -> visitor.visitOnConflictActionDoNothing(this, ctx)
        }

        public data class DoReplace(
            public override val id: Int,
            public val `value`: Value
        ) : Action() {
            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                visitor.visitOnConflictActionDoReplace(this, ctx)

            public companion object {
                @JvmStatic
                public fun builder(): OnConflictActionDoReplaceBuilder = OnConflictActionDoReplaceBuilder()
            }
        }

        public data class DoUpdate(
            public override val id: Int,
            public val `value`: Value
        ) : Action() {
            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                visitor.visitOnConflictActionDoUpdate(this, ctx)

            public companion object {
                @JvmStatic
                public fun builder(): OnConflictActionDoUpdateBuilder = OnConflictActionDoUpdateBuilder()
            }
        }

        public data class DoNothing(
            public override val id: Int
        ) : Action() {
            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                visitor.visitOnConflictActionDoNothing(this, ctx)

            public companion object {
                @JvmStatic
                public fun builder(): OnConflictActionDoNothingBuilder = OnConflictActionDoNothingBuilder()
            }
        }
    }

    public companion object {
        @JvmStatic
        public fun builder(): OnConflictBuilder = OnConflictBuilder()
    }
}

public data class Returning(
    public override val id: Int,
    public val columns: List<Column>
) : AstNode() {
    public override val children: List<AstNode> by lazy {
        val kids = mutableListOf<AstNode?>()
        kids.addAll(columns)
        kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
        visitor.visitReturning(this, ctx)

    public data class Column(
        public override val id: Int,
        public val status: Status,
        public val age: Age,
        public val `value`: Value
    ) : AstNode() {
        public override val children: List<AstNode> by lazy {
            val kids = mutableListOf<AstNode?>()
            kids.add(value)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitReturningColumn(this, ctx)

        public enum class Status {
            MODIFIED,
            ALL,
        }

        public enum class Age {
            OLD,
            NEW,
        }

        public sealed class Value : AstNode() {
            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = when (this) {
                is Wildcard -> visitor.visitReturningColumnValueWildcard(this, ctx)
                is Expression -> visitor.visitReturningColumnValueExpression(this, ctx)
            }

            public data class Wildcard(
                public override val id: Int
            ) : Value() {
                public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                    visitor.visitReturningColumnValueWildcard(this, ctx)

                public companion object {
                    @JvmStatic
                    public fun builder(): ReturningColumnValueWildcardBuilder =
                        ReturningColumnValueWildcardBuilder()
                }
            }

            public data class Expression(
                public override val id: Int,
                public val expr: Expr
            ) : Value() {
                public override val children: List<AstNode> by lazy {
                    val kids = mutableListOf<AstNode?>()
                    kids.add(expr)
                    kids.filterNotNull()
                }

                public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                    visitor.visitReturningColumnValueExpression(this, ctx)

                public companion object {
                    @JvmStatic
                    public fun builder(): ReturningColumnValueExpressionBuilder =
                        ReturningColumnValueExpressionBuilder()
                }
            }
        }

        public companion object {
            @JvmStatic
            public fun builder(): ReturningColumnBuilder = ReturningColumnBuilder()
        }
    }

    public companion object {
        @JvmStatic
        public fun builder(): ReturningBuilder = ReturningBuilder()
    }
}

public data class TableDefinition(
    public override val id: Int,
    public val columns: List<Column>
) : AstNode() {
    public override val children: List<AstNode> by lazy {
        val kids = mutableListOf<AstNode?>()
        kids.addAll(columns)
        kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
        visitor.visitTableDefinition(this, ctx)

    public data class Column(
        public override val id: Int,
        public val name: String,
        public val type: Type,
        public val constraints: List<Constraint>
    ) : AstNode() {
        public override val children: List<AstNode> by lazy {
            val kids = mutableListOf<AstNode?>()
            kids.add(type)
            kids.addAll(constraints)
            kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitTableDefinitionColumn(this, ctx)

        public data class Constraint(
            public override val id: Int,
            public val name: String?,
            public val body: Body
        ) : AstNode() {
            public override val children: List<AstNode> by lazy {
                val kids = mutableListOf<AstNode?>()
                kids.add(body)
                kids.filterNotNull()
            }

            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                visitor.visitTableDefinitionColumnConstraint(this, ctx)

            public sealed class Body : AstNode() {
                public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = when (this) {
                    is Nullable -> visitor.visitTableDefinitionColumnConstraintBodyNullable(this, ctx)
                    is NotNull -> visitor.visitTableDefinitionColumnConstraintBodyNotNull(this, ctx)
                    is Check -> visitor.visitTableDefinitionColumnConstraintBodyCheck(this, ctx)
                }

                public data class Nullable(
                    public override val id: Int
                ) : Body() {
                    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                        visitor.visitTableDefinitionColumnConstraintBodyNullable(this, ctx)

                    public companion object {
                        @JvmStatic
                        public fun builder(): TableDefinitionColumnConstraintBodyNullableBuilder =
                            TableDefinitionColumnConstraintBodyNullableBuilder()
                    }
                }

                public data class NotNull(
                    public override val id: Int
                ) : Body() {
                    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                        visitor.visitTableDefinitionColumnConstraintBodyNotNull(this, ctx)

                    public companion object {
                        @JvmStatic
                        public fun builder(): TableDefinitionColumnConstraintBodyNotNullBuilder =
                            TableDefinitionColumnConstraintBodyNotNullBuilder()
                    }
                }

                public data class Check(
                    public override val id: Int,
                    public val expr: Expr
                ) : Body() {
                    public override val children: List<AstNode> by lazy {
                        val kids = mutableListOf<AstNode?>()
                        kids.add(expr)
                        kids.filterNotNull()
                    }

                    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                        visitor.visitTableDefinitionColumnConstraintBodyCheck(this, ctx)

                    public companion object {
                        @JvmStatic
                        public fun builder(): TableDefinitionColumnConstraintBodyCheckBuilder =
                            TableDefinitionColumnConstraintBodyCheckBuilder()
                    }
                }
            }

            public companion object {
                @JvmStatic
                public fun builder(): TableDefinitionColumnConstraintBuilder =
                    TableDefinitionColumnConstraintBuilder()
            }
        }

        public companion object {
            @JvmStatic
            public fun builder(): TableDefinitionColumnBuilder = TableDefinitionColumnBuilder()
        }
    }

    public companion object {
        @JvmStatic
        public fun builder(): TableDefinitionBuilder = TableDefinitionBuilder()
    }
}

public enum class SetQuantifier {
    ALL,
    DISTINCT,
}

public enum class Case {
    SENSITIVE,
    INSENSITIVE,
}
