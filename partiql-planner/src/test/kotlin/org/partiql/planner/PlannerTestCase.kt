package org.partiql.planner

import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.ListElement
import com.amazon.ionelement.api.StringElement
import com.amazon.ionelement.api.StructElement
import org.partiql.plugins.local.getAngry
import org.partiql.plugins.local.toStaticType
import org.partiql.types.StaticType

data class PlannerTestCase(
    public val input: String,
    public val catalog: String,
    public val catalogPath: List<String>,
    public val schema: StaticType,
) {

    companion object {

        fun load(ion: StructElement): PlannerTestCase {
            // Required
            val input = ion.getAngry<StringElement>("input").textValue
            val catalog = ion.getAngry<StringElement>("catalog").textValue
            val schema = ion.getAngry<IonElement>("schema").toStaticType()
            // Optional
            val catalogPath = mutableListOf<String>()
            val pathArr = ion.getOptional("catalogPath")
            if (pathArr != null && pathArr is ListElement) {
                pathArr.asList().values.forEach { v ->
                    if (v !is StringElement) {
                        error("catalogPath must be a list of strings")
                    }
                    catalogPath.add((v as StringElement).textValue)
                }
            }
            return PlannerTestCase(input, catalog, catalogPath, schema)
        }
    }
}
