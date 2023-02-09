package org.partiql.ir.rel.builder

import org.partiql.ir.rel.Binding
import org.partiql.ir.rel.Common
import org.partiql.ir.rel.PlanNode
import org.partiql.ir.rel.Property
import org.partiql.ir.rel.Rel
import org.partiql.lang.domains.PartiqlAst.Expr

/**
 * The Builder is inside this private final class for DSL aesthetics
 */
public class Plan private constructor() {
    @Suppress("ClassName")
    public class Builder(
        private val factory: PlanFactory
    ) {
        public fun common(
            schema: MutableMap<String, Rel.Join.Type> = mutableMapOf(),
            properties: MutableSet<Property> = mutableSetOf(),
            metas: MutableMap<String, Any> = mutableMapOf(),
            block: _Common.() -> Unit = {}
        ): Common {
            val b = _Common(schema, properties, metas)
            b.block()
            return factory.common(schema = b.schema, properties = b.properties, metas = b.metas)
        }

        public fun relScan(
            common: Common? = null,
            `value`: Expr? = null,
            `as`: String? = null,
            at: String? = null,
            `by`: String? = null,
            block: _RelScan.() -> Unit = {}
        ): Rel.Scan {
            val b = _RelScan(common, value, `as`, at, by)
            b.block()
            return factory.relScan(
                common = b.common!!, value = b.value!!, `as` = b.`as`, at = b.at, by = b.by
            )
        }

        public fun relCross(
            common: Common? = null,
            lhs: Rel? = null,
            rhs: Rel? = null,
            block: _RelCross.() -> Unit = {}
        ): Rel.Cross {
            val b = _RelCross(common, lhs, rhs)
            b.block()
            return factory.relCross(common = b.common!!, lhs = b.lhs!!, rhs = b.rhs!!)
        }

        public fun relFilter(
            common: Common? = null,
            input: Rel? = null,
            condition: Expr? = null,
            block: _RelFilter.() -> Unit = {}
        ): Rel.Filter {
            val b = _RelFilter(common, input, condition)
            b.block()
            return factory.relFilter(common = b.common!!, input = b.input!!, condition = b.condition!!)
        }

        public fun relSort(
            common: Common? = null,
            expr: Expr? = null,
            dir: Rel.Sort.Dir? = null,
            nulls: Rel.Sort.Nulls? = null,
            block: _RelSort.() -> Unit = {}
        ): Rel.Sort {
            val b = _RelSort(common, expr, dir, nulls)
            b.block()
            return factory.relSort(common = b.common!!, expr = b.expr!!, dir = b.dir!!, nulls = b.nulls!!)
        }

        public fun relBag(
            common: Common? = null,
            lhs: Rel? = null,
            rhs: Rel? = null,
            op: Rel.Bag.Op? = null,
            block: _RelBag.() -> Unit = {}
        ): Rel.Bag {
            val b = _RelBag(common, lhs, rhs, op)
            b.block()
            return factory.relBag(common = b.common!!, lhs = b.lhs!!, rhs = b.rhs!!, op = b.op!!)
        }

        public fun relFetch(
            common: Common? = null,
            input: Rel? = null,
            limit: Long? = null,
            offset: Long? = null,
            block: _RelFetch.() -> Unit = {}
        ): Rel.Fetch {
            val b = _RelFetch(common, input, limit, offset)
            b.block()
            return factory.relFetch(
                common = b.common!!, input = b.input!!, limit = b.limit!!, offset = b.offset!!
            )
        }

        public fun relProject(
            common: Common? = null,
            input: Rel? = null,
            exprs: MutableList<Binding> = mutableListOf(),
            block: _RelProject.() -> Unit = {}
        ): Rel.Project {
            val b = _RelProject(common, input, exprs)
            b.block()
            return factory.relProject(common = b.common!!, input = b.input!!, exprs = b.exprs)
        }

        public fun relJoin(
            common: Common? = null,
            lhs: Rel? = null,
            rhs: Rel? = null,
            condition: Expr? = null,
            type: Rel.Join.Type? = null,
            block: _RelJoin.() -> Unit = {}
        ): Rel.Join {
            val b = _RelJoin(common, lhs, rhs, condition, type)
            b.block()
            return factory.relJoin(
                common = b.common!!, lhs = b.lhs!!, rhs = b.rhs!!, condition = b.condition, type = b.type!!
            )
        }

        public fun relAggregate(
            common: Common? = null,
            input: Rel? = null,
            calls: MutableList<Binding> = mutableListOf(),
            groups: MutableList<Expr> = mutableListOf(),
            block: _RelAggregate.() -> Unit = {}
        ): Rel.Aggregate {
            val b = _RelAggregate(common, input, calls, groups)
            b.block()
            return factory.relAggregate(
                common = b.common!!, input = b.input!!, calls = b.calls, groups = b.groups
            )
        }

        public fun binding(
            name: String? = null,
            `value`: Expr? = null,
            block: _Binding.() -> Unit = {}
        ): Binding {
            val b = _Binding(name, value)
            b.block()
            return factory.binding(name = b.name!!, value = b.value!!)
        }

        public class _Common(
            public var schema: MutableMap<String, Rel.Join.Type> = mutableMapOf(),
            public var properties: MutableSet<Property> = mutableSetOf(),
            public var metas: MutableMap<String, Any> = mutableMapOf()
        )

        public class _RelScan(
            public var common: Common? = null,
            public var `value`: Expr? = null,
            public var `as`: String? = null,
            public var at: String? = null,
            public var `by`: String? = null
        )

        public class _RelCross(
            public var common: Common? = null,
            public var lhs: Rel? = null,
            public var rhs: Rel? = null
        )

        public class _RelFilter(
            public var common: Common? = null,
            public var input: Rel? = null,
            public var condition: Expr? = null
        )

        public class _RelSort(
            public var common: Common? = null,
            public var expr: Expr? = null,
            public var dir: Rel.Sort.Dir? = null,
            public var nulls: Rel.Sort.Nulls? = null
        )

        public class _RelBag(
            public var common: Common? = null,
            public var lhs: Rel? = null,
            public var rhs: Rel? = null,
            public var op: Rel.Bag.Op? = null
        )

        public class _RelFetch(
            public var common: Common? = null,
            public var input: Rel? = null,
            public var limit: Long? = null,
            public var offset: Long? = null
        )

        public class _RelProject(
            public var common: Common? = null,
            public var input: Rel? = null,
            public var exprs: MutableList<Binding> = mutableListOf()
        )

        public class _RelJoin(
            public var common: Common? = null,
            public var lhs: Rel? = null,
            public var rhs: Rel? = null,
            public var condition: Expr? = null,
            public var type: Rel.Join.Type? = null
        )

        public class _RelAggregate(
            public var common: Common? = null,
            public var input: Rel? = null,
            public var calls: MutableList<Binding> = mutableListOf(),
            public var groups: MutableList<Expr> = mutableListOf()
        )

        public class _Binding(
            public var name: String? = null,
            public var `value`: Expr? = null
        )
    }

    public companion object {
        @JvmStatic
        public fun <T : PlanNode> build(
            factory: PlanFactory = PlanFactory.DEFAULT,
            block: Builder.() -> T
        ) = Builder(factory).block()

        @JvmStatic
        public fun <T : PlanNode> create(block: PlanFactory.() -> T) = PlanFactory.DEFAULT.block()
    }
}
