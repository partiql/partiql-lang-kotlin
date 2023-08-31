package org.partiql.planner.test

import com.amazon.ionelement.api.StringElement
import com.amazon.ionelement.api.StructElement

/**
 * Test names are the map key.
 */
private typealias PlannerTests = Map<String, PlannerTest>

public class PlannerTestSuite(
    public val name: String,
    public val session: PlannerTestSession,
    public val tests: PlannerTests,
) {

    companion object {

        /**
         * Eventually replace with something more robust.
         */
        public fun load(ion: StructElement): PlannerTestSuite {
            // Load
            val nameE = ion.getAngry<StringElement>("name")
            val sessE = ion.getAngry<StructElement>("session")
            val testsE = ion.getAngry<StructElement>("tests")
            // Parse
            val name = nameE.textValue
            val session = PlannerTestSession.load(sessE)
            val tests = testsE.fields.associate {
                assert(it.value is StructElement) { "Test value must be an Ion struct" }
                it.name to PlannerTest.load(it.value as StructElement)
            }
            return PlannerTestSuite(name, session, tests)
        }
    }
}
