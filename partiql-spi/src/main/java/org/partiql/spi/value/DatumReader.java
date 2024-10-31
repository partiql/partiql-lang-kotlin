package org.partiql.spi.value;

import org.jetbrains.annotations.Nullable;

/**
 * The {@link DatumReader} interface is a low-level reader interface for reading streams of PartiQL data.
 * <br>
 * {@see java.io.Reader}
 * <br>
 * TODO
 *  - public void reset();
 *  - public void skip(long n);
 */
public interface DatumReader extends AutoCloseable {

    /**
     * @return next Datum or null.
     */
    @Nullable
    public Datum next();
}
