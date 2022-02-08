package org.partiql.lang.ast

import com.amazon.ion.IonWriter
import com.amazon.ion.IonValue
import com.amazon.ionelement.api.loadAllElements
import org.partiql.ionschema.model.IonSchemaModel
import org.partiql.ionschema.model.toIsl
import org.partiql.ionschema.parser.parseSchema
import org.partiql.lang.mappers.ISL_META_KEY
import org.partiql.lang.mappers.IonSchemaMapper
import org.partiql.lang.mappers.StaticTypeMapper
import org.partiql.lang.types.StaticType
import org.partiql.lang.util.IonWriterContext
import org.partiql.lang.util.asIonStruct
import org.partiql.lang.util.field
import org.partiql.lang.util.stringValue

/**
 * Represents a static type for an AST element.
 *
 * Note: The (de)serialization based on ISL mappers does not work as expected but does not break anything since no one uses it today.
 * TODO: issue to track fixing the (de)serialization https://github.com/partiql/partiql-lang-kotlin/issues/512
 */
data class StaticTypeMeta(val type: StaticType) : Meta {

    override fun toString() = type.toString()

    override val tag = TAG

    override fun serialize(writer: IonWriter) {
        var name = "typeName"
        val hasISL = type.metas.containsKey(ISL_META_KEY)
        val isl = when (hasISL) {
            true -> {
                // Get name from metas if present, fallback to base type name
                name = (type.metas[ISL_META_KEY] as List<IonSchemaModel.TypeDefinition>)[0].name?.text ?: name
                IonSchemaMapper(type).toIonSchema(name).toIsl()
            }
            false -> IonSchemaMapper(type).toIonSchema(name).toIsl()
        }.joinToString(" ") { it.toString() }

        IonWriterContext(writer).apply {
            struct {
                string("name", name)
                string("hasISL", hasISL.toString())
                string("isl", isl)
                // TODO add type parameters/constraints
            }
        }
    }

    companion object {
        const val TAG = "\$static_type"

        val deserializer = object : MetaDeserializer {
            override val tag = TAG
            override fun deserialize(value: IonValue): Meta {
                val struct = value.asIonStruct()

                // get serialized fields
                val name = struct.field("name").stringValue()!!
                val hasISL = struct.field("hasISL").stringValue()!!.toBoolean()
                val isl = struct.field("isl").stringValue()!!
                // TODO add type parameters/constraints

                // create StaticType from ISL
                val schema = parseSchema(loadAllElements(isl).toList())
                val staticType = StaticTypeMapper(schema).toStaticType(name)

                // return StaticType with or without metas as appropriate
                return when (hasISL) {
                    true -> StaticTypeMeta(staticType)
                    false -> {
                        StaticTypeMeta(staticType.withMetas(staticType.metas.filterKeys { it != ISL_META_KEY }))
                    }
                }
            }
        }
    }
}

val MetaContainer.staticType: StaticTypeMeta? get() = find(StaticTypeMeta.TAG) as StaticTypeMeta?
