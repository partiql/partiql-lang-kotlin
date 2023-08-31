package org.partiql.transpiler.test.targets.partiql

import com.amazon.ionelement.api.StringElement
import com.amazon.ionelement.api.StructElement
import org.partiql.planner.test.getAngry

public class PartiQLTargetTest(
    public val statement: String,
) {

    companion object {

        fun load(ion: StructElement): PartiQLTargetTest {
            // Load
            val statementE = ion.getAngry<StringElement>("statement")
            // Parse
            val statement = statementE.textValue
            return PartiQLTargetTest(statement)
        }
    }
}
