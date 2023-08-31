package org.partiql.transpiler.test.targets.athena

import com.amazon.ionelement.api.StringElement
import com.amazon.ionelement.api.StructElement
import org.partiql.planner.test.getAngry

public class AthenaTargetTest(
    public val statement: String,
) {

    companion object {

        fun load(ion: StructElement): AthenaTargetTest {
            // Load
            val statementE = ion.getAngry<StringElement>("statement")
            // Parse
            val statement = statementE.textValue
            return AthenaTargetTest(statement)
        }
    }
}
