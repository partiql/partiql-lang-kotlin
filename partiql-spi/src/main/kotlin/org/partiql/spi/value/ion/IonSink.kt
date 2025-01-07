package org.partiql.spi.value.ion

import com.amazon.ion.IonType
import com.amazon.ion.IonWriter
import com.amazon.ion.Timestamp
import com.amazon.ion.system.IonBinaryWriterBuilder
import com.amazon.ion.system.IonTextWriterBuilder
import org.partiql.spi.stream.PSink
import org.partiql.types.PType
import java.io.OutputStream
import java.lang.Double.parseDouble
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.util.BitSet

/**
 * IonSink is an encoder for PartiQL values using an IonWriter.
 */
public class IonSink : PSink {

    /**
     * The underlying IonWriter
     */
    private val writer: IonWriter

    /**
     * The type elisions.
     */
    private val elisions: BitSet

    /**
     * Create an IonSink from an IonWriter
     */
    @Suppress("ConvertSecondaryConstructorToPrimary")
    public constructor(writer: IonWriter, elisions: BitSet) {
        this.writer = writer
        this.elisions = elisions
    }

    public companion object {

        /**
         * The standard Ion elisions.
         */
        @JvmStatic
        private val elisions = intArrayOf(
            PType.BOOL,
            PType.BIGINT,
            PType.DOUBLE,
            PType.STRING,
            PType.CLOB,
            PType.BLOB,
            PType.ARRAY,
            PType.TIMESTAMP,
            PType.TIMESTAMPZ,
        )

        /**
         * Create a standard IonSink backed by an Ion text writer.
         */
        @JvmStatic
        @JvmOverloads
        public fun text(out: Appendable, elisions: IntArray? = null): IonSink {
            return Builder(elisions ?: this.elisions).build((IonTextWriterBuilder.standard().build(out)))
        }

        /**
         * Create an IonSink backed by an Ion pretty text writer.
         */
        @JvmStatic
        @JvmOverloads
        public fun pretty(out: Appendable, elisions: IntArray? = null): IonSink {
            return Builder(elisions ?: this.elisions).build((IonTextWriterBuilder.pretty().build(out)))
        }

        /**
         * Create an IonSink backed by an Ion binary writer.
         */
        @JvmStatic
        @JvmOverloads
        public fun binary(out: OutputStream, elisions: IntArray? = null): IonSink {
            return Builder(elisions ?: this.elisions).build((IonBinaryWriterBuilder.standard().build(out)))
        }

        /**
         * Create an IonSink backed by the given IonWriter with standard type decorators.
         */
        @JvmStatic
        public fun standard(writer: IonWriter): IonSink {
            return standard().build(writer)
        }

        /**
         * @return a new IonSink.Builder with standard type decorators.
         */
        @JvmStatic
        public fun standard(): Builder = Builder(elisions)

        /**
         * @return a new IonSink.Builder with all type decorators.
         */
        @JvmStatic
        public fun decorated(): Builder {
            return Builder(intArrayOf())
        }

        /**
         * @return a new IonSink.Builder with all type elisions.
         */
        @JvmStatic
        public fun elided(): Builder {
            return Builder(PType.codes())
        }
    }

    override fun close() {
        this.writer.close()
    }

    override fun finish() {
        this.writer.finish()
    }

    override fun flush() {
        this.writer.flush()
    }

    override fun setType(type: PType) {
        if (type.code() == PType.UNKNOWN) {
            return // skip
        }
        this.writer.setTypeAnnotations(symbol(type))
    }

    override fun writeNull() {
        this.writer.writeNull()
    }

    override fun writeMissing() {
        this.writer.writeSymbol("missing")
    }

    override fun writeBool(value: Boolean) {
        if (elisions[PType.BOOL]) {
            this.writer.setTypeAnnotations()
        }
        this.writer.writeBool(value)
    }

    override fun writeTinyint(value: Byte) {
        if (elisions[PType.TINYINT]) {
            this.writer.setTypeAnnotations()
        }
        this.writer.writeInt(value.toLong())
    }

    override fun writeSmallint(value: Short) {
        if (elisions[PType.SMALLINT]) {
            this.writer.setTypeAnnotations()
        }
        this.writer.writeInt(value.toLong())
    }

    override fun writeInt(value: Int) {
        if (elisions[PType.INTEGER]) {
            this.writer.setTypeAnnotations()
        }
        this.writer.writeInt(value.toLong())
    }

    override fun writeBigint(value: Long) {
        if (elisions[PType.BIGINT]) {
            this.writer.setTypeAnnotations()
        }
        this.writer.writeInt(value)
    }

    override fun writeNumeric(value: BigDecimal) {
        if (elisions[PType.NUMERIC]) {
            this.writer.setTypeAnnotations()
        }
        this.writer.writeDecimal(value)
    }

    override fun writeDecimal(value: BigDecimal) {
        if (elisions[PType.DECIMAL]) {
            this.writer.setTypeAnnotations()
        }
        this.writer.writeDecimal(value)
    }

    override fun writeReal(value: Float) {
        if (elisions[PType.REAL]) {
            this.writer.setTypeAnnotations()
        }
        // IonWriter expects a double,
        // 1. parseDouble((3.14f).toString()) -> PASS: real:3.14e0
        // 2. (3.14f).toDouble()              -> FAIL: Expected: real::3.14e0, Actual: real::3.140000104904175e0
        val v = parseDouble(value.toString())
        this.writer.writeFloat(v)
    }

    override fun writeDouble(value: Double) {
        if (elisions[PType.DOUBLE]) {
            this.writer.setTypeAnnotations()
        }
        this.writer.writeFloat(value)
    }

    override fun writeChar(value: String) {
        if (elisions[PType.CHAR]) {
            this.writer.setTypeAnnotations()
        }
        this.writer.writeString(value)
    }

    override fun writeVarchar(value: String) {
        if (elisions[PType.VARCHAR]) {
            this.writer.setTypeAnnotations()
        }
        this.writer.writeString(value)
    }

    override fun writeString(value: String) {
        if (elisions[PType.STRING]) {
            this.writer.setTypeAnnotations()
        }
        this.writer.writeString(value)
    }

    override fun writeBlob(value: ByteArray) {
        if (elisions[PType.BLOB]) {
            this.writer.setTypeAnnotations()
        }
        this.writer.writeBlob(value)
    }

    override fun writeClob(value: ByteArray) {
        if (elisions[PType.CLOB]) {
            this.writer.setTypeAnnotations()
        }
        this.writer.writeClob(value)
    }

    override fun writeDate(value: LocalDate) {
        if (elisions[PType.DATE]) {
            this.writer.setTypeAnnotations()
        }
        val iso8601 = value.toString()
        this.writer.writeString(iso8601)
    }

    override fun writeTime(value: LocalTime) {
        if (elisions[PType.TIME]) {
            this.writer.setTypeAnnotations()
        }
        val iso8601 = value.toString()
        this.writer.writeString(iso8601)
    }

    override fun writeTimez(value: OffsetTime) {
        if (elisions[PType.TIMEZ]) {
            this.writer.setTypeAnnotations()
        }
        val iso8601 = value.toString()
        this.writer.writeString(iso8601)
    }

    override fun writeTimestamp(value: LocalDateTime) {
        if (elisions[PType.TIMESTAMP]) {
            this.writer.setTypeAnnotations()
        }
        val iso8601 = value.toString()
        this.writer.writeString(iso8601)
    }

    override fun writeTimestampz(value: OffsetDateTime) {
        if (elisions[PType.TIMESTAMPZ]) {
            this.writer.setTypeAnnotations()
        }
        val iso8601 = value.toString()
        this.writer.writeString(iso8601)
    }

    override fun <T : Any> writeVariant(value: T) {
        TODO("Not yet implemented")
    }

    override fun writeField(name: String) {
        this.writer.setFieldName(name)
    }

    override fun stepIn(container: Int) {
        when (container) {
            PType.ARRAY -> this.writer.stepIn(IonType.LIST)
            PType.BAG -> this.writer.stepIn(IonType.LIST)
            PType.ROW -> this.writer.stepIn(IonType.STRUCT)
            PType.STRUCT -> this.writer.stepIn(IonType.STRUCT)
            else -> error("Expected ARRAY, BAG, ROW, or STRUCT, found code: $container")
        }
    }

    override fun stepOut() {
        this.writer.stepOut()
    }

    /**
     * Writes a PartiQL type as an Ion symbol.
     */
    private fun symbol(type: PType): String = when (type.code()) {
        PType.BOOL -> "bool"
        PType.TINYINT -> "tinyint"
        PType.SMALLINT -> "smallint"
        PType.INTEGER -> "int"
        PType.BIGINT -> "bigint"
        PType.NUMERIC -> "numeric(${type.precision},${type.scale})"
        PType.DECIMAL -> "decimal(${type.precision},${type.scale})"
        PType.REAL -> "real"
        PType.DOUBLE -> "double"
        PType.CHAR -> "char(${type.length})"
        PType.VARCHAR -> "varchar(${type.length})"
        PType.STRING -> "string"
        PType.BLOB -> "blob(${type.length})"
        PType.CLOB -> "clob(${type.length})"
        PType.DATE -> "date"
        PType.TIME -> "time(${type.precision})"
        PType.TIMEZ -> "timez(${type.precision})"
        PType.TIMESTAMP -> "timestamp(${type.precision})"
        PType.TIMESTAMPZ -> "timestampz(${type.precision})"
        PType.ARRAY -> "array<${symbol(type.typeParameter)}>"
        PType.BAG -> "bag"
        PType.ROW -> "row"
        PType.STRUCT -> "struct"
        PType.DYNAMIC -> "dynamic"
        PType.UNKNOWN -> error("Unexpected UNKNOWN type")
        else -> error("Unexpected ptype code: ${type.code()}")
    }

    /**
     * A builder to configure an IonSink.
     */
    public class Builder internal constructor(elisions: IntArray) {

        /**
         * You could make this a bit flag for some throughput gains (maybe).
         */
        private val elisions = BitSet()

        init {
            for (code in elisions) {
                this.elide(code)
            }
        }

        /**
         * Adds a type elision (removes the type decorator if it exists).
         *
         * @return this builder
         */
        public fun elide(code: Int): Builder {
            elisions[code] = true
            return this
        }

        /**
         * Adds a type decorator (removes the type elision if it exists).
         *
         * @return this builder
         */
        public fun decorate(code: Int): Builder {
            elisions[code] = false
            return this
        }

        /**
         * @return a new IonSink instance.
         */
        public fun build(writer: IonWriter): IonSink {
            // impls could be smart about which direction to put branches or omit them altogether
            return IonSink(writer, elisions)
        }
    }
}
