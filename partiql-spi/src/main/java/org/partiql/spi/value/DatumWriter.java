package org.partiql.spi.value;

/**
 * The {@link DatumWriter} interface is a low-level writer interface for writing streams of PartiQL data.
 * <br>
 * {@see java.io.Writer}
 */
public interface DatumWriter extends AutoCloseable {

    /**
     * Like java.io.Reader with combined `append` and `write` since this does not implement Appendable.
     *
     * @param datum to write.
     */
    public DatumWriter write(Datum datum);
}
