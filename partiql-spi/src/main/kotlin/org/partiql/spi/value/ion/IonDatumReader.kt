package org.partiql.spi.value.ion;

import com.amazon.ion.IonException
import com.amazon.ion.IonReader
import com.amazon.ion.IonType
import com.amazon.ion.Span
import com.amazon.ion.SpanProvider
import com.amazon.ion.system.IonReaderBuilder
import com.amazon.ionelement.api.loadSingleElement
import org.partiql.spi.value.Datum
import org.partiql.spi.value.DatumReader
import java.io.IOException
import java.io.InputStream

/**
 * A [DatumReader] implementation for Ion encoded PartiQL data.
 *
 * TODO this will need to be re-worked for structural/collection types.
 */
public class IonDatumReader private constructor(
    private val reader: IonReader,
    private val others: Map<String, DatumReader>,
) : DatumReader {

    /**
     * Create a reader from the [IonReader].
     */
    public constructor(reader: IonReader) : this(reader, emptyMap())

    /**
     * Create a reader from the [InputStream] using the standard [IonReader].
     */
    public constructor(input: InputStream) : this(IonReaderBuilder.standard().build(input))

    /**
     * Helper for current span on errors.
     */
    private fun span(): Span = reader.asFacet(SpanProvider::class.java).currentSpan()

    /**
     * From AutoCloseable.
     */
    override fun close() {
        reader.close()
    }

    /**
     * Read next Datum or null.
     */
    @Throws(IOException::class, IonDatumException::class)
    override fun read(): Datum? {
        return try {
            val type = reader.next() ?: return null
            val anno = reader.typeAnnotations
            when (anno.size) {
                0 -> read0(type)
                1 -> read1(anno[0])
                else -> throw IonDatumException("expected 0 or 1 annotations", null, span())
            }
        } catch (ex: IonException) {
            throw IonDatumException("data exception", ex, span())
        }
    }

    /**
     * Any read
     */
    private fun read0(type: IonType): Datum = when (type) {
        IonType.NULL -> Datum.nullValue()
        IonType.BOOL -> bool()
        IonType.INT -> bigint()
        IonType.FLOAT -> double()
        IonType.DECIMAL -> decimal()
        IonType.TIMESTAMP -> TODO("timestamp")
        IonType.STRING -> varchar()
        IonType.CLOB -> clob()
        IonType.BLOB -> clob()
        IonType.LIST -> array()
        IonType.SEXP -> TODO()
        IonType.STRUCT -> struct()
        IonType.SYMBOL -> missing()
        IonType.DATAGRAM -> throw IonDatumException("encountered datagram", null, span())
    }

    /**
     * Would be nice to NOT do string comparisons.
     */
    private fun read1(annotation: String): Datum = when (annotation) {
        "bool" -> bool()
        "tinyint" -> tinyint()
        "smallint" -> smallint()
        "int" -> int()
        "bigint" -> bigint()
        "real" -> real()
        "double" -> double()
        "varchar" -> varchar()
        "array" -> array()
        "bag" -> bag()
        "struct" -> struct()
        "ion" -> IonDatum(loadSingleElement(reader))
        else -> TODO("check others")
    }

    private fun missing(): Datum = when (reader.symbolValue().text) {
        "missing" -> Datum.missing()
        else -> throw IonDatumException("expected symbol `missing`", null, span())
    }

    private fun bool(): Datum = Datum.bool(reader.booleanValue())

    private fun tinyint(): Datum {
        val v = reader.longValue()
        if (v < Byte.MIN_VALUE || v > Byte.MAX_VALUE) {
            throw IonDatumException("tinyint out of range", null, span())
        }
        return Datum.tinyint(v.toByte())
    }

    private fun smallint(): Datum {
        val v = reader.longValue()
        if (v < Short.MIN_VALUE || v > Short.MAX_VALUE) {
            throw IonDatumException("smallint out of range", null, span())
        }
        return Datum.smallint(v.toShort())
    }

    /**
     * As far as I can tell, IonReader impls do `(int) longValue()`.
     */
    private fun int(): Datum {
        val v = reader.longValue()
        if (v < Int.MIN_VALUE || v > Int.MAX_VALUE) {
            throw IonDatumException("int out of range", null, span())
        }
        return Datum.integer(v.toInt())
    }

    private fun bigint(): Datum = Datum.bigint(reader.longValue())

    private fun decimal(): Datum = TODO()

    private fun decimal(precision: Int): Datum = TODO()

    private fun decimal(precision: Int, scale: Int): Datum = TODO()

    private fun float(precision: Int): Datum = TODO("float")

    private fun real(): Datum {
        val v = reader.doubleValue()
        if (v > Float.MAX_VALUE || v < Float.MIN_VALUE) {
            throw IonDatumException("real out of range", null, span())
        }
        return Datum.real(v.toFloat())
    }

    private fun double(): Datum = Datum.doublePrecision(reader.doubleValue())

    private fun char(length: Int): Datum = TODO()

    private fun varchar(): Datum = Datum.string(reader.stringValue())

    private fun varchar(length: Int): Datum = TODO()

    private fun clob(): Datum = TODO()

    private fun clob(length: Int): Datum = TODO()

    private fun blob(): Datum = TODO()

    private fun blob(length: Int): Datum = TODO()

    private fun date(): Datum = TODO()

    private fun time(precision: Int): Datum = TODO()

    private fun timez(precision: Int): Datum = TODO()

    private fun timestamp(precision: Int): Datum = TODO()

    private fun timestampz(precision: Int): Datum = TODO()

    private fun array(): Datum = TODO()

    private fun bag(): Datum = TODO()

    private fun struct(): Datum = TODO()

    public companion object {

        @JvmStatic
        public fun build(): Builder = Builder()
    }

    /**
     * IonDatumReader.Builder can be re-used
     */
    public class Builder {

        private val others = mutableMapOf<String, DatumReader>()

        public fun register(encoding: String, reader: DatumReader): Builder {
            others[encoding] = reader
            return this
        }

        public fun build(reader: IonReader): IonDatumReader = IonDatumReader(reader, others)

        public fun build(input: InputStream): IonDatumReader = IonDatumReader(
            reader = IonReaderBuilder.standard().build(input),
            others = others,
        )
    }
}
