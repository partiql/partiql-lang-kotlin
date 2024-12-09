package org.partiql.spi.stream;

import org.jetbrains.annotations.NotNull;
import org.partiql.types.PType;
import org.partiql.value.datetime.Date;
import org.partiql.value.datetime.Time;
import org.partiql.value.datetime.Timestamp;

import java.math.BigDecimal;

/**
 * This is a PartiQL value stream source.
 * <br>
 * Developer Note:
 * - There should be a method for every Datum *java* value and all PType arguments.
 * - Method names are derived from PType.Kind as pascal case.
 */
public interface PSource {

    default void close() {
        // no-op
    }

    /**
     * Positions the source to the next value, return its type.
     */
    PType next();

    void readNull();

    void readMissing();

    boolean readBool();

    byte readTinyint();

    short readSmallint();

    int readInt();

    long readBigint();

    @NotNull
    BigDecimal readDecimal();

    float readReal();

    double readDouble();

    @NotNull
    String readChar();

    @NotNull
    String readVarchar();

    @NotNull
    String readString();

    @NotNull
    byte[] readBlob();

    @NotNull
    byte[] readClob();

    @NotNull
    Date readDate();

    @NotNull
    Time readTime();

    @NotNull
    Time readTimez();

    @NotNull
    Timestamp readTimestamp();

    @NotNull
    Timestamp readTimestampz();

    @NotNull
    String readField(@NotNull String name);

    void stepIn();

    void stepOut();
}
