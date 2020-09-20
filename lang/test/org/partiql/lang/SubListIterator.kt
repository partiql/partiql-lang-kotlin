package org.partiql.lang

class SubListIterator<T>(private val backingList: List<T>) : ListIterator<T> {
    private var idx = -1;

    override fun hasNext(): Boolean = (backingList.size - 1) > idx

    override fun hasPrevious(): Boolean = idx >= 1

    override fun next(): T {
        if(!hasNext()) throw NoSuchElementException()
        return backingList[++idx]
    }

    override fun nextIndex(): Int = idx

    override fun previous(): T {
        if(!hasPrevious()) throw NoSuchElementException()
        return backingList[--idx]
    }

    override fun previousIndex(): Int = idx - 1

    fun subListIterator(fromIdx: Int): SubListIterator<T> {
        val newIdx = idx + fromIdx
        if(newIdx >= backingList.size) throw NoSuchElementException()
        return SubListIterator(backingList.subList(idx + fromIdx, backingList.size))
    }

    fun skipToEnd() {
        idx = backingList.size - 1
    }

    fun clone(): SubListIterator<T> = SubListIterator(backingList).also { it.idx = this.idx }
}