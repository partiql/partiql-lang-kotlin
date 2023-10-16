package org.partiql.planner.test

import java.io.File

public class PartiQLTestGroup(
    public val name: String,
    public val inputs: List<PartiQLTestCase>,
) {

    companion object {

        public fun load(dir: File): PartiQLTestGroup {
            val tests = dir.listFiles()!!.flatMap { loadAllTests(it) }
            return PartiQLTestGroup(dir.name, tests)
        }

        public fun loadAllTests(file: File): List<PartiQLTestCase> {
            val tests = mutableListOf<PartiQLTestCase>()
            var name = ""
            val statement = StringBuilder()
            for (line in file.readLines()) {

                // start of test
                if (line.startsWith("--#[") and line.endsWith("]")) {
                    name = line.substring(4, line.length - 1)
                    statement.clear()
                }

                if (name.isNotEmpty() && line.isNotBlank()) {
                    // accumulating test statement
                    statement.appendLine(line)
                } else {
                    // skip these lines
                    continue
                }

                // Finish & Reset
                if (line.endsWith(";")) {
                    tests.add(PartiQLTestCase(name, statement.toString()))
                    name = ""
                    statement.clear()
                }
            }
            return tests
        }
    }
}
