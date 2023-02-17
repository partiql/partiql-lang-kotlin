package org.partiql.sprout.test.generated.builder

import com.amazon.ionelement.api.TimestampElement
import org.partiql.sprout.test.generated.Collection
import org.partiql.sprout.test.generated.Inlines
import org.partiql.sprout.test.generated.Node
import org.partiql.sprout.test.generated.SproutTestNode

public abstract class SproutTestFactory {
    public open fun node(
        a: Boolean,
        b: Int,
        c: String,
        d: Float?,
        e: TimestampElement
    ) = Node(a, b, c, d, e)

    public open fun collectionMySet(values: Set<Int>) = Collection.MySet(values)

    public open fun collectionMyList(values: List<Int>) = Collection.MyList(values)

    public open fun collectionMyMap(values: Map<String, Int>) = Collection.MyMap(values)

    public open fun inlines(
        `enum`: Inlines.Bar?,
        product: Inlines.Foo,
        sum: Inlines.Sum
    ) = Inlines(enum, product, sum)

    public open fun inlinesFoo(x: Int, y: Inlines.Bar) = Inlines.Foo(x, y)

    public open fun inlinesSumU(foo: String) = Inlines.Sum.U(foo)

    public open fun inlinesSumV(bar: String) = Inlines.Sum.V(bar)

    public companion object {
        public val DEFAULT: SproutTestFactory = object : SproutTestFactory() {}

        @JvmStatic
        public fun <T : SproutTestNode> create(block: SproutTestFactory.() -> T) =
            SproutTestFactory.DEFAULT.block()
    }
}
