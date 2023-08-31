package org.partiql.planner.test

import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.StringElement
import com.amazon.ionelement.api.StructElement
import org.partiql.types.StaticType

/**
 * TODO replace or improve [StaticType].
 */
public class PlannerTest(
    public val statement: String,
    public val schema: StaticType,
) {

    companion object {

        fun load(ion: StructElement): PlannerTest {
            // Load
            val statementE = ion.getAngry<StringElement>("statement")
            val schemaE = ion.getAngry<IonElement>("schema")
            // Parse
            val statement = statementE.textValue
            val schema = schemaE.toStaticType()
            return PlannerTest(statement, schema)
        }
    }
}
