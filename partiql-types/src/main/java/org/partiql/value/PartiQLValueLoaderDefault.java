package org.partiql.value;

import kotlin.Pair;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import static org.partiql.value.PartiQL.*;

class PartiQLValueLoaderDefault implements PartiQLValueLoader {
    @NotNull
    @Override
    public PartiQLValue load(@NotNull PartiQLCursor data) {
        data.next();
        return _loadSingleValue(data);
    }

    /**
     * MUST place the cursor on the data before executing.
     *
     * @param data the input PartiQL Data.
     * @return a materialized PartiQL Value
     */
    @NotNull
    private PartiQLValue _loadSingleValue(@NotNull PartiQLCursor data) {
        PartiQLValueType type = data.getType();
        switch (type) {
            case BOOL:
                return boolValue(orNull(data, PartiQLCursor::getBoolValue));
            case INT8:
                return int8Value(orNull(data, PartiQLCursor::getInt8Value));
            case INT16:
                return int16Value(orNull(data, PartiQLCursor::getInt16Value));
            case INT32:
                return int32Value(orNull(data, PartiQLCursor::getInt32Value));
            case INT64:
                return int64Value(orNull(data, PartiQLCursor::getInt64Value));
            case INT:
                return intValue(orNull(data, PartiQLCursor::getIntValue));
            case LIST:
                return collectionValue(data, PartiQL::listValue);
            case BAG:
                return collectionValue(data, PartiQL::bagValue);
            case SEXP:
                return collectionValue(data, PartiQL::sexpValue);
            case STRUCT:
                return createStructValue(data);
            case NULL:
                return nullValue();
            case STRING:
                return stringValue(orNull(data, PartiQLCursor::getStringValue));
            case SYMBOL:
                return symbolValue(orNull(data, PartiQLCursor::getSymbolValue));
            case CHAR:
                // TODO: The implementation of CHAR VALUE is wrong.
                String val = orNull(data, PartiQLCursor::getCharValue);
                if (val == null) {
                    return charValue(null);
                }
                return charValue(val.charAt(0));
            case MISSING:
                return missingValue();
            case DECIMAL_ARBITRARY:
                return decimalValue(orNull(data, PartiQLCursor::getDecimalArbitraryValue));
            case DECIMAL:
                return decimalValue(orNull(data, PartiQLCursor::getDecimalValue));
            case INTERVAL:
                return intervalValue(orNull(data, PartiQLCursor::getIntervalValue));
            case TIMESTAMP:
                return timestampValue(orNull(data, PartiQLCursor::getTimestampValue));
            case DATE:
                return dateValue(orNull(data, PartiQLCursor::getDateValue));
            case CLOB:
                return clobValue(orNull(data, PartiQLCursor::getClobValue));
            case BLOB:
                return blobValue(orNull(data, PartiQLCursor::getBlobValue));
            case BINARY:
                byte[] bytes = orNull(data, PartiQLCursor::getBinaryValue);
                if (bytes == null) {
                    return binaryValue(null);
                }
                return binaryValue(BitSet.valueOf(bytes));
            case BYTE:
                return byteValue(orNull(data, PartiQLCursor::getByteValue));
            case TIME:
                return timeValue(orNull(data, PartiQLCursor::getTimeValue));
            case FLOAT32:
                return float32Value(orNull(data, PartiQLCursor::getFloat32Value));
            case FLOAT64:
                return float64Value(orNull(data, PartiQLCursor::getFloat64Value));
            case ANY:
            default:
                throw new IllegalStateException("Cannot load data of type: " + type);
        }
    }

    private <R> R orNull(PartiQLCursor data, Function1<PartiQLCursor, R> result) {
        return data.isNullValue() ? null : result.invoke(data);
    }

    private PartiQLValue collectionValue(PartiQLCursor data, Function1<Iterable<PartiQLValue>, PartiQLValue> collectionConstructor) {
        if (data.isNullValue()) {
            return collectionConstructor.invoke(null);
        }
        data.stepIn();
        List<PartiQLValue> values = new ArrayList<>();
        while (data.hasNext()) {
            data.next();
            PartiQLValue value = this._loadSingleValue(data);
            values.add(value);
        }
        data.stepOut();
        return collectionConstructor.invoke(values);
    }

    private PartiQLValue createStructValue(PartiQLCursor data) {
        if (data.isNullValue()) {
            return structValue((Iterable<? extends Pair<String, ? extends PartiQLValue>>) null);
        }
        data.stepIn();
        List<Pair<String, PartiQLValue>> values = new ArrayList<>();
        while (data.hasNext()) {
            data.next();
            String name = data.getFieldName();
            PartiQLValue value = this._loadSingleValue(data);
            values.add(new Pair<>(name, value));
        }
        data.stepOut();
        return structValue(values);
    }
}
