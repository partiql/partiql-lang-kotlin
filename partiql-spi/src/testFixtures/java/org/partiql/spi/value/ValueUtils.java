package org.partiql.spi.value;

import kotlin.NotImplementedError;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.partiql.spi.types.PType;
import org.partiql.value.IntValue;
import org.partiql.value.PartiQL;
import org.partiql.value.PartiQLValue;
import org.partiql.value.PartiQLValueType;
import org.partiql.value.datetime.DateTimeUtil;

import java.math.BigDecimal;
import java.util.Objects;
import static org.partiql.spi.types.PType.ARRAY;
import static org.partiql.spi.types.PType.BAG;
import static org.partiql.spi.types.PType.BIGINT;
import static org.partiql.spi.types.PType.BLOB;
import static org.partiql.spi.types.PType.BOOL;
import static org.partiql.spi.types.PType.CHAR;
import static org.partiql.spi.types.PType.CLOB;
import static org.partiql.spi.types.PType.DATE;
import static org.partiql.spi.types.PType.DECIMAL;
import static org.partiql.spi.types.PType.DOUBLE;
import static org.partiql.spi.types.PType.DYNAMIC;
import static org.partiql.spi.types.PType.INTEGER;
import static org.partiql.spi.types.PType.NUMERIC;
import static org.partiql.spi.types.PType.REAL;
import static org.partiql.spi.types.PType.ROW;
import static org.partiql.spi.types.PType.SMALLINT;
import static org.partiql.spi.types.PType.STRING;
import static org.partiql.spi.types.PType.STRUCT;
import static org.partiql.spi.types.PType.TIME;
import static org.partiql.spi.types.PType.TIMESTAMP;
import static org.partiql.spi.types.PType.TIMESTAMPZ;
import static org.partiql.spi.types.PType.TIMEZ;
import static org.partiql.spi.types.PType.TINYINT;
import static org.partiql.spi.types.PType.UNKNOWN;
import static org.partiql.spi.types.PType.VARIANT;

/**
 * This internal class contains utility methods pertaining to {@link PartiQLValue} and {@link Datum}.
 */
public class ValueUtils {

    /**
     * Converts a {@link Datum} into a {@link PartiQLValue}.
     * @return the equivalent {@link PartiQLValue}
     */
    @NotNull
    public static PartiQLValue newPartiQLValue(@NotNull Datum datum) {
        PType type = datum.getType();
        if (datum.isMissing()) {
            return PartiQL.missingValue();
        }
        switch (type.code()) {
            case BOOL:
                return datum.isNull() ? PartiQL.boolValue(null) : PartiQL.boolValue(datum.getBoolean());
            case TINYINT:
                return datum.isNull() ? PartiQL.int8Value(null) : PartiQL.int8Value(datum.getByte());
            case SMALLINT:
                return datum.isNull() ? PartiQL.int16Value(null) : PartiQL.int16Value(datum.getShort());
            case INTEGER:
                return datum.isNull() ? PartiQL.int32Value(null) : PartiQL.int32Value(datum.getInt());
            case BIGINT:
                return datum.isNull() ? PartiQL.int64Value(null) : PartiQL.int64Value(datum.getLong());
            case NUMERIC:
                return datum.isNull() ? PartiQL.intValue(null) : PartiQL.intValue(datum.getBigDecimal().toBigInteger());
            case DECIMAL:
                return datum.isNull() ? PartiQL.decimalValue(null) : PartiQL.decimalValue(datum.getBigDecimal());
            case REAL:
                return datum.isNull() ? PartiQL.float32Value(null) : PartiQL.float32Value(datum.getFloat());
            case DOUBLE:
                return datum.isNull() ? PartiQL.float64Value(null) : PartiQL.float64Value(datum.getDouble());
            case CHAR:
                return datum.isNull() ? PartiQL.charValue(null) : PartiQL.charValue(datum.getString().charAt(0));
            case STRING:
                return datum.isNull() ? PartiQL.stringValue(null) : PartiQL.stringValue(datum.getString());
            case BLOB:
                return datum.isNull() ? PartiQL.blobValue(null) : PartiQL.blobValue(datum.getBytes());
            case CLOB:
                return datum.isNull() ? PartiQL.clobValue(null) : PartiQL.clobValue(datum.getBytes());
            case DATE:
                return datum.isNull() ? PartiQL.dateValue(null) : PartiQL.dateValue(DateTimeUtil.toDate(datum.getLocalDate()));
            case TIME:
                return datum.isNull() ? PartiQL.timeValue(null) : PartiQL.timeValue(DateTimeUtil.toTime(datum.getLocalTime()));
            case TIMEZ:
                return datum.isNull() ? PartiQL.timeValue(null) : PartiQL.timeValue(DateTimeUtil.toTime(datum.getOffsetTime()));
            case TIMESTAMP:
                return datum.isNull() ? PartiQL.timestampValue(null) : PartiQL.timestampValue(DateTimeUtil.toTimestamp(datum.getLocalDateTime()));
            case TIMESTAMPZ:
                return datum.isNull() ? PartiQL.timestampValue(null) : PartiQL.timestampValue(DateTimeUtil.toTimestamp(datum.getOffsetDateTime()));
            case BAG:
                return datum.isNull() ? PartiQL.bagValue((Iterable<? extends PartiQLValue>) null) : PartiQL.bagValue(new PQLToPartiQLIterable(datum));
            case ARRAY:
                return datum.isNull() ? PartiQL.listValue((Iterable<? extends PartiQLValue>) null) : PartiQL.listValue(new PQLToPartiQLIterable(datum));
            case STRUCT:
            case ROW:
                return datum.isNull() ? PartiQL.structValue((Iterable<? extends Pair<String, ? extends PartiQLValue>>) null) : PartiQL.structValue(new PQLToPartiQLStruct(datum));
            case DYNAMIC:
            case UNKNOWN:
                if (datum.isNull()) {
                    return PartiQL.nullValue();
                } else if (datum.isMissing()) {
                    return PartiQL.missingValue();
                }
            case VARIANT:
                return datum.isNull() ? PartiQL.nullValue() : newPartiQLValue(datum.lower());
            default:
                throw new UnsupportedOperationException("Unsupported datum type: " + type);
        }
    }

    /**
     * Converts a {@link PartiQLValue} into {@link Datum}.
     *
     * @return the equivalent {@link Datum}
     */
    @NotNull
    public static Datum newDatum(PartiQLValue value) {
        PartiQLValueType type = value.getType();
        if (value.isNull()) {
            return new DatumNull(type.toPType());
        }
        switch (type) {
            case MISSING:
                return new DatumMissing();
            case NULL:
                return new DatumNull();
            case INT8:
                org.partiql.value.Int8Value int8Value = (org.partiql.value.Int8Value) value;
                return new DatumByte(Objects.requireNonNull(int8Value.getValue()), PType.tinyint());
            case STRUCT:
                @SuppressWarnings("unchecked") org.partiql.value.StructValue<PartiQLValue> STRUCTValue = (org.partiql.value.StructValue<PartiQLValue>) value;
                return new DatumStruct(new PartiQLToPQLStruct(Objects.requireNonNull(STRUCTValue)));
            case STRING:
                org.partiql.value.StringValue STRINGValue = (org.partiql.value.StringValue) value;
                return new DatumString(Objects.requireNonNull(STRINGValue.getValue()), PType.string());
            case INT64:
                org.partiql.value.Int64Value INT64Value = (org.partiql.value.Int64Value) value;
                return new DatumLong(Objects.requireNonNull(INT64Value.getValue()));
            case INT32:
                org.partiql.value.Int32Value INT32Value = (org.partiql.value.Int32Value) value;
                return new DatumInt(Objects.requireNonNull(INT32Value.getValue()));
            case INT16:
                org.partiql.value.Int16Value INT16Value = (org.partiql.value.Int16Value) value;
                return new DatumShort(Objects.requireNonNull(INT16Value.getValue()));
            case SEXP:
                throw new IllegalStateException("SEXP is not supported.");
            case LIST:
                @SuppressWarnings("unchecked") org.partiql.value.ListValue<PartiQLValue> LISTValue = (org.partiql.value.ListValue<PartiQLValue>) value;
                return new DatumCollection(new PartiQLToPQLIterable(Objects.requireNonNull(LISTValue)), PType.array());
            case BOOL:
                org.partiql.value.BoolValue BOOLValue = (org.partiql.value.BoolValue) value;
                return new DatumBoolean(Objects.requireNonNull(BOOLValue.getValue()));
            case INT:
                org.partiql.value.IntValue INTValue = (org.partiql.value.IntValue) value;
                return new DatumDecimal(new BigDecimal(Objects.requireNonNull(INTValue.getValue())), PType.numeric());
            case BAG:
                @SuppressWarnings("unchecked") org.partiql.value.BagValue<PartiQLValue> BAGValue = (org.partiql.value.BagValue<PartiQLValue>) value;
                return new DatumCollection(new PartiQLToPQLIterable(Objects.requireNonNull(BAGValue)), PType.bag());
            case BINARY:
                throw new UnsupportedOperationException();
            case DATE:
                org.partiql.value.DateValue DATEValue = (org.partiql.value.DateValue) value;
                return DateTimeUtil.toDatumDate(Objects.requireNonNull(DATEValue.getValue()));
            case TIME:
                org.partiql.value.TimeValue TIMEValue = (org.partiql.value.TimeValue) value;
                return DateTimeUtil.toDatumTime(Objects.requireNonNull(TIMEValue.getValue()));
            case TIMESTAMP:
                org.partiql.value.TimestampValue TIMESTAMPValue = (org.partiql.value.TimestampValue) value;
                return DateTimeUtil.toDatumTimestamp(Objects.requireNonNull(TIMESTAMPValue.getValue()));
            case INTERVAL:
                throw new UnsupportedOperationException("interval not supported");
            case FLOAT32:
                org.partiql.value.Float32Value FLOAT32Value = (org.partiql.value.Float32Value) value;
                return new DatumFloat(Objects.requireNonNull(FLOAT32Value.getValue()));
            case FLOAT64:
                org.partiql.value.Float64Value FLOAT64Value = (org.partiql.value.Float64Value) value;
                return new DatumDouble(Objects.requireNonNull(FLOAT64Value.getValue()));
            case DECIMAL:
            case DECIMAL_ARBITRARY:
                org.partiql.value.DecimalValue DECIMALValue = (org.partiql.value.DecimalValue) value;
                BigDecimal d = Objects.requireNonNull(DECIMALValue.getValue());
                return Datum.decimal(d, d.precision(), d.scale());
            case CHAR:
                org.partiql.value.CharValue CHARValue = (org.partiql.value.CharValue) value;
                String charString = Objects.requireNonNull(CHARValue.getValue()).toString();
                return new DatumChars(charString, charString.length());
            case SYMBOL:
                throw new IllegalStateException("SYMBOL not supported.");
            case CLOB:
                org.partiql.value.ClobValue CLOBValue = (org.partiql.value.ClobValue) value;
                return new DatumBytes(Objects.requireNonNull(CLOBValue.getValue()), PType.clob(Integer.MAX_VALUE)); // TODO
            case BLOB:
                org.partiql.value.BlobValue BLOBValue = (org.partiql.value.BlobValue) value;
                return new DatumBytes(Objects.requireNonNull(BLOBValue.getValue()), PType.blob(Integer.MAX_VALUE)); // TODO
            case BYTE:
                throw new UnsupportedOperationException();
            case ANY:
            default:
                throw new NotImplementedError();
        }
    }
}
