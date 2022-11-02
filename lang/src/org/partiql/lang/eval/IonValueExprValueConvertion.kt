package org.partiql.lang.eval

import com.amazon.ion.IonBlob
import com.amazon.ion.IonBool
import com.amazon.ion.IonClob
import com.amazon.ion.IonDecimal
import com.amazon.ion.IonFloat
import com.amazon.ion.IonInt
import com.amazon.ion.IonList
import com.amazon.ion.IonReader
import com.amazon.ion.IonSexp
import com.amazon.ion.IonString
import com.amazon.ion.IonStruct
import com.amazon.ion.IonSymbol
import com.amazon.ion.IonSystem
import com.amazon.ion.IonTimestamp
import com.amazon.ion.IonType
import com.amazon.ion.IonValue
import com.amazon.ion.Timestamp
import org.partiql.lang.eval.time.NANOS_PER_SECOND
import org.partiql.lang.eval.time.Time
import org.partiql.lang.util.bytesValue
import org.partiql.lang.util.errAmbiguousBinding
import java.math.BigDecimal

fun ExprValue.toIonValue(ion: IonSystem): IonValue =
    when (type) {
        ExprValueType.NULL -> {
            val ionType = metas["ion_null_type"] as? IonType ?: IonType.NULL
            ion.newNull(ionType)
        }
        ExprValueType.MISSING -> ion.newNull().apply { addTypeAnnotation(MISSING_ANNOTATION) }
        ExprValueType.BOOL -> ion.newBool(booleanValue())
        ExprValueType.INT -> ion.newInt(longValue())
        ExprValueType.FLOAT -> ion.newFloat(numberValue().toDouble())
        ExprValueType.DECIMAL -> ion.newDecimal(bigDecimalValue())
        ExprValueType.DATE -> {
            val value = dateValue()
            ion.newTimestamp(Timestamp.forDay(value.year, value.monthValue, value.dayOfMonth)).apply {
                addTypeAnnotation(DATE_ANNOTATION)
            }
        }
        ExprValueType.TIMESTAMP -> ion.newTimestamp(timestampValue())
        ExprValueType.TIME -> timeValue().toIonValue(ion)
        ExprValueType.SYMBOL -> ion.newSymbol(stringValue())
        ExprValueType.STRING -> ion.newString(stringValue())
        ExprValueType.CLOB -> ion.newClob(bytesValue())
        ExprValueType.BLOB -> ion.newBlob(bytesValue())
        ExprValueType.LIST -> mapTo(ion.newEmptyList()) { it.toIonValue(ion).clone() }
        ExprValueType.SEXP -> mapTo(ion.newEmptySexp()) { it.toIonValue(ion).clone() }
        ExprValueType.BAG -> mapTo(
            ion.newEmptyList().apply { addTypeAnnotation(BAG_ANNOTATION) }
        ) { it.toIonValue(ion).clone() }
        ExprValueType.STRUCT -> toIonStruct(ion)
    }

/**
 * [SequenceExprValue] may call this function to get a mutable instance of the IonValue that it can add
 * directly to its lazily constructed list.  This is a performance optimization--otherwise the value would be
 * cloned twice: once by this class's implementation of [ionValue], and again before adding the value to its list.
 *
 * Note: it is not possible to add a sealed (non-mutuable) [IonValue] as a child of a container.
 */
private fun ExprValue.toIonStruct(ion: IonSystem): IonStruct {
    return ion.newEmptyStruct().apply {
        this@toIonStruct.forEach {
            val nameVal = it.name
            if (nameVal != null && nameVal.type.isText && it.type != ExprValueType.MISSING) {
                val name = nameVal.stringValue()
                add(name, it.toIonValue(ion).clone())
            }
        }
    }
}

fun IonValue.toExprValue(): ExprValue =
    when {
        isNullValue && hasTypeAnnotation(MISSING_ANNOTATION) -> missingExprValue() // MISSING
        isNullValue -> object : ExprValue by nullExprValue() {
            override val metas =
                mapOf(
                    Pair("ion_null_type", this@toExprValue.type)
                )
        } // NULL
        this is IonBool -> boolExprValue(booleanValue()) // BOOL
        this is IonInt -> intExprValue(longValue()) // INT
        this is IonFloat -> floatExprValue(doubleValue()) // FLOAT
        this is IonDecimal -> decimalExprValue(decimalValue()) // DECIMAL
        this is IonTimestamp && hasTypeAnnotation(DATE_ANNOTATION) -> {
            val timestampValue = timestampValue()
            dateExprValue(timestampValue.year, timestampValue.month, timestampValue.day)
        } // DATE
        this is IonTimestamp -> timestampExprValue(timestampValue()) // TIMESTAMP
        this is IonStruct && hasTypeAnnotation(TIME_ANNOTATION) -> {
            val hourValue = (get("hour") as IonInt).intValue()
            val minuteValue = (get("minute") as IonInt).intValue()
            val secondInDecimal = (get("second") as IonDecimal).decimalValue()
            val secondValue = secondInDecimal.toInt()
            val nanoValue = secondInDecimal.remainder(BigDecimal.ONE).multiply(NANOS_PER_SECOND.toBigDecimal()).toInt()
            val timeZoneHourValue = (get("timezone_hour") as IonInt).intValue()
            val timeZoneMinuteValue = (get("timezone_minute") as IonInt).intValue()
            timeExprValue(Time.of(hourValue, minuteValue, secondValue, nanoValue, secondInDecimal.scale(), timeZoneHourValue * 60 + timeZoneMinuteValue))
        } // TIME
        this is IonSymbol -> symbolExprValue(stringValue()) // SYMBOL
        this is IonString -> stringExprValue(stringValue()) // STRING
        this is IonClob -> clobExprValue(bytesValue()) // CLOB
        this is IonBlob -> blobExprValue(bytesValue()) // BLOB
        this is IonList && hasTypeAnnotation(BAG_ANNOTATION) -> bagExprValue(asSequence().map { it.toExprValue() }) // BAG
        this is IonList -> listExprValue(asSequence().map { it.toExprValue() }) // LIST
        this is IonSexp -> sexpExprValue(asSequence().map { it.toExprValue() }) // SEXP
        this is IonStruct -> object : ExprValue by structExprValue(
            asSequence().map {
                it.toExprValue().namedValue(stringExprValue(it.fieldName))
            },
            StructOrdering.UNORDERED
        ) {
            override val bindings: Bindings<ExprValue> =
                IonStructBindings(this@toExprValue)
        } // STRUCT
        else -> error("Invalid IonValue")
    }

fun getExprValueFromIonRead(ion: IonSystem, reader: IonReader): ExprValue =
    ion.newValue(reader).toExprValue()

/**
 * Custom implementation of [Bindings] that lazily computes case sensitive or insensitive hash tables which
 * will speed up the lookup of bindings within structs.
 *
 * The key difference in behavior between this and other [Bindings] implementations is that it
 * can throw an ambiguous binding [EvaluationException] even for case-sensitive lookups as it is
 * entirely possible that fields with identical names can appear within [IonStruct]s.
 *
 * Important: this class is critical to performance for many queries.  Change with caution.
 */
internal class IonStructBindings(private val myStruct: IonStruct) : Bindings<ExprValue> {

    private val caseInsensitiveFieldMap by lazy {
        HashMap<String, ArrayList<IonValue>>().apply {
            for (field in myStruct) {
                val entries = getOrPut(field.fieldName.toLowerCase()) { ArrayList(1) }
                entries.add(field)
            }
        }
    }

    private val caseSensitiveFieldMap by lazy {
        HashMap<String, ArrayList<IonValue>>().apply {
            for (field in myStruct) {
                val entries = getOrPut(field.fieldName) { ArrayList(1) }
                entries.add(field)
            }
        }
    }

    private fun caseSensitiveLookup(fieldName: String): IonValue? =
        caseSensitiveFieldMap[fieldName]?.let { entries -> handleMatches(entries, fieldName) }

    private fun caseInsensitiveLookup(fieldName: String): IonValue? =
        caseInsensitiveFieldMap[fieldName.toLowerCase()]?.let { entries -> handleMatches(entries, fieldName) }

    private fun handleMatches(entries: List<IonValue>, fieldName: String): IonValue? =
        when (entries.size) {
            0 -> null
            1 -> entries[0]
            else ->
                errAmbiguousBinding(fieldName, entries.map { it.fieldName })
        }

    override operator fun get(bindingName: BindingName): ExprValue? =
        when (bindingName.bindingCase) {
            BindingCase.SENSITIVE -> caseSensitiveLookup(bindingName.name)
            BindingCase.INSENSITIVE -> caseInsensitiveLookup(bindingName.name)
        }?.let {
            it.toExprValue().namedValue(stringExprValue(it.fieldName))
        }
}
