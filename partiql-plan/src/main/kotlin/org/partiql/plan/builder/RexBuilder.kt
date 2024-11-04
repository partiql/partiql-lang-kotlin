package org.partiql.plan.builder

import org.partiql.plan.rel.Rel
import org.partiql.plan.rex.Rex
import org.partiql.plan.rex.RexStruct
import org.partiql.plan.rex.RexSubqueryTest
import org.partiql.spi.catalog.Table
import org.partiql.spi.value.Datum
import org.partiql.types.PType

/**
 * DataFrame style fluent-builder for PartiQL logical plans.
 */
@Suppress("LocalVariableName")
public class RexBuilder private constructor(rex: Builder) {

    // DO NOT USE FINAL MEMBERS
    private var self: Builder = rex

    /**
     * Invoke the builder with the default [PlanFactory] implementation.
     */
    public fun build(): Rex = build(PlanFactory.STANDARD)

    /**
     * Invoke the builder with the given [PlanFactory] implementation.
     */
    public fun build(factory: PlanFactory): Rex = self.build(factory)

    /**
     * This object holds named constructors for the [RexBuilder] class.
     */
    public companion object {

        @JvmStatic
        public fun variable(offset: Int): RexBuilder = RexBuilder {
            it.rexVar(0, offset)
        }

        @JvmStatic
        public fun variable(depth: Int, offset: Int): RexBuilder = RexBuilder {
            it.rexVar(depth, offset)
        }

        @JvmStatic
        public fun table(table: Table): RexBuilder = RexBuilder {
            it.rexTable(table)
        }

        @JvmStatic
        public fun lit(value: Boolean): RexBuilder = lit(Datum.bool(value))

        @JvmStatic
        public fun lit(value: Int): RexBuilder = lit(Datum.integer(value))

        @JvmStatic
        public fun lit(value: Long): RexBuilder = lit(Datum.bigint(value))

        @JvmStatic
        public fun lit(value: String): RexBuilder = lit(Datum.string(value))

        @JvmStatic
        public fun lit(value: Datum): RexBuilder = RexBuilder { it.rexLit(value) }

        @JvmStatic
        public fun array(values: Collection<Rex>): RexBuilder = RexBuilder { it.rexArray(values) }

        @JvmStatic
        public fun bag(values: Collection<Rex>): RexBuilder = RexBuilder { it.rexBag(values) }

        @JvmStatic
        public fun coalesce(args: List<Rex>): RexBuilder = RexBuilder {
            it.rexCoalesce(args)
        }

        /**
         * TODO add some vararg and vararg pair overloads.
         */
        @JvmStatic
        public fun struct(fields: List<RexStruct.Field>): RexBuilder = RexBuilder {
            it.rexStruct(fields)
        }

        /**
         * Spread because it's similar to the struct/dict spread of other languages. { x..., y... }
         */
        @JvmStatic
        public fun spread(args: List<Rex>): RexBuilder = RexBuilder {
            it.rexSpread(args)
        }

        /**
         * Scalar subquery coercion.
         */
        @JvmStatic
        public fun subquery(rel: RelBuilder): RexBuilder = RexBuilder {
            throw UnsupportedOperationException("subquery builders are removed until supported in partiql-planner")
        }

        /**
         * Subquery EXISTS.
         */
        @JvmStatic
        public fun exists(rel: RelBuilder): RexBuilder = RexBuilder {
            val _rel = rel.build(it)
            it.rexSubqueryTest(RexSubqueryTest.Test.EXISTS, _rel)
        }

        /**
         * Subquery UNIQUE.
         */
        @JvmStatic
        public fun unique(rel: RelBuilder): RexBuilder = RexBuilder {
            val _rel = rel.build(it)
            it.rexSubqueryTest(RexSubqueryTest.Test.UNIQUE, _rel)
        }

        /**
         * Creates a RexPivot operator.
         */
        @JvmStatic
        public fun pivot(input: RelBuilder, key: RexBuilder, value: RexBuilder): RexBuilder = RexBuilder {
            val _input = input.build(it)
            val _key = key.build(it)
            val _value = value.build(it)
            it.rexPivot(_input, _key, _value)
        }

        /**
         * Creates a RexSelect operator.
         *
         * @param input
         * @param constructor
         * @return
         */
        @JvmStatic
        public fun select(input: RelBuilder, constructor: RexBuilder): RexBuilder = RexBuilder {
            val _input = input.build(it)
            val _constructor = constructor.build(it)
            it.rexSelect(_input, _constructor)
        }
    }

    /**
     * Appends a RexCast to the current rex builder.
     */
    public fun cast(target: PType): RexBuilder = RexBuilder {
        val _operand = self.build(it)
        it.rexCast(_operand, target)
    }

    /**
     * Appends a RexPathKey (or RexPathSymbol) to the current rex builder.
     *
     * @param key
     * @return
     */
    public fun path(key: String, caseInsensitive: Boolean = false): RexBuilder = RexBuilder {
        val _key = it.rexLit(Datum.string(key))
        val _operand = self.build(it)
        if (caseInsensitive) {
            it.rexPathSymbol(_operand, key)
        } else {
            it.rexPathKey(_operand, _key)
        }
    }

    /**
     * Appends a RexPathIndex to the current rex builder.
     *
     * @param index
     * @return
     */
    public fun path(index: Int): RexBuilder = RexBuilder {
        val _index = it.rexLit(Datum.integer(index))
        val _operand = self.build(it)
        it.rexPathIndex(_operand, _index)
    }

    /**
     * Transform the [Rex] operator into a RelScan operator - this a rex->rel projection.
     */
    public fun scan(): RelBuilder = RelBuilder.scan(this)

    /**
     * Transform the [Rel] operator into a RelIterate operator – this is rex->rel projection.
     *
     * @return
     */
    public fun iterate(): RelBuilder = RelBuilder.iterate(this)

    /**
     * The UNPIVOT expression-to-relation projection.
     */
    public fun unpivot(): RelBuilder = RelBuilder.unpivot(this)

    /**
     * PRIVATE FUNCTIONAL INTERFACE COMPILES DOWN TO PRIVATE STATIC METHODS.
     */
    private fun interface Builder {
        fun build(factory: PlanFactory): Rex
    }
}
