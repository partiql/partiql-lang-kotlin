package org.partiql.lang.ast

import com.amazon.ion.*
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.types.*
import org.partiql.lang.util.*

/**
 * Represents a static type for an AST element.
 */
data class StaticTypeMeta(val type: StaticType) : Meta {
    override fun toString() = type.toString()

    override val tag = TAG

    override fun serialize(writer: IonWriter) {
        IonWriterContext(writer).apply {
            struct {
                string("name", type.name)
                // TODO add type parameters/constraints
            }
        }
    }

    companion object {
        const val TAG = "\$static_type"

        val deserializer = object : MetaDeserializer {
            override val tag = TAG
            override fun deserialize(sexp: IonSexp): Meta {
                val struct = sexp.first().asIonStruct()
                val typeName = struct.field("name").stringValue()!!
                // TODO add type parameters/constraints

                val staticType = StaticType.fromTypeName(typeName)
                return StaticTypeMeta(staticType)
            }
        }
    }
}

val MetaContainer.staticType: StaticTypeMeta? get() = find(StaticTypeMeta.TAG) as StaticTypeMeta?