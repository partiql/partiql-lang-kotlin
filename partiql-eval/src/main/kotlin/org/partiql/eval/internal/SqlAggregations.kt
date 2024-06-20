package org.partiql.eval.internal

internal class SqlAggregations private constructor(private val map: Map<String, Aggregation>) {

    fun get(specific: String): Aggregation? = map[specific]

    companion object {



    }

}
