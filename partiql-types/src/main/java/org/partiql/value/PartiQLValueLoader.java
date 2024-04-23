package org.partiql.value;

import org.jetbrains.annotations.NotNull;

/**
 * Provides functions for loading {@link PartiQLCursor} into {@link PartiQLValue} instances.
 *
 * @see PartiQLCursor
 */
public interface PartiQLValueLoader {
    /**
     * Loads un-materialized {@link PartiQLCursor} into a materialized {@link PartiQLValue}. The {@link PartiQLCursor} cursor
     * must be set <b>before</b> the value that you'd like to load.
     * <p>
     * This method will invoke {@link PartiQLCursor#next()}. This method will <b>not</b> throw an error is there is
     * more data to be processed after the value immediately following the cursor.
     *
     * @param data the PartiQL data to load.
     * @return a materialized, in-memory instance of a {@link PartiQLValue} containing the contents of the {@code data}.
     */
    @NotNull
    PartiQLValue load(@NotNull PartiQLCursor data);

    /**
     * @return a basic implementation of {@link PartiQLValueLoader}.
     */
    static PartiQLValueLoader standard() {
        return new PartiQLValueLoaderDefault();
    }
}
