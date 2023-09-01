package org.partiql.transpiler.test.targets.trino

import com.amazon.ionelement.api.StringElement
import com.amazon.ionelement.api.StructElement
import org.partiql.planner.test.getAngry

public class TrinoTargetTest(
    public val statement: String,
) {

    companion object {

        fun load(ion: StructElement): TrinoTargetTest {
            // Load
            val statementE = ion.getAngry<StringElement>("statement")
            // Parse
            val statement = statementE.textValue
            return TrinoTargetTest(statement)
        }
    }
}
