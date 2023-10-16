package org.partiql.planner

import com.amazon.ionelement.api.StructElement
import com.amazon.ionelement.api.loadAllElements
import java.io.File

data class PlannerTestGroup(
    public val name: String,
    public val cases: List<PlannerTestCase>,
) {

    companion object {

        public fun load(dir: File): PlannerTestGroup {
            val tests = dir.listFiles()!!.flatMap { loadAllTests(it) }
            return PlannerTestGroup(dir.name, tests)
        }

        public fun loadAllTests(file: File): List<PlannerTestCase> {
            val text = file.readText()
            val ion = loadAllElements(text)
            return ion.map { PlannerTestCase.load(it as StructElement) }
        }
    }
}
