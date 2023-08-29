package org.partiql.transpiler.test

import com.amazon.ionelement.api.StringElement
import com.amazon.ionelement.api.StructElement

/**
 * Test names are the map key.
 */
private typealias TranspilerTests = Map<String, TranspilerTest>

public class TranspilerTestSuite(
    public val name: String,
    public val session: TranspilerTestSession,
    public val tests: TranspilerTests,
) {

    companion object {

        /**
         * Eventually replace with something more robust.
         */
        public fun load(ion: StructElement): TranspilerTestSuite {
            // Load
            val nameE = ion.getAngry<StringElement>("name")
            val sessE = ion.getAngry<StructElement>("session")
            val testsE = ion.getAngry<StructElement>("tests")
            // Parse
            val name = nameE.textValue
            val session = TranspilerTestSession.load(sessE)
            val tests = testsE.fields.associate {
                assert (it.value is StructElement) { "Test value must be an Ion struct" }
                it.name to TranspilerTest.load(it.value as StructElement)
            }
            return TranspilerTestSuite(name, session, tests)
        }
    }
}
