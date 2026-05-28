package org.partiql.spi.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.types.PType;

/**
 * Represents an entry within a PartiQL {@link PType#MAP}.
 * @deprecated This feature is experimental and is subject to change.
 */
@Deprecated
public interface Entry {

    /**
     * @return the key in the key-value pair that this entry represents.
     */
    @NotNull
    Datum getKey();

    /**
     * @return the value in the key-value pair that this entry represents.
     */
    @NotNull
    Datum getValue();

    /**
     * Returns an instance of an {@link Entry}.
     * @param key the key datum
     * @param value the value datum
     * @return an instance of {@link Entry} that holds the {@code key} and {@code value}
     */
    @NotNull
    static Entry of(@NotNull Datum key, @NotNull Datum value) {
        return new Entry() {
            @NotNull
            @Override
            public Datum getKey() {
                return key;
            }

            @NotNull
            @Override
            public Datum getValue() {
                return value;
            }
        };
    }
}
