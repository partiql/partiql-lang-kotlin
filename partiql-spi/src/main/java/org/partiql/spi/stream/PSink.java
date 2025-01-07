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
 * Each value can be written with or without type decoration based upon the actual encoding.
 */
public interface PSink {

    default void close() {
        // no-op
    }

    default void finish() {
        // no-op
    }

    default void flush() {
        // no-op
    }

    /**
     * Set the PType for the next written value; cleared after write.
     */
    void setType(@NotNull PType type);

    /**
     * Write NULL value.
     */
    void writeNull();

    /**
     * Write MISSING value.
     */
    void writeMissing();

    /**
     * Write BOOL value.
     */
    void writeBool(boolean value);

    /**
     * Write TINYINT value.
     */
    void writeTinyint(byte value);

    /**
     * Write SMALLINT value.
     */
    void writeSmallint(short value);

    /**
     * Write INT value.
     */
    void writeInt(int value);

    /**
     * Write BIGINT value.
     */
    void writeBigint(long value);

    /**
     * Write NUMERIC value.
     */
    void writeNumeric(@NotNull BigDecimal value);

    /**
     * Write DECIMAL value.
     */
    void writeDecimal(@NotNull BigDecimal value);

    /**
     * Write REAL value.
     */
    void writeReal(float value);

    /**
     * Write DOUBLE PRECISION value.
     */
    void writeDouble(double value);

    /**
     * Write CHAR value.
     */
    void writeChar(@NotNull String value);

    /**
     * Write VARCHAR value.
     */
    void writeVarchar(@NotNull String value);

    /**
     * Write STRING value.
     */
    void writeString(@NotNull String value);

    /**
     * Write BLOB value.
     */
    void writeBlob(@NotNull byte[] value);

    /**
     * Write CLOB value.
     */
    void writeClob(@NotNull byte[] value);

    /**
     * Write DATE value.
     */
    void writeDate(@NotNull Date value);

    /**
     * Write TIME value.
     */
    void writeTime(@NotNull Time value);

    /**
     * Write TIMEZ value.
     */
    void writeTimez(@NotNull Time value);

    /**
     * Write TIMESTAMP value.
     */
    void writeTimestamp(@NotNull Timestamp value);

    /**
     * Write TIMESTAMPZ with given precision.
     */
    void writeTimestampz(@NotNull Timestamp value);

    /**
     * Write a VARIANT type.
     */
    <T> void writeVariant(@NotNull T value);

    /**
     * Write STRUCT or ROW field name.
     */
    void writeField(@NotNull String name);

    /**
     * Step into container, given as PType code.
     */
    void stepIn(int container);

    /**
     * Step out of container type.
     */
    void stepOut();
}
