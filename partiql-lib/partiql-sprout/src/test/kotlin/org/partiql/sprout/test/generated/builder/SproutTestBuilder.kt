package org.partiql.sprout.test.generated.builder

import com.amazon.ionelement.api.TimestampElement
import org.partiql.sprout.test.generated.Collection
import org.partiql.sprout.test.generated.Inlines
import org.partiql.sprout.test.generated.Node
import org.partiql.sprout.test.generated.SproutTestNode

public fun <T : SproutTestNode> sproutTest(
    factory: SproutTestFactory = SproutTestFactory.DEFAULT,
    block: SproutTestBuilder.() -> T
) = SproutTestBuilder(factory).block()

public class SproutTestBuilder(
    private val factory: SproutTestFactory = SproutTestFactory.DEFAULT
) {
    public fun node(
        a: Boolean? = null,
        b: Int? = null,
        c: String? = null,
        d: Float? = null,
        e: TimestampElement? = null,
        block: NodeBuilder.() -> Unit = {}
    ): Node {
        val builder = NodeBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun collectionMySet(
        values: MutableSet<Int> = mutableSetOf(),
        block: CollectionMySetBuilder.() -> Unit = {}
    ): Collection.MySet {
        val builder = CollectionMySetBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun collectionMyList(
        values: MutableList<Int> = mutableListOf(),
        block: CollectionMyListBuilder.() -> Unit = {}
    ): Collection.MyList {
        val builder = CollectionMyListBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun collectionMyMap(
        values: MutableMap<String, Int> = mutableMapOf(),
        block: CollectionMyMapBuilder.() -> Unit = {}
    ): Collection.MyMap {
        val builder = CollectionMyMapBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun inlines(
        `enum`: Inlines.Bar? = null,
        product: Inlines.Foo? = null,
        sum: Inlines.Sum? = null,
        block: InlinesBuilder.() -> Unit = {}
    ): Inlines {
        val builder = InlinesBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun inlinesFoo(
        x: Int? = null,
        y: Inlines.Bar? = null,
        block: InlinesFooBuilder.() -> Unit = {}
    ): Inlines.Foo {
        val builder = InlinesFooBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun inlinesSumU(foo: String? = null, block: InlinesSumUBuilder.() -> Unit = {}):
        Inlines.Sum.U {
        val builder = InlinesSumUBuilder()
        builder.block()
        return builder.build(factory)
    }

    public fun inlinesSumV(bar: String? = null, block: InlinesSumVBuilder.() -> Unit = {}):
        Inlines.Sum.V {
        val builder = InlinesSumVBuilder()
        builder.block()
        return builder.build(factory)
    }
}
