package org.partiql.transpiler.test.targets.trino

import com.amazon.ionelement.api.StringElement
import com.amazon.ionelement.api.StructElement
import org.partiql.planner.test.getAngry

private typealias PartiQLTargetTests = Map<String, TrinoTargetTest>

public class TrinoTargetTestSuite(
    public val name: String,
    public val tests: PartiQLTargetTests,
) {

    companion object {

        /**
         * Eventually replace with something more robust.
         */
        public fun load(ion: StructElement): TrinoTargetTestSuite {
            // Load
            val suiteE = ion.getAngry<StringElement>("suite")
            val testsE = ion.getAngry<StructElement>("tests")
            // Parse
            val name = suiteE.textValue
            val tests = testsE.fields.associate {
                assert(it.value is StructElement) { "Test value must be an Ion struct" }
                it.name to TrinoTargetTest.load(it.value as StructElement)
            }
            return TrinoTargetTestSuite(name, tests)
        }
    }
}
