package org.partiql.plan;

import org.jetbrains.annotations.NotNull;

/**
 * A plan holds operations that can be executed.
 */
public interface Plan {

    /**
     * @return version for serialization and debugging.
     */
    @NotNull
    default public Version getVersion() {
        return Version.UNKNOWN();
    }

    /**
     * @return statement action to execute.
     */
    @NotNull
    public Action getAction();
}
