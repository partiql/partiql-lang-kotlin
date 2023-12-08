package org.partiql.lang.eval.internal

import com.amazon.ion.IonBlob
import com.amazon.ion.IonBool
import com.amazon.ion.IonClob
import com.amazon.ion.IonDatagram
import com.amazon.ion.IonDecimal
import com.amazon.ion.IonFloat
import com.amazon.ion.IonInt
import com.amazon.ion.IonList
import com.amazon.ion.IonSexp
import com.amazon.ion.IonString
import com.amazon.ion.IonStruct
import com.amazon.ion.IonSymbol
import com.amazon.ion.IonTimestamp
import com.amazon.ion.IonValue
import org.partiql.lang.eval.BAG_ANNOTATION
import org.partiql.lang.eval.Bindings
import org.partiql.lang.eval.DATE_ANNOTATION
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.GRAPH_ANNOTATION
import org.partiql.lang.eval.MISSING_ANNOTATION
import org.partiql.lang.eval.TIME_ANNOTATION
import org.partiql.lang.eval.internal.ext.namedValue
import org.partiql.lang.eval.time.Time
import org.partiql.lang.graph.ExternalGraphReader
import org.partiql.lang.util.bytesValue
import java.math.BigDecimal

/**
 * Creates a new [ExprValue] instance from any Ion value.
 *
 * If possible, prefer the use of the other methods instead because they might return [ExprValue] instances
 * that are better optimized for their specific data type (depending on implementation).
 */
internal fun ionValueToExprValue(value: IonValue): ExprValue {
    return when {
        value.isNullValue && value.hasTypeAnnotation(MISSING_ANNOTATION) -> ExprValue.missingValue // MISSING
        value.isNullValue -> ExprValue.newNull(value.type) // NULL
        value is IonBool -> ExprValue.newBoolean(value.booleanValue()) // BOOL
        value is IonInt -> ExprValue.newInt(value.longValue()) // INT
        value is IonFloat -> ExprValue.newFloat(value.doubleValue()) // FLOAT
        value is IonDecimal -> ExprValue.newDecimal(value.decimalValue()) // DECIMAL
        value is IonTimestamp && value.hasTypeAnnotation(DATE_ANNOTATION) -> { // DATE
            val timestampValue = value.timestampValue()
            ExprValue.newDate(timestampValue.year, timestampValue.month, timestampValue.day)
        }
        value is IonTimestamp -> ExprValue.newTimestamp(value.timestampValue()) // TIMESTAMP
        value is IonStruct && value.hasTypeAnnotation(TIME_ANNOTATION) -> { // TIME
            val hourValue = (value["hour"] as IonInt).intValue()
            val minuteValue = (value["minute"] as IonInt).intValue()
            val secondInDecimal = (value["second"] as IonDecimal).decimalValue()
            val secondValue = secondInDecimal.toInt()
            val nanoValue = secondInDecimal.remainder(BigDecimal.ONE).multiply(NANOS_PER_SECOND.toBigDecimal()).toInt()
            val timeZoneHourValue = (value["timezone_hour"] as IonInt).intValue()
            val timeZoneMinuteValue = (value["timezone_minute"] as IonInt).intValue()
            ExprValue.newTime(
                Time.of(
                    hourValue,
                    minuteValue,
                    secondValue,
                    nanoValue,
                    secondInDecimal.scale(),
                    timeZoneHourValue * 60 + timeZoneMinuteValue
                )
            )
        }
        value is IonStruct && value.hasTypeAnnotation(GRAPH_ANNOTATION) -> // GRAPH
            ExprValue.newGraph(ExternalGraphReader.read(value))
        value is IonSymbol -> ExprValue.newSymbol(value.stringValue()) // SYMBOL
        value is IonString -> ExprValue.newString(value.stringValue()) // STRING
        value is IonClob -> ExprValue.newClob(value.bytesValue()) // CLOB
        value is IonBlob -> ExprValue.newBlob(value.bytesValue()) // BLOB
        value is IonList && value.hasTypeAnnotation(BAG_ANNOTATION) -> BagExprValue(value.map { ionValueToExprValue(it) }) // BAG
        value is IonList -> ListExprValue(value.map { ionValueToExprValue(it) }) // LIST
        value is IonSexp -> SexpExprValue(value.map { ionValueToExprValue(it) }) // SEXP
        value is IonStruct -> IonStructExprValue(value) // STRUCT
        value is IonDatagram -> BagExprValue(value.map { ionValueToExprValue(it) }) // DATAGRAM represented as BAG ExprValue
        else -> error("Unrecognized IonValue to transform to ExprValue: $value")
    }
}

private class IonStructExprValue(
    ionStruct: IonStruct
) : StructExprValue(
    StructOrdering.UNORDERED,
    ionStruct.asSequence().map { ionValueToExprValue(it).namedValue(ExprValue.newString(it.fieldName)) }
) {
    override val bindings: Bindings<ExprValue> =
        IonStructBindings(ionStruct)
}
