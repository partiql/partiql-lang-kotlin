package org.partiql.ast.v1

/**
 * TODO docs, equals, hashcode
 */
public class GraphMatch(
    @JvmField
    public var patterns: List<Pattern>,
    @JvmField
    public var selector: Selector?,
) : AstNode() {
    public override fun children(): Collection<AstNode> {
        val kids = mutableListOf<AstNode?>()
        kids.addAll(patterns)
        selector?.let { kids.add(it) }
        return kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
        visitor.visitGraphMatch(this, ctx)

    /**
     * TODO docs, equals, hashcode
     */
    public enum class Direction {
        LEFT,
        UNDIRECTED,
        RIGHT,
        LEFT_OR_UNDIRECTED,
        UNDIRECTED_OR_RIGHT,
        LEFT_OR_RIGHT,
        LEFT_UNDIRECTED_OR_RIGHT,
        OTHER,
    }

    /**
     * TODO docs, equals, hashcode
     */
    public enum class Restrictor {
        TRAIL,
        ACYCLIC,
        SIMPLE,
        OTHER,
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Pattern(
        @JvmField
        public var restrictor: Restrictor?,
        @JvmField
        public var prefilter: Expr?,
        @JvmField
        public var variable: String?,
        @JvmField
        public var quantifier: Quantifier?,
        @JvmField
        public var parts: List<Part>,
    ) : AstNode() {
        public override fun children(): Collection<AstNode> {
            val kids = mutableListOf<AstNode?>()
            prefilter?.let { kids.add(it) }
            quantifier?.let { kids.add(it) }
            kids.addAll(parts)
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitGraphMatchPattern(this, ctx)

        /**
         * TODO docs, equals, hashcode
         */
        public abstract class Part : AstNode() {
            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = when (this) {
                is Node -> visitor.visitGraphMatchPatternPartNode(this, ctx)
                is Edge -> visitor.visitGraphMatchPatternPartEdge(this, ctx)
                is Pattern -> visitor.visitGraphMatchPatternPartPattern(this, ctx)
                else -> throw NotImplementedError()
            }

            /**
             * TODO docs, equals, hashcode
             */
            public class Node(
                @JvmField
                public var prefilter: Expr?,
                @JvmField
                public var variable: String?,
                @JvmField
                public var label: Label?,
            ) : Part() {
                public override fun children(): Collection<AstNode> {
                    val kids = mutableListOf<AstNode?>()
                    prefilter?.let { kids.add(it) }
                    label?.let { kids.add(it) }
                    return kids.filterNotNull()
                }

                public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                    visitor.visitGraphMatchPatternPartNode(this, ctx)
            }

            /**
             * TODO docs, equals, hashcode
             */
            public class Edge(
                @JvmField
                public var direction: Direction,
                @JvmField
                public var quantifier: Quantifier?,
                @JvmField
                public var prefilter: Expr?,
                @JvmField
                public var variable: String?,
                @JvmField
                public var label: Label?,
            ) : Part() {
                public override fun children(): Collection<AstNode> {
                    val kids = mutableListOf<AstNode?>()
                    quantifier?.let { kids.add(it) }
                    prefilter?.let { kids.add(it) }
                    label?.let { kids.add(it) }
                    return kids.filterNotNull()
                }

                public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                    visitor.visitGraphMatchPatternPartEdge(this, ctx)
            }

            /**
             * TODO docs, equals, hashcode
             */
            public class Pattern(
                @JvmField
                public var pattern: GraphMatch.Pattern,
            ) : Part() {
                public override fun children(): Collection<AstNode> {
                    val kids = mutableListOf<AstNode?>()
                    kids.add(pattern)
                    return kids.filterNotNull()
                }

                public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                    visitor.visitGraphMatchPatternPartPattern(this, ctx)
            }
        }
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Quantifier(
        @JvmField
        public var lower: Long,
        @JvmField
        public var upper: Long?,
    ) : AstNode() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitGraphMatchQuantifier(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public abstract class Selector : AstNode() {
        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = when (this) {
            is AnyShortest -> visitor.visitGraphMatchSelectorAnyShortest(this, ctx)
            is AllShortest -> visitor.visitGraphMatchSelectorAllShortest(this, ctx)
            is Any -> visitor.visitGraphMatchSelectorAny(this, ctx)
            is AnyK -> visitor.visitGraphMatchSelectorAnyK(this, ctx)
            is ShortestK -> visitor.visitGraphMatchSelectorShortestK(this, ctx)
            is ShortestKGroup -> visitor.visitGraphMatchSelectorShortestKGroup(this, ctx)
            else -> throw NotImplementedError()
        }

        /**
         * TODO docs, equals, hashcode
         */
        public object AnyShortest : Selector() {
            public override fun children(): Collection<AstNode> = emptyList()

            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                visitor.visitGraphMatchSelectorAnyShortest(this, ctx)
        }

        /**
         * TODO docs, equals, hashcode
         */
        public object AllShortest : Selector() {
            public override fun children(): Collection<AstNode> = emptyList()

            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                visitor.visitGraphMatchSelectorAllShortest(this, ctx)
        }

        /**
         * TODO docs, equals, hashcode
         */
        public object Any : Selector() {
            public override fun children(): Collection<AstNode> = emptyList()

            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                visitor.visitGraphMatchSelectorAny(this, ctx)
        }

        /**
         * TODO docs, equals, hashcode
         */
        public class AnyK(
            @JvmField
            public var k: Long,
        ) : Selector() {
            public override fun children(): Collection<AstNode> = emptyList()

            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                visitor.visitGraphMatchSelectorAnyK(this, ctx)
        }

        /**
         * TODO docs, equals, hashcode
         */
        public class ShortestK(
            @JvmField
            public var k: Long,
        ) : Selector() {
            public override fun children(): Collection<AstNode> = emptyList()

            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                visitor.visitGraphMatchSelectorShortestK(this, ctx)
        }

        /**
         * TODO docs, equals, hashcode
         */
        public class ShortestKGroup(
            @JvmField
            public var k: Long,
        ) : Selector() {
            public override fun children(): Collection<AstNode> = emptyList()

            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                visitor.visitGraphMatchSelectorShortestKGroup(this, ctx)
        }
    }

    /**
     * TODO docs, equals, hashcode
     */
    public abstract class Label : AstNode() {
        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = when (this) {
            is Name -> visitor.visitGraphMatchLabelName(this, ctx)
            is Wildcard -> visitor.visitGraphMatchLabelWildcard(this, ctx)
            is Negation -> visitor.visitGraphMatchLabelNegation(this, ctx)
            is Conj -> visitor.visitGraphMatchLabelConj(this, ctx)
            is Disj -> visitor.visitGraphMatchLabelDisj(this, ctx)
            else -> throw NotImplementedError()
        }

        /**
         * TODO docs, equals, hashcode
         */
        public class Name(
            @JvmField
            public var name: String,
        ) : Label() {
            public override fun children(): Collection<AstNode> = emptyList()

            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                visitor.visitGraphMatchLabelName(this, ctx)
        }

        /**
         * TODO docs, equals, hashcode
         */
        public object Wildcard : Label() {
            public override fun children(): Collection<AstNode> = emptyList()

            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                visitor.visitGraphMatchLabelWildcard(this, ctx)
        }

        /**
         * TODO docs, equals, hashcode
         */
        public class Negation(
            @JvmField
            public var arg: Label,
        ) : Label() {
            public override fun children(): Collection<AstNode> {
                val kids = mutableListOf<AstNode?>()
                kids.add(arg)
                return kids.filterNotNull()
            }

            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                visitor.visitGraphMatchLabelNegation(this, ctx)
        }

        /**
         * TODO docs, equals, hashcode
         */
        public class Conj(
            @JvmField
            public var lhs: Label,
            @JvmField
            public var rhs: Label,
        ) : Label() {
            public override fun children(): Collection<AstNode> {
                val kids = mutableListOf<AstNode?>()
                kids.add(lhs)
                kids.add(rhs)
                return kids.filterNotNull()
            }

            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                visitor.visitGraphMatchLabelConj(this, ctx)
        }

        /**
         * TODO docs, equals, hashcode
         */
        public class Disj(
            @JvmField
            public var lhs: Label,
            @JvmField
            public var rhs: Label,
        ) : Label() {
            public override fun children(): Collection<AstNode> {
                val kids = mutableListOf<AstNode?>()
                kids.add(lhs)
                kids.add(rhs)
                return kids.filterNotNull()
            }

            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                visitor.visitGraphMatchLabelDisj(this, ctx)
        }
    }
}
