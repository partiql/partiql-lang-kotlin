package org.partiql.sprout.test.generated.builder

import com.amazon.ionelement.api.TimestampElement
import kotlin.Boolean
import kotlin.Float
import kotlin.Int
import kotlin.String
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import org.partiql.sprout.test.generated.Collection
import org.partiql.sprout.test.generated.Inlines
import org.partiql.sprout.test.generated.Node

public class NodeBuilder {
    public var a: Boolean? = null

    public var b: Int? = null

    public var c: String? = null

    public var d: Float? = null

    public var e: TimestampElement? = null

    public fun a(a: Boolean?): NodeBuilder = this.apply {
        this.a = a
    }

    public fun b(b: Int?): NodeBuilder = this.apply {
        this.b = b
    }

    public fun c(c: String?): NodeBuilder = this.apply {
        this.c = c
    }

    public fun d(d: Float?): NodeBuilder = this.apply {
        this.d = d
    }

    public fun e(e: TimestampElement?): NodeBuilder = this.apply {
        this.e = e
    }

    public fun build(): Node = build(SproutTestFactory.DEFAULT)

    public fun build(factory: SproutTestFactory = SproutTestFactory.DEFAULT): Node = factory.node(a =
    a!!, b = b!!, c = c!!, d = d, e = e!!)
}

public class CollectionMySetBuilder {
    public var values: MutableSet<Int> = mutableSetOf()

    public fun values(values: MutableSet<Int>): CollectionMySetBuilder = this.apply {
        this.values = values
    }

    public fun build(): Collection.MySet = build(SproutTestFactory.DEFAULT)

    public fun build(factory: SproutTestFactory = SproutTestFactory.DEFAULT): Collection.MySet =
        factory.collectionMySet(values = values)
}

public class CollectionMyListBuilder {
    public var values: MutableList<Int> = mutableListOf()

    public fun values(values: MutableList<Int>): CollectionMyListBuilder = this.apply {
        this.values = values
    }

    public fun build(): Collection.MyList = build(SproutTestFactory.DEFAULT)

    public fun build(factory: SproutTestFactory = SproutTestFactory.DEFAULT): Collection.MyList =
        factory.collectionMyList(values = values)
}

public class CollectionMyMapBuilder {
    public var values: MutableMap<String, Int> = mutableMapOf()

    public fun values(values: MutableMap<String, Int>): CollectionMyMapBuilder = this.apply {
        this.values = values
    }

    public fun build(): Collection.MyMap = build(SproutTestFactory.DEFAULT)

    public fun build(factory: SproutTestFactory = SproutTestFactory.DEFAULT): Collection.MyMap =
        factory.collectionMyMap(values = values)
}

public class InlinesBuilder {
    public var `enum`: Inlines.Bar? = null

    public var product: Inlines.Foo? = null

    public var sum: Inlines.Sum? = null

    public fun `enum`(`enum`: Inlines.Bar?): InlinesBuilder = this.apply {
        this.`enum` = `enum`
    }

    public fun product(product: Inlines.Foo?): InlinesBuilder = this.apply {
        this.product = product
    }

    public fun sum(sum: Inlines.Sum?): InlinesBuilder = this.apply {
        this.sum = sum
    }

    public fun build(): Inlines = build(SproutTestFactory.DEFAULT)

    public fun build(factory: SproutTestFactory = SproutTestFactory.DEFAULT): Inlines =
        factory.inlines(enum = enum, product = product!!, sum = sum!!)
}

public class InlinesFooBuilder {
    public var x: Int? = null

    public var y: Inlines.Bar? = null

    public fun x(x: Int?): InlinesFooBuilder = this.apply {
        this.x = x
    }

    public fun y(y: Inlines.Bar?): InlinesFooBuilder = this.apply {
        this.y = y
    }

    public fun build(): Inlines.Foo = build(SproutTestFactory.DEFAULT)

    public fun build(factory: SproutTestFactory = SproutTestFactory.DEFAULT): Inlines.Foo =
        factory.inlinesFoo(x = x!!, y = y!!)
}

public class InlinesSumUBuilder {
    public var foo: String? = null

    public fun foo(foo: String?): InlinesSumUBuilder = this.apply {
        this.foo = foo
    }

    public fun build(): Inlines.Sum.U = build(SproutTestFactory.DEFAULT)

    public fun build(factory: SproutTestFactory = SproutTestFactory.DEFAULT): Inlines.Sum.U =
        factory.inlinesSumU(foo = foo!!)
}

public class InlinesSumVBuilder {
    public var bar: String? = null

    public fun bar(bar: String?): InlinesSumVBuilder = this.apply {
        this.bar = bar
    }

    public fun build(): Inlines.Sum.V = build(SproutTestFactory.DEFAULT)

    public fun build(factory: SproutTestFactory = SproutTestFactory.DEFAULT): Inlines.Sum.V =
        factory.inlinesSumV(bar = bar!!)
}