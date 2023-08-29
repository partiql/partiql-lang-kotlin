package org.partiql.transpiler.test

import com.amazon.ionelement.api.ListElement
import com.amazon.ionelement.api.StringElement
import com.amazon.ionelement.api.StructElement
import com.amazon.ionelement.api.TextElement

public class TranspilerTestSession(
    public val catalog: String,
    public val path: List<String>,
    public val vars: Map<String, String>,
) {

    companion object {

        public fun load(ion: StructElement): TranspilerTestSession {
            // Load
            val catalogE = ion.getAngry<StringElement>("catalog")
            val pathE = ion.getAngry<ListElement>("path")
            val varsE = ion.getAngry<StructElement>("vars")
            // Parse
            val catalog = catalogE.textValue
            val path = pathE.values.map { (it as TextElement).textValue }
            val vars = varsE.fields.associate {
                it.name to (it.value as StringElement).textValue
            }
            return TranspilerTestSession(catalog, path, vars)
        }
    }
}
