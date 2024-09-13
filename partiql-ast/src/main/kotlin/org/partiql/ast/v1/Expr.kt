package org.partiql.ast.v1

import com.amazon.ionelement.api.IonElement
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

/**
 * TODO docs, equals, hashcode
 */
public abstract class Expr : AstNode() {
    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = when (this) {
        is Lit -> visitor.visitExprLit(this, ctx)
        is Ion -> visitor.visitExprIon(this, ctx)
        is Var -> visitor.visitExprVar(this, ctx)
        is SessionAttribute -> visitor.visitExprSessionAttribute(this, ctx)
        is Path -> visitor.visitExprPath(this, ctx)
        is Call -> visitor.visitExprCall(this, ctx)
        is Parameter -> visitor.visitExprParameter(this, ctx)
        is Operator -> visitor.visitExprOperator(this, ctx)
        is Not -> visitor.visitExprNot(this, ctx)
        is And -> visitor.visitExprAnd(this, ctx)
        is Or -> visitor.visitExprOr(this, ctx)
        is Values -> visitor.visitExprValues(this, ctx)
        is Collection -> visitor.visitExprCollection(this, ctx)
        is Struct -> visitor.visitExprStruct(this, ctx)
        is Like -> visitor.visitExprLike(this, ctx)
        is Between -> visitor.visitExprBetween(this, ctx)
        is InCollection -> visitor.visitExprInCollection(this, ctx)
        is IsType -> visitor.visitExprIsType(this, ctx)
        is Case -> visitor.visitExprCase(this, ctx)
        is Coalesce -> visitor.visitExprCoalesce(this, ctx)
        is NullIf -> visitor.visitExprNullIf(this, ctx)
        is Substring -> visitor.visitExprSubstring(this, ctx)
        is Position -> visitor.visitExprPosition(this, ctx)
        is Trim -> visitor.visitExprTrim(this, ctx)
        is Overlay -> visitor.visitExprOverlay(this, ctx)
        is Extract -> visitor.visitExprExtract(this, ctx)
        is Cast -> visitor.visitExprCast(this, ctx)
        is DateAdd -> visitor.visitExprDateAdd(this, ctx)
        is DateDiff -> visitor.visitExprDateDiff(this, ctx)
        is QuerySet -> visitor.visitExprQuerySet(this, ctx)
        is Match -> visitor.visitExprMatch(this, ctx)
        is Window -> visitor.visitExprWindow(this, ctx)
        else -> throw NotImplementedError()
    }

    @OptIn(PartiQLValueExperimental::class)
    /**
     * TODO docs, equals, hashcode
     */
    public class Lit(
        @JvmField
        public var `value`: PartiQLValue,
    ) : Expr() {
        public override fun children(): kotlin.collections.Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprLit(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Ion(
        @JvmField
        public var `value`: IonElement,
    ) : Expr() {
        public override fun children(): kotlin.collections.Collection<Expr> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprIon(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Var(
        @JvmField
        public var identifier: Identifier,
        @JvmField
        public var scope: Scope,
    ) : Expr() {
        public override fun children(): List<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.add(identifier)
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprVar(this, ctx)

        /**
         * TODO docs, equals, hashcode
         */
        public enum class Scope {
            DEFAULT,
            LOCAL,
            OTHER,
        }
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class SessionAttribute(
        @JvmField
        public var attribute: Attribute,
    ) : Expr() {
        public override fun children(): List<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprSessionAttribute(this, ctx)

        /**
         * TODO docs, equals, hashcode
         */
        public enum class Attribute {
            CURRENT_USER,
            CURRENT_DATE,
            OTHER,
        }
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Path(
        @JvmField
        public var root: Expr,
        @JvmField
        public var steps: List<Step>,
    ) : Expr() {
        public override fun children(): List<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.add(root)
            kids.addAll(steps)
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprPath(this, ctx)

        /**
         * TODO docs, equals, hashcode
         */
        public abstract class Step : AstNode() {
            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = when (this) {
                is Symbol -> visitor.visitExprPathStepSymbol(this, ctx)
                is Index -> visitor.visitExprPathStepIndex(this, ctx)
                is Wildcard -> visitor.visitExprPathStepWildcard(this, ctx)
                is Unpivot -> visitor.visitExprPathStepUnpivot(this, ctx)
                else -> throw NotImplementedError()
            }

            /**
             * TODO docs, equals, hashcode
             */
            public class Symbol(
                @JvmField
                public var symbol: Identifier.Symbol,
            ) : Step() {
                public override fun children(): List<AstNode> {
                    val kids = mutableListOf<AstNode?>()
                    kids.add(symbol)
                    return kids.filterNotNull()
                }

                public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                    visitor.visitExprPathStepSymbol(this, ctx)
            }

            /**
             * TODO docs, equals, hashcode
             */
            public class Index(
                @JvmField
                public var key: Expr,
            ) : Step() {
                public override fun children(): List<AstNode> {
                    val kids = mutableListOf<AstNode?>()
                    kids.add(key)
                    return kids.filterNotNull()
                }

                public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                    visitor.visitExprPathStepIndex(this, ctx)
            }

            /**
             * TODO docs, equals, hashcode
             */
            public object Wildcard : Step() {
                public override fun children(): List<AstNode> = emptyList()

                public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                    visitor.visitExprPathStepWildcard(this, ctx)
            }

            /**
             * TODO docs, equals, hashcode
             */
            public object Unpivot : Step() {
                public override fun children(): List<AstNode> = emptyList()

                public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                    visitor.visitExprPathStepUnpivot(this, ctx)
            }
        }
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Call(
        @JvmField
        public var function: Identifier,
        @JvmField
        public var args: List<Expr>,
        @JvmField
        public var setq: SetQuantifier?,
    ) : Expr() {
        public override fun children(): List<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.add(function)
            kids.addAll(args)
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprCall(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Parameter(
        @JvmField
        public var index: Int,
    ) : Expr() {
        public override fun children(): List<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprParameter(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Operator(
        @JvmField
        public var symbol: String,
        @JvmField
        public var lhs: Expr?,
        @JvmField
        public var rhs: Expr,
    ) : Expr() {
        public override fun children(): List<AstNode> {
            val kids = mutableListOf<AstNode?>()
            lhs?.let { kids.add(it) }
            kids.add(rhs)
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprOperator(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Not(
        @JvmField
        public var `value`: Expr,
    ) : Expr() {
        public override fun children(): List<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.add(value)
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprNot(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class And(
        @JvmField
        public var lhs: Expr,
        @JvmField
        public var rhs: Expr,
    ) : Expr() {
        public override fun children(): List<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.add(lhs)
            kids.add(rhs)
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprAnd(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Or(
        @JvmField
        public var lhs: Expr,
        @JvmField
        public var rhs: Expr,
    ) : Expr() {
        public override fun children(): List<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.add(lhs)
            kids.add(rhs)
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprOr(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Values(
        @JvmField
        public var rows: List<Row>,
    ) : Expr() {
        public override fun children(): List<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.addAll(rows)
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprValues(this, ctx)

        /**
         * TODO docs, equals, hashcode
         */
        public class Row(
            @JvmField
            public var items: List<Expr>,
        ) : AstNode() {
            public override fun children(): List<AstNode> {
                val kids = mutableListOf<AstNode?>()
                kids.addAll(items)
                return kids.filterNotNull()
            }

            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                visitor.visitExprValuesRow(this, ctx)
        }
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Collection(
        @JvmField
        public var type: Type,
        @JvmField
        public var values: List<Expr>,
    ) : Expr() {
        public override fun children(): List<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.addAll(values)
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprCollection(this, ctx)

        /**
         * TODO docs, equals, hashcode
         */
        public enum class Type {
            BAG,
            ARRAY,
            VALUES,
            LIST,
            SEXP,
            OTHER,
        }
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Struct(
        @JvmField
        public var fields: List<Field>,
    ) : Expr() {
        public override fun children(): List<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.addAll(fields)
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprStruct(this, ctx)

        /**
         * TODO docs, equals, hashcode
         */
        public class Field(
            @JvmField
            public var name: Expr,
            @JvmField
            public var `value`: Expr,
        ) : AstNode() {
            public override fun children(): List<AstNode> {
                val kids = mutableListOf<AstNode?>()
                kids.add(name)
                kids.add(value)
                return kids.filterNotNull()
            }

            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                visitor.visitExprStructField(this, ctx)
        }
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Like(
        @JvmField
        public var `value`: Expr,
        @JvmField
        public var pattern: Expr,
        @JvmField
        public var escape: Expr?,
        @JvmField
        public var not: Boolean?,
    ) : Expr() {
        public override fun children(): List<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.add(value)
            kids.add(pattern)
            escape?.let { kids.add(it) }
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprLike(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Between(
        @JvmField
        public var `value`: Expr,
        @JvmField
        public var from: Expr,
        @JvmField
        public var to: Expr,
        @JvmField
        public var not: Boolean?,
    ) : Expr() {
        public override fun children(): List<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.add(value)
            kids.add(from)
            kids.add(to)
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprBetween(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class InCollection(
        @JvmField
        public var lhs: Expr,
        @JvmField
        public var rhs: Expr,
        @JvmField
        public var not: Boolean?,
    ) : Expr() {
        public override fun children(): List<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.add(lhs)
            kids.add(rhs)
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprInCollection(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class IsType(
        @JvmField
        public var `value`: Expr,
        @JvmField
        public var type: Type,
        @JvmField
        public var not: Boolean?,
    ) : Expr() {
        public override fun children(): List<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.add(value)
            kids.add(type)
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprIsType(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Case(
        @JvmField
        public var expr: Expr?,
        @JvmField
        public var branches: List<Branch>,
        @JvmField
        public var default: Expr?,
    ) : Expr() {
        public override fun children(): List<AstNode> {
            val kids = mutableListOf<AstNode?>()
            expr?.let { kids.add(it) }
            kids.addAll(branches)
            default?.let { kids.add(it) }
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprCase(this, ctx)

        /**
         * TODO docs, equals, hashcode
         */
        public class Branch(
            @JvmField
            public var condition: Expr,
            @JvmField
            public var expr: Expr,
        ) : AstNode() {
            public override fun children(): List<AstNode> {
                val kids = mutableListOf<AstNode?>()
                kids.add(condition)
                kids.add(expr)
                return kids.filterNotNull()
            }

            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                visitor.visitExprCaseBranch(this, ctx)
        }
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Coalesce(
        @JvmField
        public var args: List<Expr>,
    ) : Expr() {
        public override fun children(): List<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.addAll(args)
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprCoalesce(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class NullIf(
        @JvmField
        public var `value`: Expr,
        @JvmField
        public var nullifier: Expr,
    ) : Expr() {
        public override fun children(): List<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.add(value)
            kids.add(nullifier)
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprNullIf(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Substring(
        @JvmField
        public var `value`: Expr,
        @JvmField
        public var start: Expr?,
        @JvmField
        public var length: Expr?,
    ) : Expr() {
        public override fun children(): List<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.add(value)
            start?.let { kids.add(it) }
            length?.let { kids.add(it) }
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprSubstring(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Position(
        @JvmField
        public var lhs: Expr,
        @JvmField
        public var rhs: Expr,
    ) : Expr() {
        public override fun children(): List<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.add(lhs)
            kids.add(rhs)
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprPosition(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Trim(
        @JvmField
        public var `value`: Expr,
        @JvmField
        public var chars: Expr?,
        @JvmField
        public var spec: Spec?,
    ) : Expr() {
        public override fun children(): List<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.add(value)
            chars?.let { kids.add(it) }
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprTrim(this, ctx)

        public enum class Spec {
            LEADING,
            TRAILING,
            BOTH,
            OTHER,
        }
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Overlay(
        @JvmField
        public var `value`: Expr,
        @JvmField
        public var overlay: Expr,
        @JvmField
        public var start: Expr,
        @JvmField
        public var length: Expr?,
    ) : Expr() {
        public override fun children(): List<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.add(value)
            kids.add(overlay)
            kids.add(start)
            length?.let { kids.add(it) }
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprOverlay(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Extract(
        @JvmField
        public var `field`: DatetimeField,
        @JvmField
        public var source: Expr,
    ) : Expr() {
        public override fun children(): List<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.add(source)
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprExtract(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Cast(
        @JvmField
        public var `value`: Expr,
        @JvmField
        public var asType: Type,
    ) : Expr() {
        public override fun children(): List<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.add(value)
            kids.add(asType)
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprCast(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class DateAdd(
        @JvmField
        public var `field`: DatetimeField,
        @JvmField
        public var lhs: Expr,
        @JvmField
        public var rhs: Expr,
    ) : Expr() {
        public override fun children(): List<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.add(lhs)
            kids.add(rhs)
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprDateAdd(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class DateDiff(
        @JvmField
        public var `field`: DatetimeField,
        @JvmField
        public var lhs: Expr,
        @JvmField
        public var rhs: Expr,
    ) : Expr() {
        public override fun children(): List<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.add(lhs)
            kids.add(rhs)
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprDateDiff(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class QuerySet(
        @JvmField
        public var body: QueryBody,
        @JvmField
        public var orderBy: OrderBy?,
        @JvmField
        public var limit: Expr?,
        @JvmField
        public var offset: Expr?,
    ) : Expr() {
        public override fun children(): List<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.add(body)
            orderBy?.let { kids.add(it) }
            limit?.let { kids.add(it) }
            offset?.let { kids.add(it) }
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprQuerySet(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Match(
        @JvmField
        public var expr: Expr,
        @JvmField
        public var pattern: GraphMatch,
    ) : Expr() {
        public override fun children(): List<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.add(expr)
            kids.add(pattern)
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprMatch(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Window(
        @JvmField
        public var function: Function,
        @JvmField
        public var expression: Expr,
        @JvmField
        public var offset: Expr?,
        @JvmField
        public var default: Expr?,
        @JvmField
        public var over: Over,
    ) : Expr() {
        public override fun children(): List<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.add(expression)
            offset?.let { kids.add(it) }
            default?.let { kids.add(it) }
            kids.add(over)
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprWindow(this, ctx)

        /**
         * TODO docs, equals, hashcode
         */
        public enum class Function {
            LAG,
            LEAD,
            OTHER,
        }

        /**
         * TODO docs, equals, hashcode
         */
        public class Over(
            @JvmField
            public var partitions: List<Expr>?,
            @JvmField
            public var sorts: List<Sort>?,
        ) : AstNode() {
            public override fun children(): List<AstNode> {
                val kids = mutableListOf<AstNode?>()
                partitions?.let { kids.addAll(it) }
                sorts?.let { kids.addAll(it) }
                return kids.filterNotNull()
            }

            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                visitor.visitExprWindowOver(this, ctx)
        }
    }
}
