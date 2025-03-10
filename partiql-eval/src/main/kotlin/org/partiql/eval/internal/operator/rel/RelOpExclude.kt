package org.partiql.eval.internal.operator.rel

import org.partiql.eval.Environment
import org.partiql.eval.ExprRelation
import org.partiql.eval.Row
import org.partiql.plan.Exclusion
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Field

/**
 * Implementation of the EXCLUDE clause; there are good opportunities to tune/optimize this.
 *
 * Consider more memoization, use arrays, combine coll/struct exclusions in one method, ano others!
 */
internal class RelOpExclude(
    private val input: ExprRelation,
    private val exclusions: List<Exclusion>,
) : ExprRelation {

    override fun open(env: Environment) {
        input.open(env)
    }

    override fun hasNext(): Boolean {
        return input.hasNext()
    }

    override fun next(): Row {
        val record = input.next()
        exclusions.forEach { exclusion ->
            // TODO memoize offsets and steps (i.e. don't call getVar(), getOffset(), and getItems() every time).
            val o = exclusion.getVar().getOffset()
            val value = record.values[o]
            record.values[o] = value.exclude(exclusion.getItems())
        }
        return record
    }

    override fun close() {
        input.close()
    }

    /**
     * A place to memoize exclusion branches before recursing; we cannot use the Exclude.Item directly
     * because `items` affects the equals/hashcode.
     */
    private class Branches {

        private var collIndex: MutableMap<Int, Exclusion.CollIndex> = mutableMapOf()
        private var collWildcard: Exclusion.CollWildcard? = null
        private val structSymbols: MutableMap<String, Exclusion.StructSymbol> = mutableMapOf()
        private val structKeys: MutableMap<String, Exclusion.StructKey> = mutableMapOf()
        private var structWildcard: Exclusion.StructWildcard? = null

        fun put(item: Exclusion.Item) {
            when (item) {
                is Exclusion.StructWildcard -> structWildcard = item
                is Exclusion.StructSymbol -> structSymbols[item.getSymbol()] = item
                is Exclusion.StructKey -> structKeys[item.getKey()] = item
                is Exclusion.CollIndex -> collIndex[item.getIndex()] = item
                is Exclusion.CollWildcard -> collWildcard = item
            }
        }

        fun getCollIndex(index: Int) = collIndex[index]

        fun getCollWildcard() = collWildcard

        fun getStructKey(name: String) = structKeys[name]

        fun getStructSymbol(cnf: String) = structSymbols[cnf]

        fun getStructWildCard() = structWildcard
    }

    /**
     * Entry-point to apply exclusions to an arbitrary [Datum].
     */
    private fun Datum.exclude(exclusions: List<Exclusion.Item>): Datum = when (this.type.code()) {
        PType.ROW, PType.STRUCT -> this.structExclude(exclusions)
        PType.BAG, PType.ARRAY -> this.collExclude(exclusions)
        else -> this
    }

    private fun Datum.structExclude(exclusions: List<Exclusion.Item>): Datum {
        // keep track of what to exclude.
        val structSymbols = mutableSetOf<String>() // case-normalized to lower.
        val structKeys = mutableSetOf<String>()
        val branches = Branches()
        for (e in exclusions) {
            if (e.hasItems()) {
                // apply later
                branches.put(e)
            } else {
                when (e) {
                    is Exclusion.StructWildcard -> return Datum.struct()
                    is Exclusion.StructSymbol -> structSymbols.add(e.getSymbol().lowercase())
                    is Exclusion.StructKey -> structKeys.add(e.getKey())
                    else -> {} // coll item; do nothing.
                }
            }
        }
        // apply exclusions
        val fields = mutableListOf<Field>()
        for (field in this.fields) {
            // see if any exclusions apply to this field
            val key = field.name
            val symbol = field.name.lowercase() // case-normalized to lower.
            var value = field.value
            // apply exclusions
            if (structKeys.contains(key) || structSymbols.contains(symbol)) {
                continue // skip
            }
            // apply exclusions to subtree
            branches.getStructKey(key)?.let {
                value = value.exclude(it.getItems())
            }
            // apply nested symbols exclusions
            branches.getStructSymbol(symbol)?.let {
                value = value.exclude(it.getItems())
            }
            // apply nested wildcard exclusions
            branches.getStructWildCard()?.let {
                value = value.exclude(it.getItems())
            }

            fields.add(Field.of(key, value))
        }
        return Datum.struct(fields)
    }

    /**
     * Returns a [Datum] created from an iterable of [coll]. Requires [type] to be a collection type
     * (i.e. [PType.ARRAY] or [PType.BAG].).
     */
    private fun newCollValue(type: PType, coll: Iterable<Datum>): Datum {
        return when (type.code()) {
            PType.ARRAY -> Datum.array(coll)
            PType.BAG -> Datum.bag(coll)
            else -> error("Collection type required")
        }
    }

    private fun Datum.collExclude(exclusions: List<Exclusion.Item>): Datum {
        // keep track of what to exclude.
        val collIndexes = mutableSetOf<Int>()
        val branches = Branches()
        for (e in exclusions) {
            if (e.hasItems()) {
                // apply later
                branches.put(e)
            } else {
                when (e) {
                    is Exclusion.CollIndex -> collIndexes.add(e.getIndex())
                    is Exclusion.CollWildcard -> newCollValue(this.type, emptyList())
                    else -> {} // struct item; do nothing
                }
            }
        }

        var i = 0
        val elements = mutableListOf<Datum>()
        for (element in this.iterator()) {
            // apply exclusions
            if (collIndexes.contains(i)) {
                i++
                continue // skip
            }
            // apply exclusions to subtree
            var value = element
            // apply collection index exclusions at deeper levels for lists
            if (type.code() == PType.ARRAY) {
                branches.getCollIndex(i)?.let {
                    value = value.exclude(it.getItems())
                }
            }
            // apply collection wildcard exclusions at deeper levels for lists and bags
            branches.getCollWildcard()?.let {
                value = value.exclude(it.getItems())
            }
            elements.add(value)
        }
        return newCollValue(type, elements)
    }
}
