package org.partiql.engine.impl.relation

import org.partiql.engine.impl.PhysicalNode
import org.partiql.engine.impl.Record
import org.partiql.value.PartiQLValueExperimental

internal class Projection(
    private val input: PhysicalNode.Relation,
    val projections: List<PhysicalNode.Expression>
) : PhysicalNode.Relation {
    @OptIn(PartiQLValueExperimental::class)
    override fun evaluate(): Iterator<Record> {

        val inputIter = input.evaluate()

        return object : Iterator<Record> {
            override fun hasNext(): Boolean {
                return inputIter.hasNext()
            }

            override fun next(): Record {
                val inputRecord = inputIter.next()
                return Record(
                    projections.map { it.evaluate(inputRecord) }
                )
            }
        }
    }
}
