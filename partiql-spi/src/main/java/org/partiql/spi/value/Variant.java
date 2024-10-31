package org.partiql.spi.value;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;

/**
 * Variant is a {@link Datum} with the ability to pack and unpack a value.
 *
 * @param <T>
 */
public interface Variant<T> extends Datum {

    /**
     * Unpack the inner variant value.
     *
     * @return T
     */
    T unpack();

    /**
     * TODO move to writer?
     * <br>
     * Pack the variant into a byte array.
     *
     * @return byte[]
     */
    default byte[] pack() {
        throw new UnsupportedOperationException("variant does not have a byte[] encoding.");
    }

    /**
     * TODO move to writer?
     * <br>
     * Pack the variant into a byte array with the given charset.
     *
     * @param charset   charset
     * @return          byte[]
     */
    default byte[] pack(@NotNull  Charset charset) {
        throw new UnsupportedOperationException("variant does not have an encoding for charset: " + charset.name());
    }
}
