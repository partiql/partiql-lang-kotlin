package org.partiql.transpiler.test.targets.partiql

import com.amazon.ionelement.api.StringElement
import com.amazon.ionelement.api.StructElement
import org.partiql.planner.test.getAngry

private typealias PartiQLTargetTests = Map<String, PartiQLTargetTest>

public class PartiQLTargetTestSuite(
    public val name: String,
    public val tests: PartiQLTargetTests,
) {

    companion object {

        /**
         * Eventually replace with something more robust.
         */
        public fun load(ion: StructElement): PartiQLTargetTestSuite {
            // Load
            val nameE = ion.getAngry<StringElement>("name")
            val testsE = ion.getAngry<StructElement>("tests")
            // Parse
            val name = nameE.textValue
            val tests = testsE.fields.associate {
                assert(it.value is StructElement) { "Test value must be an Ion struct" }
                it.name to PartiQLTargetTest.load(it.value as StructElement)
            }
            return PartiQLTargetTestSuite(name, tests)
        }
    }
}