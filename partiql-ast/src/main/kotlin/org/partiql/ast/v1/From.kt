package org.partiql.ast.v1

/**
 * TODO docs, equals, hashcode
 */
public abstract class From : AstNode() {
    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = when (this) {
        is Value -> visitor.visitFromValue(this, ctx)
        is Join -> visitor.visitFromJoin(this, ctx)
        else -> throw NotImplementedError()
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Value(
        @JvmField
        public var expr: Expr,
        @JvmField
        public var type: Type,
        @JvmField
        public var asAlias: Identifier.Symbol?,
        @JvmField
        public var atAlias: Identifier.Symbol?,
        @JvmField
        public var byAlias: Identifier.Symbol?,
    ) : From() {
        public override fun children(): Collection<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.add(expr)
            asAlias?.let { kids.add(it) }
            atAlias?.let { kids.add(it) }
            byAlias?.let { kids.add(it) }
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitFromValue(this, ctx)

        /**
         * TODO docs, equals, hashcode
         */
        public enum class Type {
            SCAN,
            UNPIVOT,
            OTHER,
        }
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Join(
        @JvmField
        public var lhs: From,
        @JvmField
        public var rhs: From,
        @JvmField
        public var type: Type?,
        @JvmField
        public var condition: Expr?,
    ) : From() {
        public override fun children(): Collection<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.add(lhs)
            kids.add(rhs)
            condition?.let { kids.add(it) }
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitFromJoin(this, ctx)

        /**
         * TODO docs, equals, hashcode
         */
        public enum class Type {
            INNER,
            LEFT,
            LEFT_OUTER,
            RIGHT,
            RIGHT_OUTER,
            FULL,
            FULL_OUTER,
            CROSS,
            COMMA,
            OTHER,
        }
    }
}
