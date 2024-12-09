package org.partiql.eval.internal

import org.partiql.spi.value.Datum
import org.partiql.spi.value.Field
import org.partiql.types.PType

object DatumMaterialize {
    @JvmStatic
    fun materialize(datum: Datum): Datum {
        val type = datum.type
        val kind = type.code()
        if (datum.isNull) {
            return Datum.nullValue(type)
        }
        if (datum.isMissing) {
            return Datum.missing(type)
        }
        return when (kind) {
            PType.BAG -> {
                val elements = datum.map { materialize(it) }
                Datum.bag(elements)
            }
            PType.ARRAY -> {
                val elements = datum.map { materialize(it) }
                Datum.array(elements)
            }
            // TODO: Need to create a Datum.row()
            PType.STRUCT, PType.ROW -> {
                val fieldIter = datum.fields.iterator()
                val newFields = mutableListOf<Field>()
                fieldIter.forEach {
                    newFields.add(Field.of(it.name, materialize(it.value)))
                }
                Datum.struct(newFields)
            }
            else -> datum
        }
    }
}
