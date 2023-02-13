package org.partiql.sprout.test.generated.visitor

import org.partiql.sprout.test.generated.Collection
import org.partiql.sprout.test.generated.Inlines
import org.partiql.sprout.test.generated.Node
import org.partiql.sprout.test.generated.SproutTestNode

public interface SproutTestVisitor<R, C> {
    public fun visit(node: SproutTestNode, ctx: C): R

    public fun visitNode(node: Node, ctx: C): R

    public fun visitCollection(node: Collection, ctx: C): R

    public fun visitCollectionMySet(node: Collection.MySet, ctx: C): R

    public fun visitCollectionMyList(node: Collection.MyList, ctx: C): R

    public fun visitCollectionMyMap(node: Collection.MyMap, ctx: C): R

    public fun visitInlines(node: Inlines, ctx: C): R

    public fun visitInlinesFoo(node: Inlines.Foo, ctx: C): R

    public fun visitInlinesSum(node: Inlines.Sum, ctx: C): R

    public fun visitInlinesSumU(node: Inlines.Sum.U, ctx: C): R

    public fun visitInlinesSumV(node: Inlines.Sum.V, ctx: C): R
}
