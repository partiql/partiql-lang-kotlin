package org.partiql.plan.v1.rex

import org.partiql.eval.value.Datum
import org.partiql.plan.v1.rel.Rel
import org.partiql.plan.v1.rel.RelBuilder
import org.partiql.types.PType

/**
 * DataFrame style fluent-builder for PartiQL logical plans.
 *
 * TODO schemas and field names.
 * TODO call expressions.
 * TODO list SOME/ANY/ALL CONTAINS etc.
 */
public class RexBuilder internal constructor(rex: Rex) {

    // KEEP FINAL TO ENSURE ITS NEVER MUTATED
    private val _rex = rex

    // TODO CONSIDER BINARY OPERATORS

    /**
     * The CAST(<_rex> AS <target>) operator.
     *
     * @param target
     * @return
     */
    public fun cast(target: PType): RexBuilder {
        val rex = object : RexCast.Base(_rex, target) {}
        return RexBuilder(rex)
    }

    /**
     * The SCAN expression-to-relation projects — i.e. FROM <rex>
     *
     * @return
     */
    public fun scan(): RelBuilder = RelBuilder.scan(_rex)

    /**
     * The pathing expressions for keys and symbols — i.e. <rex>.<key> / <rex>['key']
     *
     * @param key           The key expression.
     * @param insensitive   If true, then a symbol lookup is used.
     * @return
     */
    public fun path(key: String, insensitive: Boolean = false): RexBuilder {
        TODO("not implemented")
    }

    public fun index(index: Rex): RexBuilder {
        TODO("not implemented")
    }

    /**
     * The UNPIVOT expression-to-relation projection.
     */
    public fun unpivot(): RelBuilder = RelBuilder.unpivot(_rex)

    public companion object {

        /**
         * TODO NAMING??
         */
        @JvmStatic
        public fun local(depth: Int, offset: Int): RexBuilder {
            val rex = object : RexVar.Base(depth, offset) {}
            return RexBuilder(rex)
        }

        @JvmStatic
        public fun lit(value: Datum): RexBuilder {
            val rex = object : RexLit.Base(value) {}
            return RexBuilder(rex)
        }

        @JvmStatic
        public fun collection(values: List<Rex>): RexBuilder {
            val rex = object : RexCollection.Base(values) {}
            return RexBuilder(rex)
        }

        @JvmStatic
        public fun coalesce(args: List<Rex>): RexBuilder {
            val rex = object : RexCoalesce.Base(args) {}
            return RexBuilder(rex)
        }

        /**
         * Scalar subquery coercion.
         */
        @JvmStatic
        public fun subquery(rel: Rel): RexBuilder {
            val rex = object : RexSubquery.Base(rel) {}
            return RexBuilder(rex)
        }

        /**
         * TODO add some vararg and vararg pair overloads.
         */
        @JvmStatic
        public fun struct(fields: List<RexStructField>): RexBuilder {
            val rex = object : RexStruct.Base(fields) {}
            return RexBuilder(rex)
        }

        /**
         * The TUPLEUNION function (which could just be a scalar function..)
         *
         * Spread because it's similar to the struct/dict spread of other languages. { x..., y... }
         *
         * TODO NAMING??
         */
        @JvmStatic
        public fun spread(args: List<Rex>): RexBuilder {
            val rex = object : RexTupleUnion.Base(args) {}
            return RexBuilder(rex)
        }
    }
}
