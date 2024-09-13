package org.partiql.ast.v1

/**
 * TODO docs, equals, hashcode
 */
public abstract class Statement : AstNode() {
    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = when (this) {
        is Query -> visitor.visitStatementQuery(this, ctx)
        is DDL -> visitor.visitStatementDDL(this, ctx)
        is Explain -> visitor.visitStatementExplain(this, ctx)
        else -> throw NotImplementedError()
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Query(
        @JvmField
        public var expr: Expr,
    ) : Statement() {
        public override fun children(): Collection<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.add(expr)
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitStatementQuery(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public abstract class DDL : Statement() {
        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = when (this) {
            is CreateTable -> visitor.visitStatementDDLCreateTable(this, ctx)
            is CreateIndex -> visitor.visitStatementDDLCreateIndex(this, ctx)
            is DropTable -> visitor.visitStatementDDLDropTable(this, ctx)
            is DropIndex -> visitor.visitStatementDDLDropIndex(this, ctx)
            else -> throw NotImplementedError()
        }

        /**
         * TODO docs, equals, hashcode
         */
        public class CreateTable(
            @JvmField
            public var name: Identifier,
            @JvmField
            public var definition: TableDefinition?,
        ) : DDL() {
            public override fun children(): Collection<AstNode> {
                val kids = mutableListOf<AstNode?>()
                kids.add(name)
                definition?.let { kids.add(it) }
                return kids.filterNotNull()
            }

            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                visitor.visitStatementDDLCreateTable(this, ctx)
        }

        /**
         * TODO docs, equals, hashcode
         */
        public class CreateIndex(
            @JvmField
            public var index: Identifier?,
            @JvmField
            public var table: Identifier,
            @JvmField
            public var fields: List<PathLit>,
        ) : DDL() {
            public override fun children(): Collection<AstNode> {
                val kids = mutableListOf<AstNode?>()
                index?.let { kids.add(it) }
                kids.add(table)
                kids.addAll(fields)
                return kids.filterNotNull()
            }

            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                visitor.visitStatementDDLCreateIndex(this, ctx)
        }

        /**
         * TODO docs, equals, hashcode
         */
        public class DropTable(
            @JvmField
            public var table: Identifier,
        ) : DDL() {
            public override fun children(): Collection<AstNode> {
                val kids = mutableListOf<AstNode?>()
                kids.add(table)
                return kids.filterNotNull()
            }

            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                visitor.visitStatementDDLDropTable(this, ctx)
        }

        /**
         * TODO docs, equals, hashcode
         */
        public class DropIndex(
            @JvmField
            public var index: Identifier,
            @JvmField
            public var table: Identifier,
        ) : DDL() {
            public override fun children(): Collection<AstNode> {
                val kids = mutableListOf<AstNode?>()
                kids.add(index)
                kids.add(table)
                return kids.filterNotNull()
            }

            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                visitor.visitStatementDDLDropIndex(this, ctx)
        }
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Explain(
        @JvmField
        public var target: Target,
    ) : Statement() {
        public override fun children(): Collection<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.add(target)
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitStatementExplain(this, ctx)

        /**
         * TODO docs, equals, hashcode
         */
        public abstract class Target : AstNode() {
            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = when (this) {
                is Domain -> visitor.visitStatementExplainTargetDomain(this, ctx)
                else -> throw NotImplementedError()
            }

            /**
             * TODO docs, equals, hashcode
             */
            public class Domain(
                @JvmField
                public var statement: Statement,
                @JvmField
                public var type: String?,
                @JvmField
                public var format: String?,
            ) : Target() {
                public override fun children(): Collection<AstNode> {
                    val kids = mutableListOf<AstNode?>()
                    kids.add(statement)
                    return kids.filterNotNull()
                }

                public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                    visitor.visitStatementExplainTargetDomain(this, ctx)
            }
        }
    }
}
