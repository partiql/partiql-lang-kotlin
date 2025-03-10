package org.partiql.eval;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.value.Datum;

public interface WindowFunction {

    /**
     * TODO
     * @param partition TODO
     */
    void reset(@NotNull WindowPartition partition);

    /**
     * TODO
     * @return TODO
     */
    @NotNull
    Datum eval(@NotNull Environment env, long orderingGroupStart, long orderingGroupEnd);
}