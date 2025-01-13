package org.partiql.plan;

import org.jetbrains.annotations.NotNull;

/**
 * A plan holds operations that can be executed.
 */
public interface Plan {

    /**
     * Returns the version of this plan.
     * @return version for serialization and debugging.
     */
    @NotNull
    default Version getVersion() {
        return Version.V1();
    }

    /**
     * Returns the statement action to execute.
     * @return statement action to execute.
     */
    @NotNull
    Action getAction();
}
