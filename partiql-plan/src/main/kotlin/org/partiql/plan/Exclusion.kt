package org.partiql.plan

import org.partiql.plan.rex.RexVar

/**
 * An [Exclusion] is represented as a tree of the subsumed (combined) exclusion paths.
 */
public class Exclusion(variable: RexVar, items: List<Item>) {

    // PRIVATE VAR
    private var _variable: RexVar = variable
    private var _items: List<Item> = items

    init {
        if (items.isEmpty()) {
            throw IllegalArgumentException("Exclusion must have at least one item")
        }
    }

    /**
     * Convenience constructor to use varargs.
     */
    public constructor(variable: RexVar, vararg items: Item) : this(variable, items.toList())

    /**
     * The root of an exclusion tree is (currently) always an identifier and therefore a variable ([RexVar]).
     *
     * Developer note: consider a more generic `getRoot(): Rex` if exclusions are later generalized beyond variables.
     */
    public fun getVar(): RexVar = _variable

    /**
     * The list of subsumed (combined) exclusion paths; this forms the tree.
     */
    public fun getItems(): List<Item> = _items

    // generated
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Exclusion) return false

        if (_variable != other._variable) return false
        if (_items != other._items) return false

        return true
    }

    // generated
    override fun hashCode(): Int {
        var result = _variable.hashCode()
        result = 31 * result + _items.hashCode()
        return result
    }

    /**
     * An [Item] is a node of the exclusion tree.
     */
    public interface Item {

        /**
         * True if this is an inner node; false if this is a leaf.
         */
        public fun hasItems(): Boolean

        /**
         * The exclusion subtrees.
         */
        public fun getItems(): List<Item>
    }

    /**
     * Exclude the element at the given index from a collection.
     */
    public class CollIndex internal constructor(index: Int, items: List<Item>) : Item {

        // PRIVATE VAR
        private var _index: Int = index
        private var _items: List<Item> = items
        private var _hasItems: Boolean = _items.isNotEmpty()

        public fun getIndex(): Int = _index

        override fun hasItems(): Boolean = _hasItems

        override fun getItems(): List<Item> = _items

        override fun toString(): String = "[$_index]"

        // generated
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is CollIndex) return false
            if (_index != other._index) return false
            if (_items != other._items) return false
            return true
        }

        // generated
        override fun hashCode(): Int {
            var result = _index
            result = 31 * result + _items.hashCode()
            return result
        }
    }

    /**
     * Exclude all elements from a collection.
     */
    public class CollWildcard internal constructor(items: List<Item>) : Item {

        // PRIVATE VAR
        private var _items: List<Item> = items
        private var _hasItems: Boolean = _items.isNotEmpty()

        override fun hasItems(): Boolean = _hasItems

        override fun getItems(): List<Item> = _items

        override fun toString(): String = "[*]"

        // generated
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is CollWildcard) return false
            if (_items != other._items) return false
            return true
        }

        // generated
        override fun hashCode(): Int {
            return _items.hashCode()
        }
    }

    /**
     * Exclude a key from a struct.
     */
    public class StructKey internal constructor(key: String, items: List<Item>) : Item {

        // PRIVATE VAR
        private var _key: String = key
        private var _items: List<Item> = items
        private var _hasItems: Boolean = _items.isNotEmpty()

        public fun getKey(): String = _key

        override fun hasItems(): Boolean = _hasItems

        override fun getItems(): List<Item> = _items

        override fun toString(): String = "\"$_key\""

        // generated
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is StructKey) return false
            if (_key != other._key) return false
            if (_items != other._items) return false
            return true
        }

        // generated
        override fun hashCode(): Int {
            var result = _key.hashCode()
            result = 31 * result + _items.hashCode()
            return result
        }
    }

    /**
     * Exclude a symbol (case-insensitive key) from a struct.
     */
    public class StructSymbol internal constructor(symbol: String, items: List<Item>) : Item {

        // PRIVATE VAR
        private var _symbol: String = symbol.lowercase() // case-normalize lower
        private var _items: List<Item> = items
        private var _hasItems: Boolean = _items.isNotEmpty()

        public fun getSymbol(): String = _symbol

        override fun hasItems(): Boolean = _hasItems

        override fun getItems(): List<Item> = _items

        override fun toString(): String = _symbol

        // generated
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is StructSymbol) return false
            if (_symbol != other._symbol) return false
            if (_items != other._items) return false
            return true
        }

        // generated
        override fun hashCode(): Int {
            var result = _symbol.hashCode()
            result = 31 * result + _items.hashCode()
            return result
        }
    }

    /**
     * Exclude all keys from a struct.
     */
    public class StructWildcard internal constructor(items: List<Item>) : Item {

        // PRIVATE VAR
        private var _items: List<Item> = items
        private var _hasItems: Boolean = _items.isNotEmpty()

        override fun hasItems(): Boolean = _hasItems

        override fun getItems(): List<Item> = _items

        override fun toString(): String = "*"

        // generated
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is StructWildcard) return false

            if (_items != other._items) return false

            return true
        }

        // generated
        override fun hashCode(): Int {
            return _items.hashCode()
        }
    }

    /**
     * Developer note: overloads are explicitly written for the Java API.
     */
    public companion object {

        @JvmStatic
        public fun collIndex(index: Int): CollIndex = CollIndex(index, emptyList())

        @JvmStatic
        public fun collIndex(index: Int, items: List<Item>): CollIndex = CollIndex(index, items)

        @JvmStatic
        public fun collIndex(index: Int, vararg items: Item): CollIndex = CollIndex(index, items.toList())

        @JvmStatic
        public fun collWildcard(): CollWildcard = CollWildcard(emptyList())

        @JvmStatic
        public fun collWildcard(children: List<Item>): CollWildcard = CollWildcard(children)

        @JvmStatic
        public fun structKey(key: String): StructKey = StructKey(key, emptyList())

        @JvmStatic
        public fun structKey(key: String, children: List<Item>): StructKey = StructKey(key, children)

        @JvmStatic
        public fun structSymbol(symbol: String): StructSymbol = StructSymbol(symbol, emptyList())

        @JvmStatic
        public fun structSymbol(symbol: String, children: List<Item>): StructSymbol = StructSymbol(symbol, children)

        @JvmStatic
        public fun structWildCard(): StructWildcard = StructWildcard(emptyList())

        @JvmStatic
        public fun structWildCard(children: List<Item>): StructWildcard = StructWildcard(children)
    }
}
