package org.partiql.runner.util

import com.amazon.ion.Decimal
import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.StructField
import com.amazon.ionelement.api.field
import com.amazon.ionelement.api.ionBool
import com.amazon.ionelement.api.ionDecimal
import com.amazon.ionelement.api.ionFloat
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionListOf
import com.amazon.ionelement.api.ionNull
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionStructOf
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

// For error display only
internal fun Datum.toIonElement(): IonElement {
    return when {
        this.isNull() -> ionNull()
        this.isMissing() -> ionNull()
        else -> {
            when (this.type.code()) {
                PType.BOOL -> ionBool(this.getBoolean())
                PType.INTEGER -> ionInt(this.getInt().toLong())
                PType.BIGINT -> ionInt(this.getLong())
                PType.STRING -> ionString(this.getString())
                PType.DECIMAL -> ionDecimal(Decimal.valueOf(this.getBigDecimal()))
                PType.DOUBLE -> ionFloat(this.getDouble())
                PType.REAL -> ionFloat(this.getFloat().toDouble())
                PType.ARRAY -> {
                    val elements = mutableListOf<IonElement>()
                    for (element in this) {
                        elements.add(element.toIonElement())
                    }
                    ionListOf(elements)
                }
                PType.BAG -> {
                    val elements = mutableListOf<IonElement>()
                    for (element in this) {
                        elements.add(element.toIonElement())
                    }
                    ionListOf(elements).withAnnotations("\$bag")
                }
                PType.STRUCT -> {
                    val fields = mutableListOf<StructField>()
                    val fieldIterator = this.getFields()
                    while (fieldIterator.hasNext()) {
                        val field = fieldIterator.next()
                        fields.add(field(field.name, field.value.toIonElement()))
                    }
                    ionStructOf(fields)
                }
                PType.VARIANT -> {
                    val lowered = this.lower()
                    lowered.toIonElement().withAnnotations("\$ion")
                }
                else -> ionString(this.toString())
            }
        }
    }
}
