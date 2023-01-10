package org.partiql.sprout.test.generated

import com.amazon.ionelement.api.TimestampElement
import kotlin.Boolean
import kotlin.Float
import kotlin.Int
import kotlin.String
import kotlin.collections.List
import org.partiql.sprout.test.generated.visitor.SproutTestVisitor

public abstract class SproutTestNode {
    public open val children: List<SproutTestNode> = emptyList()

    public abstract fun <R, C> accept(visitor: SproutTestVisitor<R, C>, ctx: C): R
}

public data class Node(
    public val a: Boolean,
    public val b: Int,
    public val c: String,
    public val d: Float?,
    public val e: TimestampElement
) : SproutTestNode() {
    public override fun <R, C> accept(visitor: SproutTestVisitor<R, C>, ctx: C): R =
        visitor.visitNode(this, ctx)
}

public sealed class Collection : SproutTestNode() {
    public override fun <R, C> accept(visitor: SproutTestVisitor<R, C>, ctx: C): R = when (this) {
        is Set -> visitor.visitCollectionSet(this, ctx)
        is List -> visitor.visitCollectionList(this, ctx)
        is Map -> visitor.visitCollectionMap(this, ctx)
    }

    public data class Set(
        public val values: kotlin.collections.Set<Int>
    ) : Collection() {
        public override fun <R, C> accept(visitor: SproutTestVisitor<R, C>, ctx: C): R =
            visitor.visitCollectionSet(this, ctx)
    }

    public data class List(
        public val values: kotlin.collections.List<Int>
    ) : Collection() {
        public override fun <R, C> accept(visitor: SproutTestVisitor<R, C>, ctx: C): R =
            visitor.visitCollectionList(this, ctx)
    }

    public data class Map(
        public val values: kotlin.collections.List<Int>
    ) : Collection() {
        public override fun <R, C> accept(visitor: SproutTestVisitor<R, C>, ctx: C): R =
            visitor.visitCollectionMap(this, ctx)
    }
}
