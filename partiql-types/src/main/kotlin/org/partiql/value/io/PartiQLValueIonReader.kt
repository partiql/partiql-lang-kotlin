package org.partiql.value.io

import com.amazon.ion.IonReader
import com.amazon.ion.IonType
import com.amazon.ion.system.IonReaderBuilder
import org.partiql.value.DecimalValue
import org.partiql.value.IntValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType
import org.partiql.value.bagValue
import org.partiql.value.blobValue
import org.partiql.value.boolValue
import org.partiql.value.clobValue
import org.partiql.value.dateValue
import org.partiql.value.datetime.DateTimeValue
import org.partiql.value.datetime.TimeZone
import org.partiql.value.decimalValue
import org.partiql.value.float64Value
import org.partiql.value.intValue
import org.partiql.value.listValue
import org.partiql.value.missingValue
import org.partiql.value.nullValue
import org.partiql.value.sexpValue
import org.partiql.value.stringValue
import org.partiql.value.structValue
import org.partiql.value.symbolValue
import org.partiql.value.timeValue
import org.partiql.value.timestampValue
import java.io.IOException
import java.io.InputStream
import java.math.BigDecimal

/**
 * Make an arbitrary call on partiql value read
 * PartiQL generated annotation must be closed to the value
 *
 * For example:
 * In `IonForPartiQL` mode: if the input is $time::$day::$time{...}
 * The reader will attempt to create a time value
 *
 */
@OptIn(PartiQLValueExperimental::class)
internal class PartiQLValueIonReader(
    private val ionReader: IonReader,
    private val sourceDataFormat: PartiQLValueIonReaderBuilder.SourceDataFormat
) : PartiQLValueReader {
    private enum class PARTIQL_ANNOTATION(val annotation: String) {
        MISSING_ANNOTATION("\$missing"),
        BAG_ANNOTATION("\$bag"),
        DATE_ANNOTATION("\$date"),
        TIME_ANNOTATION("\$time"),
        TIMESTAMP_ANNOTATION("\$timestamp"),
        GRAPH_ANNOTATION("\$graph")
    }

    @Throws(IOException::class)
    override fun read(): PartiQLValue {
        val value = if (ionReader.next() != null) {
            fromIon(ionReader)
        } else {
            throw IOException("End of File.")
        }
        return value
    }

    override fun close() {
        ionReader.close()
    }

    private fun fromIon(reader: IonReader): PartiQLValue =
        when (sourceDataFormat) {
            PartiQLValueIonReaderBuilder.SourceDataFormat.IonGeneric -> fromIonGeneric(reader)
            PartiQLValueIonReaderBuilder.SourceDataFormat.IonForPartiQL -> fromIonForPartiQL(reader)
        }

    private fun fromIonGeneric(reader: IonReader): PartiQLValue =
        when (reader.type) {
            IonType.NULL -> {
                nullValue(reader.typeAnnotations.toList())
            }

            IonType.BOOL -> {
                if (reader.isNullValue) {
                    boolValue(null, reader.typeAnnotations.toList())
                } else {
                    boolValue(reader.booleanValue(), reader.typeAnnotations.toList())
                }
            }

            IonType.INT -> {
                if (reader.isNullValue) {
                    intValue(null, reader.typeAnnotations.toList())
                } else {
                    intValue(reader.bigIntegerValue(), reader.typeAnnotations.toList())
                }
            }

            IonType.FLOAT -> {
                if (reader.isNullValue) {
                    float64Value(null, reader.typeAnnotations.toList())
                } else {
                    float64Value(reader.doubleValue(), reader.typeAnnotations.toList())
                }
            }

            IonType.DECIMAL -> {
                if (reader.isNullValue) {
                    decimalValue(null, reader.typeAnnotations.toList())
                } else {
                    decimalValue(reader.bigDecimalValue(), reader.typeAnnotations.toList())
                }
            }

            IonType.TIMESTAMP -> {
                if (reader.isNullValue) {
                    timestampValue(null, reader.typeAnnotations.toList())
                } else {
                    timestampValue(DateTimeValue.timestamp(reader.timestampValue()), reader.typeAnnotations.toList())
                }
            }

            IonType.SYMBOL -> {
                if (reader.isNullValue) {
                    symbolValue(null, reader.typeAnnotations.toList())
                } else {
                    symbolValue(reader.stringValue(), reader.typeAnnotations.toList())
                }
            }

            IonType.STRING -> {
                if (reader.isNullValue) {
                    stringValue(null, reader.typeAnnotations.toList())
                } else {
                    stringValue(reader.stringValue(), reader.typeAnnotations.toList())
                }
            }

            IonType.CLOB -> {
                if (reader.isNullValue) {
                    clobValue(null, reader.typeAnnotations.toList())
                } else {
                    clobValue(reader.newBytes(), reader.typeAnnotations.toList())
                }
            }

            IonType.BLOB -> {
                if (reader.isNullValue) {
                    blobValue(null, reader.typeAnnotations.toList())
                } else {
                    blobValue(reader.newBytes(), reader.typeAnnotations.toList())
                }
            }

            IonType.LIST -> {
                val annotations = reader.typeAnnotations.toList()
                reader.stepIn()
                val elements = mutableListOf<PartiQLValue>().also { elements ->
                    reader.loadEachValue {
                        elements.add(fromIon(reader))
                    }
                }
                reader.stepOut()
                listValue(elements.asSequence(), annotations)
            }

            IonType.SEXP -> {
                val annotation = reader.typeAnnotations.toList()
                reader.stepIn()
                val elements = mutableListOf<PartiQLValue>().also { elements ->
                    reader.loadEachValue {
                        elements.add(fromIon(reader))
                    }
                }
                reader.stepOut()
                sexpValue(elements.asSequence(), annotation)
            }

            IonType.STRUCT -> {
                val annotations = reader.typeAnnotations.toList()
                reader.stepIn()
                val elements = mutableListOf<Pair<String, PartiQLValue>>().also { elements ->
                    reader.loadEachValue {
                        val element = reader.fieldName to fromIon(reader)
                        elements.add(element)
                    }
                }
                reader.stepOut()
                structValue(elements.asSequence(), annotations)
            }

            IonType.DATAGRAM -> throw IllegalArgumentException("Datagram not supported")
        }

    // We have to check if an inappropriate annotation exists
    // for example: $missing::1
    // this makes the code not very easy to maintain as we potentially can add more reserved annotation
    // I made the deliberate decision to list out all branches so we don't accidentally forget one in the future...
    private fun fromIonForPartiQL(reader: IonReader): PartiQLValue {
        val annotations = reader.typeAnnotations.toList()
        val lastAnnotation = getPartiQLReservedAnnotation(annotations)
        return when (reader.type) {
            IonType.NULL -> {
                when (lastAnnotation) {
                    PARTIQL_ANNOTATION.MISSING_ANNOTATION -> missingValue(annotations.dropLast(1))
                    PARTIQL_ANNOTATION.BAG_ANNOTATION -> bagValue<PartiQLValue>(null, annotations.dropLast(1))
                    PARTIQL_ANNOTATION.DATE_ANNOTATION -> dateValue(null, annotations.dropLast(1))
                    PARTIQL_ANNOTATION.TIME_ANNOTATION -> timeValue(null, annotations.dropLast(1))
                    PARTIQL_ANNOTATION.TIMESTAMP_ANNOTATION -> timestampValue(null, annotations.dropLast(1))
                    PARTIQL_ANNOTATION.GRAPH_ANNOTATION -> TODO("Graph not yet implemented")
                    null -> nullValue(annotations)
                }
            }

            IonType.BOOL -> {
                when (lastAnnotation) {
                    PARTIQL_ANNOTATION.MISSING_ANNOTATION -> throw IllegalArgumentException("MISSING_ANNOTATION with Bool Value")
                    PARTIQL_ANNOTATION.BAG_ANNOTATION -> throw IllegalArgumentException("BAG_ANNOTATION with Bool Value")
                    PARTIQL_ANNOTATION.DATE_ANNOTATION -> throw IllegalArgumentException("DATE_ANNOTATION with Bool Value")
                    PARTIQL_ANNOTATION.TIME_ANNOTATION -> throw IllegalArgumentException("TIME_ANNOTATION with Bool Value")
                    PARTIQL_ANNOTATION.TIMESTAMP_ANNOTATION -> throw IllegalArgumentException("TIMESTAMP_ANNOTATION with Bool Value")
                    PARTIQL_ANNOTATION.GRAPH_ANNOTATION -> throw IllegalArgumentException("GRAPH_ANNOTATION with Bool Value")
                    null -> {
                        if (reader.isNullValue) {
                            boolValue(null, annotations)
                        } else {
                            boolValue(reader.booleanValue(), annotations)
                        }
                    }
                }
            }

            IonType.INT -> {
                when (lastAnnotation) {
                    PARTIQL_ANNOTATION.MISSING_ANNOTATION -> throw IllegalArgumentException("MISSING_ANNOTATION with Int Value")
                    PARTIQL_ANNOTATION.BAG_ANNOTATION -> throw IllegalArgumentException("BAG_ANNOTATION with Int Value")
                    PARTIQL_ANNOTATION.DATE_ANNOTATION -> throw IllegalArgumentException("DATE_ANNOTATION with Int Value")
                    PARTIQL_ANNOTATION.TIME_ANNOTATION -> throw IllegalArgumentException("TIME_ANNOTATION with Int Value")
                    PARTIQL_ANNOTATION.TIMESTAMP_ANNOTATION -> throw IllegalArgumentException("TIMESTAMP_ANNOTATION with Int Value")
                    PARTIQL_ANNOTATION.GRAPH_ANNOTATION -> throw IllegalArgumentException("GRAPH_ANNOTATION with Int Value")
                    null -> {
                        if (reader.isNullValue) {
                            intValue(null, annotations)
                        } else {
                            intValue(reader.bigIntegerValue(), annotations)
                        }
                    }
                }
            }

            IonType.FLOAT -> {
                when (lastAnnotation) {
                    PARTIQL_ANNOTATION.MISSING_ANNOTATION -> throw IllegalArgumentException("MISSING_ANNOTATION with Float Value")
                    PARTIQL_ANNOTATION.BAG_ANNOTATION -> throw IllegalArgumentException("BAG_ANNOTATION with Float Value")
                    PARTIQL_ANNOTATION.DATE_ANNOTATION -> throw IllegalArgumentException("DATE_ANNOTATION with Float Value")
                    PARTIQL_ANNOTATION.TIME_ANNOTATION -> throw IllegalArgumentException("TIME_ANNOTATION with Float Value")
                    PARTIQL_ANNOTATION.TIMESTAMP_ANNOTATION -> throw IllegalArgumentException("TIMESTAMP_ANNOTATION with Float Value")
                    PARTIQL_ANNOTATION.GRAPH_ANNOTATION -> throw IllegalArgumentException("GRAPH_ANNOTATION with Float Value")
                    null -> {
                        if (reader.isNullValue) {
                            float64Value(null, annotations)
                        } else {
                            float64Value(reader.doubleValue(), annotations)
                        }
                    }
                }
            }

            IonType.DECIMAL -> {
                when (lastAnnotation) {
                    PARTIQL_ANNOTATION.MISSING_ANNOTATION -> throw IllegalArgumentException("MISSING_ANNOTATION with Decimal Value")
                    PARTIQL_ANNOTATION.BAG_ANNOTATION -> throw IllegalArgumentException("BAG_ANNOTATION with Decimal Value")
                    PARTIQL_ANNOTATION.DATE_ANNOTATION -> throw IllegalArgumentException("DATE_ANNOTATION with Decimal Value")
                    PARTIQL_ANNOTATION.TIME_ANNOTATION -> throw IllegalArgumentException("TIME_ANNOTATION with Decimal Value")
                    PARTIQL_ANNOTATION.TIMESTAMP_ANNOTATION -> throw IllegalArgumentException("TIMESTAMP_ANNOTATION with Decimal Value")
                    PARTIQL_ANNOTATION.GRAPH_ANNOTATION -> throw IllegalArgumentException("GRAPH_ANNOTATION with Decimal Value")
                    null -> {
                        if (reader.isNullValue) {
                            decimalValue(null, annotations)
                        } else {
                            decimalValue(reader.bigDecimalValue(), annotations)
                        }
                    }
                }
            }

            IonType.TIMESTAMP -> {
                when (lastAnnotation) {
                    PARTIQL_ANNOTATION.MISSING_ANNOTATION -> throw IllegalArgumentException("MISSING_ANNOTATION with Timestamp Value")
                    PARTIQL_ANNOTATION.BAG_ANNOTATION -> throw IllegalArgumentException("BAG_ANNOTATION with Timestamp Value")
                    PARTIQL_ANNOTATION.DATE_ANNOTATION -> throw IllegalArgumentException("DATE_ANNOTATION with Timestamp Value")
                    PARTIQL_ANNOTATION.TIME_ANNOTATION -> throw IllegalArgumentException("TIME_ANNOTATION with Timestamp Value")
                    PARTIQL_ANNOTATION.TIMESTAMP_ANNOTATION -> throw IllegalArgumentException("TIMESTAMP_ANNOTATION with Timestamp Value, we except Timestamp annotation to be used with Ion Struct")
                    PARTIQL_ANNOTATION.GRAPH_ANNOTATION -> throw IllegalArgumentException("GRAPH_ANNOTATION with Timestamp Value")
                    null -> {
                        if (reader.isNullValue) {
                            timestampValue(null, annotations)
                        } else {
                            timestampValue(DateTimeValue.timestamp(reader.timestampValue()), annotations)
                        }
                    }
                }
            }

            IonType.SYMBOL -> {
                when (lastAnnotation) {
                    PARTIQL_ANNOTATION.MISSING_ANNOTATION -> throw IllegalArgumentException("MISSING_ANNOTATION with Symbol Value")
                    PARTIQL_ANNOTATION.BAG_ANNOTATION -> throw IllegalArgumentException("BAG_ANNOTATION with Symbol Value")
                    PARTIQL_ANNOTATION.DATE_ANNOTATION -> throw IllegalArgumentException("DATE_ANNOTATION with Symbol Value")
                    PARTIQL_ANNOTATION.TIME_ANNOTATION -> throw IllegalArgumentException("TIME_ANNOTATION with Symbol Value")
                    PARTIQL_ANNOTATION.TIMESTAMP_ANNOTATION -> throw IllegalArgumentException("TIMESTAMP_ANNOTATION with Symbol Value")
                    PARTIQL_ANNOTATION.GRAPH_ANNOTATION -> throw IllegalArgumentException("GRAPH_ANNOTATION with Symbol Value")
                    null -> {
                        if (reader.isNullValue) {
                            symbolValue(null, annotations)
                        } else {
                            symbolValue(reader.stringValue(), annotations)
                        }
                    }
                }
            }

            IonType.STRING -> {
                when (lastAnnotation) {
                    PARTIQL_ANNOTATION.MISSING_ANNOTATION -> throw IllegalArgumentException("MISSING_ANNOTATION with String Value")
                    PARTIQL_ANNOTATION.BAG_ANNOTATION -> throw IllegalArgumentException("BAG_ANNOTATION with String Value")
                    PARTIQL_ANNOTATION.DATE_ANNOTATION -> throw IllegalArgumentException("DATE_ANNOTATION with String Value")
                    PARTIQL_ANNOTATION.TIME_ANNOTATION -> throw IllegalArgumentException("TIME_ANNOTATION with String Value")
                    PARTIQL_ANNOTATION.TIMESTAMP_ANNOTATION -> throw IllegalArgumentException("TIMESTAMP_ANNOTATION with String Value")
                    PARTIQL_ANNOTATION.GRAPH_ANNOTATION -> throw IllegalArgumentException("GRAPH_ANNOTATION with String Value")
                    null -> {
                        if (reader.isNullValue) {
                            stringValue(null, annotations)
                        } else {
                            stringValue(reader.stringValue(), annotations)
                        }
                    }
                }
            }

            IonType.CLOB -> {
                when (lastAnnotation) {
                    PARTIQL_ANNOTATION.MISSING_ANNOTATION -> throw IllegalArgumentException("MISSING_ANNOTATION with Clob Value")
                    PARTIQL_ANNOTATION.BAG_ANNOTATION -> throw IllegalArgumentException("BAG_ANNOTATION with Clob Value")
                    PARTIQL_ANNOTATION.DATE_ANNOTATION -> throw IllegalArgumentException("DATE_ANNOTATION with Clob Value")
                    PARTIQL_ANNOTATION.TIME_ANNOTATION -> throw IllegalArgumentException("TIME_ANNOTATION with Clob Value")
                    PARTIQL_ANNOTATION.TIMESTAMP_ANNOTATION -> throw IllegalArgumentException("TIMESTAMP_ANNOTATION with Clob Value")
                    PARTIQL_ANNOTATION.GRAPH_ANNOTATION -> throw IllegalArgumentException("GRAPH_ANNOTATION with Clob Value")
                    null -> {
                        if (reader.isNullValue) {
                            clobValue(null, annotations)
                        } else {
                            clobValue(reader.newBytes(), annotations)
                        }
                    }
                }
            }

            IonType.BLOB -> {
                when (lastAnnotation) {
                    PARTIQL_ANNOTATION.MISSING_ANNOTATION -> throw IllegalArgumentException("MISSING_ANNOTATION with Blob Value")
                    PARTIQL_ANNOTATION.BAG_ANNOTATION -> throw IllegalArgumentException("BAG_ANNOTATION with Blob Value")
                    PARTIQL_ANNOTATION.DATE_ANNOTATION -> throw IllegalArgumentException("DATE_ANNOTATION with Blob Value")
                    PARTIQL_ANNOTATION.TIME_ANNOTATION -> throw IllegalArgumentException("TIME_ANNOTATION with Blob Value")
                    PARTIQL_ANNOTATION.TIMESTAMP_ANNOTATION -> throw IllegalArgumentException("TIMESTAMP_ANNOTATION with Blob Value")
                    PARTIQL_ANNOTATION.GRAPH_ANNOTATION -> throw IllegalArgumentException("GRAPH_ANNOTATION with Blob Value")
                    null -> {
                        if (reader.isNullValue) {
                            blobValue(null, annotations)
                        } else {
                            blobValue(reader.newBytes(), annotations)
                        }
                    }
                }
            }

            IonType.LIST -> {
                when (lastAnnotation) {
                    PARTIQL_ANNOTATION.MISSING_ANNOTATION -> throw IllegalArgumentException("MISSING_ANNOTATION with List Value")
                    PARTIQL_ANNOTATION.BAG_ANNOTATION -> {
                        if (reader.isNullValue) {
                            bagValue<PartiQLValue>(null, annotations.dropLast(1))
                        } else {
                            reader.stepIn()
                            val elements = mutableListOf<PartiQLValue>().also { elements ->
                                reader.loadEachValue {
                                    elements.add(fromIon(reader))
                                }
                            }
                            reader.stepOut()
                            bagValue(elements.asSequence(), annotations.dropLast(1))
                        }
                    }
                    PARTIQL_ANNOTATION.DATE_ANNOTATION -> throw IllegalArgumentException("DATE_ANNOTATION with List Value")
                    PARTIQL_ANNOTATION.TIME_ANNOTATION -> throw IllegalArgumentException("TIME_ANNOTATION with List Value")
                    PARTIQL_ANNOTATION.TIMESTAMP_ANNOTATION -> throw IllegalArgumentException("TIMESTAMP_ANNOTATION with List Value")
                    PARTIQL_ANNOTATION.GRAPH_ANNOTATION -> TODO("Not yet implemented")
                    null -> {
                        if (reader.isNullValue) {
                            listValue<PartiQLValue>(null, annotations)
                        } else {
                            reader.stepIn()
                            val elements = mutableListOf<PartiQLValue>().also { elements ->
                                reader.loadEachValue {
                                    elements.add(fromIon(reader))
                                }
                            }
                            reader.stepOut()
                            listValue(elements.asSequence(), annotations)
                        }
                    }
                }
            }

            IonType.SEXP -> {
                when (lastAnnotation) {
                    PARTIQL_ANNOTATION.MISSING_ANNOTATION -> throw IllegalArgumentException("MISSING_ANNOTATION with Sexp Value")
                    PARTIQL_ANNOTATION.BAG_ANNOTATION -> throw IllegalArgumentException("BAG_ANNOTATION with Sexp Value")
                    PARTIQL_ANNOTATION.DATE_ANNOTATION -> throw IllegalArgumentException("DATE_ANNOTATION with Sexp Value")
                    PARTIQL_ANNOTATION.TIME_ANNOTATION -> throw IllegalArgumentException("TIME_ANNOTATION with Sexp Value")
                    PARTIQL_ANNOTATION.TIMESTAMP_ANNOTATION -> throw IllegalArgumentException("TIMESTAMP_ANNOTATION with Sexp Value")
                    PARTIQL_ANNOTATION.GRAPH_ANNOTATION -> TODO("Not yet implemented")
                    null -> {
                        if (reader.isNullValue) {
                            sexpValue<PartiQLValue>(null, annotations)
                        } else {
                            reader.stepIn()
                            val elements = mutableListOf<PartiQLValue>().also { elements ->
                                reader.loadEachValue {
                                    elements.add(fromIon(reader))
                                }
                            }
                            reader.stepOut()
                            sexpValue(elements.asSequence(), annotations)
                        }
                    }
                }
            }

            IonType.STRUCT -> {
                when (lastAnnotation) {
                    PARTIQL_ANNOTATION.MISSING_ANNOTATION -> throw IllegalArgumentException("MISSING_ANNOTATION with Struct Value")
                    PARTIQL_ANNOTATION.BAG_ANNOTATION -> throw IllegalArgumentException("BAG_ANNOTATION with Struct Value")
                    PARTIQL_ANNOTATION.DATE_ANNOTATION -> {
                        if (reader.isNullValue) {
                            dateValue(null, annotations.dropLast(1))
                        } else {
                            reader.stepIn()
                            val map = mutableMapOf<String, Any?>()
                            checkRequiredFieldNameAndPut(reader, map, "year", PartiQLValueType.INT)
                            checkRequiredFieldNameAndPut(reader, map, "month", PartiQLValueType.INT)
                            checkRequiredFieldNameAndPut(reader, map, "day", PartiQLValueType.INT)
                            if (reader.next() != null) {
                                throw IllegalArgumentException("excess field in struct")
                            }
                            reader.stepOut()
                            dateValue(DateTimeValue.date(map["year"] as Int, map["month"] as Int, map["day"] as Int), annotations.dropLast(1))
                        }
                    }
                    PARTIQL_ANNOTATION.TIME_ANNOTATION -> {
                        if (reader.isNullValue) {
                            timeValue(null, annotations.dropLast(1))
                        } else {
                            reader.stepIn()
                            val map = mutableMapOf<String, Any?>()
                            checkRequiredFieldNameAndPut(reader, map, "hour", PartiQLValueType.INT)
                            checkRequiredFieldNameAndPut(reader, map, "minute", PartiQLValueType.INT)
                            checkRequiredFieldNameAndPut(reader, map, "second", PartiQLValueType.DECIMAL_ARBITRARY)
                            checkOptionalFieldNameAndPut(reader, map, "offset", PartiQLValueType.INT)
                            // check remaining
                            if (reader.next() != null) {
                                throw IllegalArgumentException("excess field in struct")
                            }
                            reader.stepOut()
                            val offset = if (!map.containsKey("offset")) {
                                null
                            } else if (map["offset"] == null) {
                                TimeZone.UnknownTimeZone
                            } else {
                                TimeZone.UtcOffset.of(map["offset"] as Int)
                            }
                            timeValue(
                                DateTimeValue.time(
                                    map["hour"] as Int, map["minute"] as Int, map["second"] as BigDecimal,
                                    offset
                                ),
                                annotations.dropLast(1)
                            )
                        }
                    }
                    PARTIQL_ANNOTATION.TIMESTAMP_ANNOTATION -> {
                        if (reader.isNullValue) {
                            timestampValue(null, annotations.dropLast(1))
                        } else {
                            reader.stepIn()
                            val map = mutableMapOf<String, Any?>()
                            // check fields
                            checkRequiredFieldNameAndPut(reader, map, "year", PartiQLValueType.INT)
                            checkRequiredFieldNameAndPut(reader, map, "month", PartiQLValueType.INT)
                            checkRequiredFieldNameAndPut(reader, map, "day", PartiQLValueType.INT)
                            checkRequiredFieldNameAndPut(reader, map, "hour", PartiQLValueType.INT)
                            checkRequiredFieldNameAndPut(reader, map, "minute", PartiQLValueType.INT)
                            checkRequiredFieldNameAndPut(reader, map, "second", PartiQLValueType.DECIMAL_ARBITRARY)
                            // check remaining
                            if (reader.next() != null) {
                                throw IllegalArgumentException("excess field in struct")
                            }
                            reader.stepOut()
                            timestampValue(
                                DateTimeValue.timestamp(
                                    map["year"] as Int, map["month"] as Int, map["day"] as Int,
                                    map["hour"] as Int, map["minute"] as Int, map["second"] as BigDecimal,
                                    null
                                ),
                                annotations.dropLast(1)
                            )
                        }
                    }
                    PARTIQL_ANNOTATION.GRAPH_ANNOTATION -> TODO("Not yet implemented")
                    null -> {
                        if (reader.isNullValue) {
                            val nullSequence: Sequence<Pair<String, PartiQLValue>>? = null
                            structValue<PartiQLValue>(nullSequence, annotations)
                        } else {
                            reader.stepIn()
                            val elements = mutableListOf<Pair<String, PartiQLValue>>().also { elements ->
                                reader.loadEachValue {
                                    val element = reader.fieldName to fromIon(reader)
                                    elements.add(element)
                                }
                            }
                            reader.stepOut()
                            structValue(elements.asSequence(), annotations)
                        }
                    }
                }
            }

            IonType.DATAGRAM -> throw IllegalArgumentException("Datagram not supported")
        }
    }

    private fun <T> IonReader.loadEachValue(block: () -> T) {
        while (this.next() != null) {
            block()
        }
    }

    private fun getPartiQLReservedAnnotation(partiqlAnnotation: List<String>) =
        partiqlAnnotation.lastOrNull()?.let { lastAnnotation ->
            PARTIQL_ANNOTATION.values().find { it.annotation == lastAnnotation }
        }

    private fun checkRequiredFieldNameAndPut(reader: IonReader, destination: MutableMap<String, Any?>, expectedField: String, expectedType: PartiQLValueType) {
        if (reader.next() == null) {
            throw IllegalArgumentException("missing $expectedField field")
        }
        if (reader.fieldName == expectedField) {
            checkAndPut(reader, destination, expectedField, expectedType)
        }
    }

    private fun checkOptionalFieldNameAndPut(reader: IonReader, destination: MutableMap<String, Any?>, expectedField: String, expectedType: PartiQLValueType) {
        if (reader.next() != null) {
            checkAndPut(reader, destination, expectedField, expectedType)
        }
    }

    private fun checkAndPut(reader: IonReader, destination: MutableMap<String, Any?>, expectedField: String, expectedType: PartiQLValueType) {
        if (reader.fieldName == expectedField) {
            val k = reader.fieldName
            val v = fromIon(reader)
            when (expectedType) {
                PartiQLValueType.INT -> destination[k] = (v as IntValue).value?.intValueExact()
                PartiQLValueType.DECIMAL_ARBITRARY -> destination[k] = (v as DecimalValue).value
                else -> throw IllegalArgumentException("$expectedField should be either INT OR DECIMAL")
            }
        } else {
            throw IllegalArgumentException("expect $expectedField, but received ${reader.fieldName}")
        }
    }
}

@OptIn(PartiQLValueExperimental::class)
public class PartiQLValueIonReaderBuilder private constructor(
    private var sourceDataFormat: SourceDataFormat = SourceDataFormat.IonForPartiQL,
    private var ionReaderBuilder: IonReaderBuilder,
) {
    public enum class SourceDataFormat {
        /**
         * The ion value annotations are always treated as annotations on PartiQL value.
         * For example, $missing::null will be treated as nullValue(annotations = ["missing"])
         */
        IonGeneric,

        /**
         * We examine the **last** annotation before convert to PartiQL Value.
         * If the annotation is PartiQL reserved, we validate Semantics and the constructed PartiQL value may be different.
         * For example:
         *   - $missing::null will be treated as missingValue(annotations = [])
         *   - a::b::$missing:null will be treated as missingValue(annotations = ["a", "b"])
         *   - a::$missing::b::null will be treated as nullValue(annotation = ["a", "$missing", "b"]
         *   - $missing::1 will cause an exception.
         */
        IonForPartiQL
    }

    public companion object {
        @JvmStatic
        public fun standard(): PartiQLValueIonReaderBuilder = PartiQLValueIonReaderBuilder(
            sourceDataFormat = SourceDataFormat.IonForPartiQL,
            ionReaderBuilder = IonReaderBuilder.standard()
        )
    }

    public fun build(inputStream: InputStream): PartiQLValueReader =
        PartiQLValueIonReader(
            ionReader = ionReaderBuilder.build(inputStream),
            sourceDataFormat = sourceDataFormat
        )

    public fun sourceDataFormat(sourceDataFormat: SourceDataFormat): PartiQLValueIonReaderBuilder = this.apply {
        this.sourceDataFormat = sourceDataFormat
    }

    public fun ionReaderBuilder(ionReaderBuilder: IonReaderBuilder): PartiQLValueIonReaderBuilder = this.apply {
        this.ionReaderBuilder = ionReaderBuilder
    }
}
