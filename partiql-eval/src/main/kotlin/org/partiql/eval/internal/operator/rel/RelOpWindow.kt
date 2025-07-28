package org.partiql.eval.internal.operator.rel

import org.partiql.eval.Environment
import org.partiql.eval.ExprRelation
import org.partiql.eval.ExprValue
import org.partiql.eval.Row
import org.partiql.eval.WindowFunction
import org.partiql.eval.WindowPartition
import org.partiql.eval.internal.helpers.DatumArrayComparator

/**
 * Assume input has been sorted.
 */
internal class RelOpWindow(
    private val input: ExprRelation,
    private val functions: List<WindowFunction>,
    private val partitionBy: List<ExprValue>,
    private val sortBy: List<Collation>
) : RelOpPeeking() {

    private companion object {
        private val comparator = DatumArrayComparator
    }

    private lateinit var _env: Environment

    /**
     * This is used to track the current partition that we are iterating through.
     */
    private var _partition: LocalPartition = LocalPartition()

    /**
     * This is used to track where we are in the partition that we are currently iterating through.
     * @see peek
     */
    private var _partitionPeekingNumber: Long = 0

    /**
     * When lazily creating the partition, we need to step out-of-bounds from the current partition to know
     * whether we are in a new partition. So, we need to put the out-of-bounds row into a place to be used
     * when creating the next partition.
     */
    private var leftoverRow: Row? = null

    override fun openPeeking(env: Environment) {
        input.open(env)
        this._env = env
        _partitionPeekingNumber = -1L
        _partition = LocalPartition()
        functions.map { it.reset(_partition) }
    }

    override fun peek(): Row? {
        // Check if there is an existing partition. If so, evaluate and return.
        _partitionPeekingNumber++
        if (_partition.size() > _partitionPeekingNumber) {
            return produceResult()
        }
        _partitionPeekingNumber = 0L

        // Create new partition's first row
        var partitionCreationIndex = 0L
        val newLocalPartition = LocalPartition()
        val firstRow = when {
            leftoverRow != null -> {
                val tempRow = leftoverRow!!
                leftoverRow = null
                tempRow
            }
            else -> when (input.hasNext()) {
                true -> input.next()
                false -> return null
            }
        }
        var previousInfoIndex = newLocalPartition.add(OrderingInfo(partitionCreationIndex))
        newLocalPartition.add(firstRow, previousInfoIndex)
        val newEnv = _env.push(firstRow)
        val firstRowPartitionKeys = Array(partitionBy.size) { partitionBy[it].eval(newEnv) }
        var previousRowSortKeys = Array(sortBy.size) { sortBy[it].expr.eval(newEnv) }

        // Add partition's remaining rows
        while (input.hasNext()) {
            partitionCreationIndex++
            val nextRow = input.next()
            val nextEnv = _env.push(nextRow)

            // Stop (and save spillover row) if at next partition
            val nextPartitionKeys = Array(partitionBy.size) { partitionBy[it].eval(nextEnv) }
            val isNewPartition = comparator.compare(firstRowPartitionKeys, nextPartitionKeys) != 0
            if (isNewPartition) {
                leftoverRow = nextRow
                break
            }

            // Add next row to partition and update ordering info (if we have reached the next sort group)
            val nextSortKeys = Array(sortBy.size) { sortBy[it].expr.eval(nextEnv) }
            val isNewSortGroup = comparator.compare(previousRowSortKeys, nextSortKeys) != 0
            if (isNewSortGroup) {
                val info = newLocalPartition.getInfo(previousInfoIndex)
                info.orderingEnd = partitionCreationIndex - 1
                previousRowSortKeys = nextSortKeys
                val nextInfo = OrderingInfo(partitionCreationIndex)
                previousInfoIndex = newLocalPartition.add(nextInfo)
            }
            newLocalPartition.add(nextRow, previousInfoIndex)
        }
        _partition = newLocalPartition
        functions.map { it.reset(_partition) }
        return produceResult()
    }

    private class LocalPartition : WindowPartition {
        private val rows: MutableList<Row> = mutableListOf()
        private val orderingInfo: MutableList<OrderingInfo> = mutableListOf()
        private val orderingMap = mutableListOf<Int>()

        fun add(row: Row, info: Int): Int {
            val toReturn = rows.size
            rows.add(row)
            orderingMap.add(info)
            return toReturn
        }

        override operator fun get(index: Long): Row {
            return rows[index.toInt()]
        }

        fun add(info: OrderingInfo): Int {
            val toReturn = orderingInfo.size
            orderingInfo.add(info)
            return toReturn
        }

        fun getInfo(index: Int): OrderingInfo {
            return orderingInfo[orderingMap[index]]
        }

        override fun size(): Long {
            return rows.size.toLong()
        }
    }

    private class OrderingInfo(
        start: Long,
    ) {
        var orderingStart: Long = start
        var orderingEnd: Long = 0
    }

    /**
     * This produces a result using the existing partition.
     */
    private fun produceResult(): Row {
        val row = _partition[_partitionPeekingNumber]
        val info = _partition.getInfo(_partitionPeekingNumber.toInt())
        val newEnv = _env.push(row)
        val functions = functions.map { it.eval(newEnv, info.orderingStart, info.orderingEnd) }
        val outputRow = row.concat(Row.of(*functions.toTypedArray()))
        return outputRow
    }

    override fun closePeeking() {
        input.close()
    }
}
