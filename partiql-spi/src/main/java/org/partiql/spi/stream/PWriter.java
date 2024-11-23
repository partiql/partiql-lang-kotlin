package org.partiql.spi.stream;

import org.jetbrains.annotations.NotNull;
import org.partiql.types.PType;
import org.partiql.value.datetime.Date;
import org.partiql.value.datetime.Time;
import org.partiql.value.datetime.Timestamp;

import java.math.BigDecimal;

/**
 * This is a PartiQL value stream sink.
 * <br>
 * Developer Note:
 * - There should be a method for every Datum *java* value and all PType arguments.
 * - Method names are derived from PType.Kind as pascal case.
 *
 * TODO move datetime to spi.datetime.
 */
public interface PWriter {

    default void close() {
        // no-op
    }

    default void finish() {
        // no-op
    }

    default void flush() {
        // no-op
    }

    void writeNull();

    void writeMissing();

    void writeBool(boolean value);

    void writeTinyint(byte value);

    void writeSmallint(short value);

    void writeInt(int value);

    void writeBigint(long value);

    void writeNumeric(@NotNull BigDecimal value);

    void writeNumeric(@NotNull BigDecimal value, int precision);

    void writeNumeric(@NotNull BigDecimal value, int precision, int scale);

    void writeDecimal(@NotNull BigDecimal value);

    void writeDecimal(@NotNull BigDecimal value, int precision);

    void writeDecimal(@NotNull BigDecimal value, int precision, int scale);

    void writeReal(float value);

    void writeDouble(double value);

    void writeChar(@NotNull String value, int length);

    void writeVarchar(@NotNull String value, int length);

    void writeString(@NotNull String value);

    void writeBlob(@NotNull byte[] value, int length);

    void writeClob(@NotNull byte[] value, int length);

    void writeDate(@NotNull Date value);

    void writeTime(@NotNull Time value, int precision);

    void writeTimez(@NotNull Time value, int precision);

    void writeTimestamp(@NotNull Timestamp value, int precision);

    void writeTimestampz(@NotNull Timestamp value, int precision);

    <T> void writeVariant(@NotNull T value);

    void writeVariant(@NotNull String value, @NotNull String encoding);

    void writeVariant(@NotNull byte[] value, @NotNull String encoding);

    void writeField(@NotNull String name);

    void stepIn(@NotNull PType container);

    void stepOut();
}
