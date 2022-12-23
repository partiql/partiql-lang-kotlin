package org.partiql.isl.internal

import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.emptyIonStruct
import com.amazon.ionelement.api.ionListOf
import com.amazon.ionelement.api.ionStructOf
import com.amazon.ionelement.api.ionSymbol
import org.partiql.isl.Footer
import org.partiql.isl.Header
import org.partiql.isl.IonSchemaNode
import org.partiql.isl.Schema
import org.partiql.isl.Type
import org.partiql.isl.Version
import org.partiql.isl.visitor.IonSchemaVisitor

object IonSchemaWriter {

    fun toIon(schema: Schema): Iterator<IonElement> {
        val document = mutableListOf<IonElement>()
        document.apply {
            when (schema.version) {
                Version.V1_0 -> {}
                Version.V2_0 -> add(ionSymbol("\$ion_schema_2_0"))
            }
            add(Visitor.visit(schema.header, null))
            schema.definitions.forEach { add(Visitor.visit(it, null)!!) }
            add(Visitor.visit(schema.footer, null))
        }
        return document.iterator()
    }

    private object Visitor : IonSchemaVisitor<IonElement, Unit>() {

        override fun visit(node: Header, ctx: Unit?): IonElement {
            val imports = visit(node.imports)
            val userReservedFields = visit(node.userReservedFields, null)!!
            val struct = ionStructOf(
                "user_reserved_fields" to userReservedFields,
                "imports" to imports,
            )
            return struct.withAnnotations("schema_header")
        }

        override fun visit(node: Type, ctx: Unit?): IonElement {
            TODO()
        }

        override fun visit(node: Footer, ctx: Unit?) = emptyIonStruct().withAnnotations("schema_footer")

        private fun visit(nodes: List<IonSchemaNode>) = ionListOf(nodes.map { it.accept(this, null)!! })
    }
}
