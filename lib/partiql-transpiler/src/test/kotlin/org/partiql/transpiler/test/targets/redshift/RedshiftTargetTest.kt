package org.partiql.transpiler.test.targets.redshift

import com.amazon.ionelement.api.StringElement
import com.amazon.ionelement.api.StructElement
import org.partiql.planner.test.getAngry

public class RedshiftTargetTest(
    public val statement: String,
) {

    companion object {

        fun load(ion: StructElement): RedshiftTargetTest {
            // Load
            val statementE = ion.getAngry<StringElement>("statement")
            // Parse
            val statement = statementE.textValue
            return RedshiftTargetTest(statement)
        }
    }
}
