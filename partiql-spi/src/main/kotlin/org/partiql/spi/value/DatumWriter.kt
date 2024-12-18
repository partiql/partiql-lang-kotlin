package org.partiql.spi.value

import org.partiql.spi.stream.PSink
import org.partiql.types.PType

/**
 * The {@link DatumWriter} provides a high-level interface for writing to a {@link PSink} implementation.
 */
public class DatumWriter : AutoCloseable {

    /**
     * The underlying value encoder.
     */
    private val sink: PSink

    /**
     * Create a DatumWriter (private)
     */
    @Suppress("ConvertSecondaryConstructorToPrimary")
    public constructor(sink: PSink) {
        this.sink = sink
    }

    /**
     * Like java.io.Writer with combined `append` and `write` since this does not implement Appendable.
     */
    public fun write(datum: Datum) {
        write(datum, true)
    }

    /**
     * TODO
     *
     * @param datum
     * @param typed
     */
    private fun write(datum: Datum, typed: Boolean) {
        val type = datum.getType()
        val code = type.code()
        // always check MISSING
        if (datum.isMissing) {
            sink.writeMissing()
            return
        }
        // types can be omitted in homogenous collections (heterogeneous is array<dynamic>)
        if (typed) {
            sink.setType(type)
        }
        // always check NULL
        if (datum.isNull) {
            sink.writeNull()
            return
        }
        // delegate to sink
        when (code) {
            PType.DYNAMIC -> error("Unexpected runtime dynamic")
            PType.BOOL -> sink.writeBool(datum.boolean)
            PType.TINYINT -> sink.writeTinyint(datum.byte)
            PType.SMALLINT -> sink.writeSmallint(datum.short)
            PType.INTEGER -> sink.writeInt(datum.int)
            PType.BIGINT -> sink.writeBigint(datum.long)
            PType.NUMERIC -> sink.writeNumeric(datum.bigDecimal)
            PType.DECIMAL -> sink.writeDecimal(datum.bigDecimal)
            PType.REAL -> sink.writeReal(datum.float)
            PType.DOUBLE -> sink.writeDouble(datum.double)
            PType.CHAR -> sink.writeChar(datum.string)
            PType.VARCHAR -> sink.writeVarchar(datum.string)
            PType.STRING -> sink.writeString(datum.string)
            PType.BLOB -> sink.writeBlob(datum.bytes)
            PType.CLOB -> sink.writeClob(datum.bytes)
            PType.DATE,
            PType.TIME,
            PType.TIMEZ,
            PType.TIMESTAMP,
            PType.TIMESTAMPZ,
            -> {
                TODO("datetime blocked on https://github.com/partiql/partiql-lang-kotlin/pull/1656")
            }
            PType.ARRAY,
            PType.BAG,
            -> {
                sink.stepIn(code)
                val dynamic = type.typeParameter.code() == PType.DYNAMIC
                for (child in datum.iterator()) {
                    write(child, dynamic)
                }
                sink.stepOut()
            }
            else -> {
                TODO("unsupported PTYPE")
            }
        }
    }

    public override fun close() {
        sink.close()
    }
}
