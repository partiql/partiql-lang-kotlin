package org.partiql.planner

import java.util.stream.Stream
import kotlin.io.path.toPath

/**
 * TODO converge with PartiQLTestProvider.
 */
class PlannerTestProvider {

    private val groups: List<PlannerTestGroup>

    init {
        val default = PlannerTestProvider::class.java.getResource("/cases")!!.toURI().toPath()
        val casesDir = default.toFile()
        groups = casesDir.listFiles { f -> f.isDirectory }!!.map { PlannerTestGroup.load(it) }
    }

    /**
     * Return test cases associated by groups.
     */
    public fun groups(): Stream<PlannerTestGroup> {
        return groups.stream()
    }
}
