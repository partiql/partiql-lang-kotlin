package org.partiql.sprout.test.generated.visitor

import org.partiql.sprout.test.generated.Collection
import org.partiql.sprout.test.generated.Node
import org.partiql.sprout.test.generated.SproutTestNode

public interface SproutTestVisitor<R, C> {
    public fun visit(node: SproutTestNode, ctx: C): R

    public fun visitNode(node: Node, ctx: C): R

    public fun visitCollection(node: Collection, ctx: C): R

    public fun visitCollectionSet(node: Collection.Set, ctx: C): R

    public fun visitCollectionList(node: Collection.List, ctx: C): R

    public fun visitCollectionMap(node: Collection.Map, ctx: C): R
}

public abstract class SproutTestBaseVisitor<R, C> : SproutTestVisitor<R, C> {
    public override fun visit(node: SproutTestNode, ctx: C): R = node.accept(this, ctx)

    public override fun visitNode(node: Node, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitCollection(node: Collection, ctx: C): R = when (node) {
        is Collection.Set -> visitCollectionSet(node, ctx)
        is Collection.List -> visitCollectionList(node, ctx)
        is Collection.Map -> visitCollectionMap(node, ctx)
    }

    public override fun visitCollectionSet(node: Collection.Set, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitCollectionList(node: Collection.List, ctx: C): R = defaultVisit(
        node, ctx
    )

    public override fun visitCollectionMap(node: Collection.Map, ctx: C): R = defaultVisit(node, ctx)

    public open fun defaultVisit(node: SproutTestNode, ctx: C): R {
        for (child in node.children) {
            child.accept(this, ctx)
        }
        return defaultReturn(node, ctx)
    }

    public abstract fun defaultReturn(node: SproutTestNode, ctx: C): R
}
