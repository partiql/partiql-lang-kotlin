package org.partiql.planner.test

import java.nio.file.Path
import java.util.stream.Stream
import kotlin.io.path.toPath

class PartiQLTestProvider(root: Path? = null) {

    private val groups: List<PartiQLTestGroup>

    init {
        val default = PartiQLTestCase::class.java.getResource("/inputs")!!.toURI().toPath()
        val inputsDir = (root ?: default).toFile()
        groups = inputsDir.listFiles { f -> f.isDirectory }!!.map { PartiQLTestGroup.load(it) }
    }

    /**
     * Return test inputs associated by groups.
     */
    public fun groups(): Stream<PartiQLTestGroup> {
        return groups.stream()
    }

    /**
     * Return test inputs associated by test key <group>__<id>.
     */
    public fun inputs(): Map<String, PartiQLTestCase> {
        val map = mutableMapOf<String, PartiQLTestCase>()
        for (group in groups) {
            for (test in group.inputs) {
                val key = "${group.name}__${test.id}"
                map[key] = test
            }
        }
        return map
    }
}
