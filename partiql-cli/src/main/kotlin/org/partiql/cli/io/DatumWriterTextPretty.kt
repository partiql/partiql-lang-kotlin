package org.partiql.cli.io

import org.partiql.spi.types.IntervalCode
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import org.partiql.spi.value.DatumWriter

class DatumWriterTextPretty(
    private val stream: Appendable,
    private val indent: String = INDENT
) : DatumWriter {

    private val out = IndentStream(stream)

    companion object {
        private const val INDENT = "  "
    }

    override fun close() {}

    /**
     * Text format
     *
     * @param indent Index prefix
     * @param prefix Prefix to print before each line
     * @param ignoreNext Ignore printing the next line's prefix
     */
    private data class Format(
        val indent: String = INDENT,
        val prefix: String = "",
        var ignoreNext: Boolean = false
    ) {
        fun nest() = copy(prefix = prefix + indent, ignoreNext = false)
        fun nest(ignoreFirstLine: Boolean) = copy(prefix = prefix + indent, ignoreNext = ignoreFirstLine)
    }

    override fun write(datum: Datum?): DatumWriter {
        val format = Format(indent)
        if (datum == null) {
            return this
        }
        write(datum, format)
        return this
    }

    private fun write(datum: Datum, format: Format?) {
        // NULL
        if (datum.isNull) {
            format(format) { writeScalar("null") }
            return
        }

        // MISSING
        if (datum.isMissing) {
            format(format) { writeScalar("missing") }
            return
        }

        // Print data according to type
        val dType = datum.type
        when (val dTypeCode = dType.code()) {
            PType.DYNAMIC -> error("The dynamic type should never be encountered at runtime.")
            PType.BOOL -> format(format) { writeScalar(datum.boolean) }
            PType.TINYINT -> format(format) { writeScalar(datum.byte) }
            PType.SMALLINT -> format(format) { writeScalar(datum.short) }
            PType.INTEGER -> format(format) { writeScalar(datum.int) }
            PType.BIGINT -> format(format) { writeScalar(datum.long) }
            PType.NUMERIC, PType.DECIMAL -> format(format) { writeScalar(datum.bigDecimal) }
            PType.REAL -> format(format) { writeScalar(datum.float) }
            PType.DOUBLE -> format(format) { writeScalar(datum.double) }
            PType.CHAR, PType.VARCHAR, PType.STRING -> format(format) { writeString(datum.string) }
            PType.BLOB -> format(format) { writeBlob(datum) } // TODO: What is the correct way to write these?
            PType.CLOB -> format(format) { writeString(datum.string) } // TODO: What is the correct way to write these?
            PType.DATE -> format(format) { writeDate(datum) }
            PType.TIME -> format(format) { writeTime(datum) }
            PType.TIMEZ -> format(format) { writeTimez(datum) }
            PType.TIMESTAMP -> format(format) { writeTimestamp(datum) }
            PType.TIMESTAMPZ -> format(format) { writeTimestampz(datum) }
            PType.ARRAY -> writeCollection(datum, format, "[", "]")
            PType.BAG -> writeCollection(datum, format, "<<", ">>")
            PType.ROW, PType.STRUCT -> writeStructure(datum, format)
            PType.UNKNOWN -> error("The unknown type should not be encountered here.")
            PType.VARIANT -> format(format) { writeVariant(datum) }
            PType.INTERVAL_YM, PType.INTERVAL_DT -> format(format) { writeInterval(datum) }
            else -> error("Unknown type: $dTypeCode")
        }
    }

    private fun format(format: Format?, block: () -> Unit) {
        if (format != null) {
            if (format.ignoreNext) {
                format.ignoreNext = false
            } else {
                this.out.print(format.prefix)
            }
        }
        block.invoke()
    }

    private fun writeTime(datum: Datum) {
        this.out.print("TIME '")
        this.out.print(datum.localTime.toString())
        this.out.print("'")
    }

    private fun writeTimez(datum: Datum) {
        this.out.print("TIME '")
        this.out.print(datum.offsetTime.toString())
        this.out.print("'")
    }

    private fun writeTimestamp(datum: Datum) {
        this.out.print("TIMESTAMP '")
        this.out.print(datum.localDateTime.toString())
        this.out.print("'")
    }

    private fun writeVariant(datum: Datum) {
        this.out.print("`")
        this.out.print(datum.pack(Charsets.UTF_8))
        this.out.print("`")
    }

    private fun writeTimestampz(datum: Datum) {
        this.out.print("TIMESTAMP '")
        this.out.print(datum.offsetDateTime.toString())
        this.out.print("'")
    }

    private fun writeDate(datum: Datum) {
        this.out.print("DATE '")
        this.out.print(datum.localDate.toString())
        this.out.print("'")
    }

    private fun writeScalar(value: Any) {
        this.out.print(value.toString())
    }

    private fun writeBlob(datum: Datum) {
        this.out.print("BLOB '")
        this.out.print(datum.bytes.toString(Charsets.UTF_8))
        this.out.print("'")
    }

    private fun writeString(value: String) {
        this.out.print("'")
        this.out.print(value.replace("'", "''"))
        this.out.print("'")
    }

    private fun writeCollection(datum: Datum, format: Format?, prefix: String, postfix: String) {
        // Print prefix
        format(format) {
            this.out.print(prefix)
        }
        // Get element data, and print postfix if empty (and return)
        val iterator = datum.iterator()
        if (!iterator.hasNext()) {
            this.out.print(postfix)
            return
        }
        this.out.println()
        // Print elements and postfix
        val newFormat = format?.nest()
        while (iterator.hasNext()) {
            write(iterator.next(), newFormat)
            if (iterator.hasNext()) {
                this.out.print(",")
            }
            this.out.println()
        }
        format(format) {
            this.out.print(postfix)
        }
    }

    private fun writeStructure(datum: Datum, format: Format?) {
        // Print prefix
        format(format) {
            this.out.print("{")
        }
        // Get fields data, and print postfix if empty (and return)
        if (!datum.fields.hasNext()) {
            this.out.print("}")
            return
        }
        this.out.println()
        // Print fields and postfix
        val iterator = datum.fields
        val fieldFormat = format?.nest(false)
        while (iterator.hasNext()) {
            val field = iterator.next()
            format(fieldFormat) {
                this.out.print("'")
                this.out.print(field.name)
                this.out.print("'")
                this.out.print(": ")
                val valueFormat = format?.nest(true)
                write(field.value, valueFormat)
                val separator = if (iterator.hasNext()) "," else ""
                this.out.println(separator)
            }
        }
        format(format) {
            this.out.print("}")
        }
    }

    private fun writeInterval(datum: Datum) {
        this.out.print("INTERVAL ")
        val type = datum.type
        when (type.intervalCode) {
            IntervalCode.YEAR -> {
                this.out.print("'")
                this.out.print(datum.years)
                this.out.print("' YEAR (")
                this.out.print(type.precision)
                this.out.print(")")
            }
            IntervalCode.MONTH -> {
                this.out.print("'")
                this.out.print(datum.months)
                this.out.print("' MONTH (")
                this.out.print(type.precision)
                this.out.print(")")
            }
            IntervalCode.DAY -> {
                this.out.print("'")
                this.out.print(datum.days)
                this.out.print("' DAY (")
                this.out.print(type.precision)
                this.out.print(")")
            }
            IntervalCode.HOUR -> {
                this.out.print("'")
                this.out.print(datum.hours)
                this.out.print("' HOUR (")
                this.out.print(type.precision)
                this.out.print(")")
            }
            IntervalCode.MINUTE -> {
                this.out.print("'")
                this.out.print(datum.minutes)
                this.out.print("' MINUTE (")
                this.out.print(type.precision)
                this.out.print(")")
            }
            IntervalCode.SECOND -> {
                this.out.print("'")
                this.out.print(datum.seconds)
                this.out.print(fractionalSeconds(datum.nanos, type.fractionalPrecision))
                this.out.print("' SECOND (")
                this.out.print(type.precision)
                this.out.print(", ")
                this.out.print(type.fractionalPrecision)
                this.out.print(")")
            }
            IntervalCode.YEAR_MONTH -> {
                this.out.print("'")
                this.out.print(datum.years)
                this.out.print("-")
                this.out.print(datum.months)
                this.out.print("' YEAR (")
                this.out.print(type.precision)
                this.out.print(") TO MONTH")
            }
            IntervalCode.DAY_HOUR -> {
                this.out.print("'")
                this.out.print(datum.days)
                this.out.print(" ")
                this.out.print(datum.hours)
                this.out.print("' DAY (")
                this.out.print(type.precision)
                this.out.print(") TO HOUR")
            }
            IntervalCode.DAY_MINUTE -> {
                this.out.print("'")
                this.out.print(datum.days)
                this.out.print(" ")
                this.out.print(datum.hours)
                this.out.print(":")
                this.out.print(datum.minutes)
                this.out.print("' DAY (")
                this.out.print(type.precision)
                this.out.print(") TO MINUTE")
            }
            IntervalCode.DAY_SECOND -> {
                this.out.print("'")
                this.out.print(datum.days)
                this.out.print(" ")
                this.out.print(datum.hours)
                this.out.print(":")
                this.out.print(datum.minutes)
                this.out.print(":")
                this.out.print(datum.seconds)
                this.out.print(fractionalSeconds(datum.nanos, type.fractionalPrecision))
                this.out.print("' DAY (")
                this.out.print(type.precision)
                this.out.print(") TO SECOND (")
                this.out.print(type.fractionalPrecision)
                this.out.print(")")
            }
            IntervalCode.HOUR_MINUTE -> {
                this.out.print("'")
                this.out.print(datum.hours)
                this.out.print(":")
                this.out.print(datum.minutes)
                this.out.print("' HOUR (")
                this.out.print(type.precision)
                this.out.print(") TO MINUTE")
            }
            IntervalCode.HOUR_SECOND -> {
                this.out.print("'")
                this.out.print(datum.hours)
                this.out.print(":")
                this.out.print(datum.minutes)
                this.out.print(":")
                this.out.print(datum.seconds)
                this.out.print(fractionalSeconds(datum.nanos, type.fractionalPrecision))
                this.out.print("' HOUR (")
                this.out.print(type.precision)
                this.out.print(") TO SECOND (")
                this.out.print(type.fractionalPrecision)
                this.out.print(")")
            }
            IntervalCode.MINUTE_SECOND -> {
                this.out.print("'")
                this.out.print(datum.minutes)
                this.out.print(":")
                this.out.print(datum.seconds)
                this.out.print(fractionalSeconds(datum.nanos, type.fractionalPrecision))
                this.out.print("' MINUTE (")
                this.out.print(type.precision)
                this.out.print(") TO SECOND (")
                this.out.print(type.fractionalPrecision)
                this.out.print(")")
            }
            else -> throw IllegalArgumentException("Unsupported interval descriptor: ${type.intervalCode}")
        }
    }

    private fun fractionalSeconds(value: Int, scale: Int): String {
        val nanoLength = 9
        return buildString {
            append(".")
            if (scale == 0) {
                return@buildString
            }
            val before = value.toString()
            val beforeLength = before.length
            val toPadBefore = nanoLength - beforeLength
            if (toPadBefore > 0) {
                append("0".repeat(toPadBefore))
            }
            val remaining = before.substring(0, scale - toPadBefore)
            append(remaining)
        }
    }

    private class IndentStream(private val out: Appendable) {
        fun print(value: Any?) {
            this.out.append(value.toString())
        }

        fun println() {
            this.out.appendLine()
        }

        fun println(value: Any?) {
            this.out.appendLine(value.toString())
        }
    }
}
