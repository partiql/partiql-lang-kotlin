package org.partiql.spi.value.ion

import com.amazon.ion.IonException
import com.amazon.ion.IonType
import com.amazon.ion.Span
import com.amazon.ion.SpanProvider
import com.amazon.ion.system.IonReaderBuilder
import com.amazon.ionelement.api.loadSingleElement
import org.partiql.spi.value.Datum
import org.partiql.spi.value.DatumReader
import org.partiql.spi.value.Encoding
import org.partiql.spi.value.Field
import org.partiql.value.datetime.DateTimeUtil.toBigDecimal
import java.io.IOException
import java.io.InputStream

/**
 * A [DatumReader] implementation for Ion encoded PartiQL data.
 *
 * It works by either parsing a value directly, or looking up the parse method.
 *  - Closures are used for parsing of typed collections, structs, rows, and eventually maps.
 *  - Overloads are named with their argument count to avoid ambiguity.
 */
internal class IonDatumReader internal constructor(
    private val input: InputStream,
    private val others: Map<Encoding, DatumReader>,
) : DatumReader {

    /**
     * Cursor to the Ion stream.
     */
    private val reader = IonReaderBuilder.standard().build(input)

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
    override fun next(): Datum? {
        return try {
            reader.next() ?: return null
            val anno = reader.typeAnnotations
            when (anno.size) {
                0 -> read()
                1 -> method(anno[0]).invoke()
                else -> throw IonDatumException("expected 0 or 1 annotations", null, span())
            }
        } catch (ex: IonException) {
            throw IonDatumException("data exception", ex, span())
        } catch (ex: NotImplementedError) {
            throw IonDatumException("unsupported type", ex, span())
        }
    }

    /**
     * Read without any explicit PartiQL type information.
     */
    private fun read(): Datum = when (reader.type) {
        IonType.NULL -> Datum.nullValue()
        IonType.BOOL -> bool()
        IonType.INT -> bigint()
        IonType.FLOAT -> double()
        IonType.DECIMAL -> decimal0()
        IonType.TIMESTAMP -> TODO("timestamp without annotation")
        IonType.STRING -> varchar0()
        IonType.CLOB -> clob0()
        IonType.BLOB -> clob0()
        IonType.LIST -> array()
        IonType.STRUCT -> struct()
        IonType.SYMBOL -> missing()
        IonType.SEXP -> {
            reader.stepIn()
            if (reader.next() == null) {
                throw IonDatumException("expected type, was null", null, span())
            }
            val method = method()
            if (reader.next() == null) {
                throw IonDatumException("expected value, was null", null, span())
            }
            val value = method()
            if (reader.next() != null) {
                throw IonDatumException("expected end of s-expression pair", null, span())
            }
            reader.stepOut()
            value
        }
        else -> throw IonDatumException("unknown type", null, span())
    }

    /**
     * Return the reader method from just a symbol string; used for both T::value and (T value) syntax.
     */
    private fun method(symbol: String): () -> Datum = when (symbol) {
        "bool" -> ::bool
        "tinyint" -> ::tinyint
        "smallint" -> ::smallint
        "int" -> ::int
        "bigint" -> ::bigint
        "real" -> ::real
        "double" -> ::double
        "char" -> ::char0
        "varchar" -> ::varchar0
        "clob" -> ::clob0
        "blob" -> ::blob0
        "array" -> ::array
        "bag" -> ::bag
        "struct" -> ::struct
        "ion" -> ::ion
        else -> throw IonDatumException("cannot read type $symbol without arguments", null, span())
    }

    /**
     * Return a  reader method from the type.
     */
    private fun method(): () -> Datum = when (reader.type) {
        IonType.SYMBOL -> {
            val symbol = reader.symbolValue().text
            method(symbol)
        }
        IonType.SEXP -> {

            throw IonDatumException("types with parameters are not supported", null, span())
        }
        else -> throw IonDatumException("expected symbol or sexp", null, span())
    }

    //
    // METHODS TO READ THE VARIOUS TYPED VALUES
    //

    private fun missing(): Datum {
        val v = reader.symbolValue().text
        if (v != "missing") {
            throw IonDatumException("expected symbol `missing`, found $v", null, span())
        }
        return Datum.missing()
    }

    private fun bool(): Datum {
        val v = reader.booleanValue()
        return Datum.bool(v)
    }

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

    private fun int(): Datum {
        val v = reader.longValue()
        if (v < Int.MIN_VALUE || v > Int.MAX_VALUE) {
            throw IonDatumException("int out of range", null, span())
        }
        return Datum.integer(v.toInt())
    }

    private fun bigint(): Datum {
        val v = reader.longValue()
        return Datum.bigint(v)
    }

    private fun decimal0(): Datum {
        val v = reader.decimalValue().toBigDecimal()
        val p = v.precision()
        val s = v.scale()
        return Datum.decimal(v, p, s)
    }

    private fun decimal1(precision: Int): Datum {
        val v = reader.decimalValue().toBigDecimal()
        val p = v.precision()
        val s = v.scale()
        if (p != precision) {
            throw IonDatumException("decimal($precision) had precision $p", null, span())
        }
        return Datum.decimal(v, p, s)
    }

    private fun decimal2(precision: Int, scale: Int): Datum {
        val v = reader.decimalValue().toBigDecimal()
        val p = v.precision()
        val s = v.scale()
        if (p != precision) {
            throw IonDatumException("decimal($precision, $scale) had precision $p", null, span())
        }
        if (s != scale) {
            throw IonDatumException("decimal($precision, $scale) had scale $s", null, span())
        }
        return Datum.decimal(v, p, s)
    }

    private fun float(precision: Int): Datum {
        throw IonDatumException("float(p) not supported", null, span())
    }

    private fun real(): Datum {
        val v = reader.doubleValue()
        if (v > Float.MAX_VALUE || v < Float.MIN_VALUE) {
            throw IonDatumException("real out of range", null, span())
        }
        return Datum.real(v.toFloat())
    }

    private fun double(): Datum {
        val v = reader.doubleValue()
        return Datum.doublePrecision(v)
    }

    private fun char0(): Datum = char1(1)

    private fun char1(length: Int): Datum {
        val v = reader.stringValue()
        val l = v.length
        if (l != length) {
            throw IonDatumException("char($length) had length $l", null, span())
        }
        return Datum.character(v, length)
    }

    private fun varchar0(): Datum {
        val v = reader.stringValue()
        return Datum.string(v)
    }

    private fun varchar1(length: Int): Datum {
        val v = reader.stringValue()
        val l = v.length
        if (l != length) {
            throw IonDatumException("varchar($length) had length $l", null, span())
        }
        return Datum.varchar(v, l)
    }

    private fun clob0(): Datum {
        val v = reader.newBytes()
        val n = v.size
        return Datum.clob(v, n)
    }

    private fun clob1(size: Int): Datum {
        val v = reader.newBytes()
        val s = v.size
        if (s != size) {
            throw IonDatumException("clob($size) had size $s", null, span())
        }
        return Datum.clob(v, s)
    }

    private fun blob0(): Datum {
        val v = reader.newBytes()
        val n = v.size
        return Datum.blob(v, n)
    }

    private fun blob1(size: Int): Datum {
        val v = reader.newBytes()
        val s = v.size
        if (s != size) {
            throw IonDatumException("blob($size) had size $s", null, span())
        }
        return Datum.blob(v, s)
    }

    private fun date0(): Datum {
        throw IonDatumException("date not supported", null, span())
    }

    private fun time1(precision: Int): Datum {
        throw IonDatumException("time(p) not supported", null, span())
    }

    private fun timez1(precision: Int): Datum {
        throw IonDatumException("timez(p) not supported", null, span())
    }

    private fun timestamp1(precision: Int): Datum {
        throw IonDatumException("timestamp(p) not supported", null, span())
    }

    private fun timestampz1(precision: Int): Datum {
        throw IonDatumException("timestampz(p) not supported", null, span())
    }

    private fun array(): Datum {
        reader.stepIn()
        val elements = mutableListOf<Datum>()
        var next = next()
        while (next != null) {
            elements.add(next)
            next = next()
        }
        reader.stepOut()
        return Datum.array(elements)
    }

    private fun bag(): Datum {
        reader.stepIn()
        val elements = mutableListOf<Datum>()
        var next = next()
        while (next != null) {
            elements.add(next)
            next = next()
        }
        reader.stepOut()
        return Datum.bag(elements)
    }

    private fun struct(): Datum {
        reader.stepIn()
        val fields = mutableListOf<Field>()
        while (reader.next() != null) {
            val name = reader.fieldName
            val value = read()
            fields.add(Field.of(name, value))
        }
        reader.stepOut()
        return Datum.struct(fields)
    }

    private fun ion(): Datum {
        val v = loadSingleElement(reader)
        return IonVariant(v)
    }
}
