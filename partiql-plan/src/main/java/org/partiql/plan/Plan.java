package org.partiql.plan;

import org.jetbrains.annotations.NotNull;

import java.util.List;

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
     * @return statement actions to execute.
     */
    @NotNull
    public List<Action> getActions();
}
