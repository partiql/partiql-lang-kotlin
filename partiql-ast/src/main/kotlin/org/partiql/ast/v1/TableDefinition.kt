package org.partiql.ast.v1

/**
 * TODO docs, equals, hashcode
 */
public class TableDefinition(
    @JvmField
    public var columns: List<Column>,
) : AstNode() {
    public override fun children(): Collection<AstNode> {
        val kids = mutableListOf<AstNode?>()
        kids.addAll(columns)
        return kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
        visitor.visitTableDefinition(this, ctx)

    /**
     * TODO docs, equals, hashcode
     */
    public class Column(
        @JvmField
        public var name: String,
        @JvmField
        public var type: Type,
        @JvmField
        public var constraints: List<Constraint>,
    ) : AstNode() {
        public override fun children(): Collection<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.add(type)
            kids.addAll(constraints)
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitTableDefinitionColumn(this, ctx)

        /**
         * TODO docs, equals, hashcode
         */
        public class Constraint(
            @JvmField
            public var name: String?,
            @JvmField
            public var body: Body,
        ) : AstNode() {
            public override fun children(): Collection<AstNode> {
                val kids = mutableListOf<AstNode?>()
                kids.add(body)
                return kids.filterNotNull()
            }

            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                visitor.visitTableDefinitionColumnConstraint(this, ctx)

            /**
             * TODO docs, equals, hashcode
             */
            public abstract class Body : AstNode() {
                public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = when (this) {
                    is Nullable -> visitor.visitTableDefinitionColumnConstraintBodyNullable(this, ctx)
                    is NotNull -> visitor.visitTableDefinitionColumnConstraintBodyNotNull(this, ctx)
                    is Check -> visitor.visitTableDefinitionColumnConstraintBodyCheck(this, ctx)
                    else -> throw NotImplementedError()
                }

                /**
                 * TODO docs, equals, hashcode
                 */
                public object Nullable : Body() {
                    public override fun children(): Collection<AstNode> = emptyList()

                    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                        visitor.visitTableDefinitionColumnConstraintBodyNullable(this, ctx)
                }

                /**
                 * TODO docs, equals, hashcode
                 */
                public object NotNull : Body() {
                    public override fun children(): Collection<AstNode> = emptyList()

                    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                        visitor.visitTableDefinitionColumnConstraintBodyNotNull(this, ctx)
                }

                /**
                 * TODO docs, equals, hashcode
                 */
                public class Check(
                    @JvmField
                    public var expr: Expr,
                ) : Body() {
                    public override fun children(): Collection<AstNode> {
                        val kids = mutableListOf<AstNode?>()
                        kids.add(expr)
                        return kids.filterNotNull()
                    }

                    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                        visitor.visitTableDefinitionColumnConstraintBodyCheck(this, ctx)
                }
            }
        }
    }
}
