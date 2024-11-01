package org.partiql.spi.value;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.spi.value.ion.IonDatumReader;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * The {@link DatumReader} interface is {@link java.io.Reader} like interface for reading encoded PartiQL values.
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

    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return a reader implementation for {@link Encoding#ION}.
     */
    @NotNull
    public static DatumReader ion(InputStream input) {
        return new IonDatumReader(input, new HashMap<>());
    }

    /**
     * A DatumReader can be re-used.
     */
    public class Builder {

        private Encoding encoding;
        private final Map<Encoding, DatumReader> others = new HashMap<>();

        public Builder encoding(Encoding encoding) {
            this.encoding = encoding;
            return this;
        }

        public Builder register(Encoding encoding, DatumReader reader) {
            others.put(encoding, reader);
            return this;
        }

        /**
         * @throws IllegalArgumentException if the encoding is not set or the encoding is not supported.
         * @param input InputStream
         * @return DatumReader
         */
        public DatumReader build(InputStream input) {
            if (encoding == null) {
                throw new IllegalArgumentException("encoding cannot be null, set with .encoding(..)");
            }
            switch (encoding.code()) {
                case Encoding.ION:
                    return new IonDatumReader(input, others);
                case Encoding.UNKNOWN:
                default:
                    throw new IllegalArgumentException("no reader for encoding: " + encoding);
            }
        }
    }
}
