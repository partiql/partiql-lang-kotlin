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
    public Version getVersion();

    /**
     * @return operations to execute.
     */
    @NotNull
    public List<Operation> getOperations();
}
